package org.example.laptopstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.example.laptopstore.entity.ChatHistory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class AIService {

    //private final OkHttpClient client = new OkHttpClient();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKey;

    private final String knowledgeBase;

    public AIService() throws IOException {
        this.knowledgeBase = Files.readString(
                Paths.get("src/main/resources/knowledge-base.txt")
        );
    }


    public String askLLM(String question, String context) {
        if (apiKey == null || apiKey.isBlank()) {
            return "OpenAI key chưa cấu hình. Vui lòng set biến môi trường OPENAI_API_KEY.";
        }

        try {
//            String system = """
//            Bạn là chuyên gia tư vấn laptop của LaptopStore.
//
//            Bạn có khả năng:
//            - phân tích nhu cầu người dùng (gaming, văn phòng, đồ họa, sinh viên, coder, designer…)
//            - so sánh CPU, GPU, RAM, SSD, tản nhiệt
//            - giải thích sự khác nhau giữa các dòng laptop
//            - gợi ý sản phẩm phù hợp dựa trên ngân sách + nhu cầu
//            - tư vấn ưu nhược điểm từng model
//            - trả lời tự nhiên như con người, ngắn gọn, rõ ràng
//            - nếu thiếu thông tin, hãy hỏi lại người dùng
//
//            Luôn trả lời bằng tiếng Việt.
//            """;
            String system = """
            Bạn là chuyên gia tư vấn laptop cao cấp của LaptopStore.
            
            QUY TẮC TRẢ LỜI:
            1. Nếu dữ liệu hệ thống (context) khớp chính xác yêu cầu khách: Hãy tư vấn sâu vào các ưu điểm của máy đó.
            2. Nếu dữ liệu hệ thống là "Danh sách gợi ý" (do không tìm thấy máy đúng ý khách): 
               - Phải khéo léo thông báo rằng mẫu máy khách tìm hiện không có sẵn hoặc không khớp hoàn toàn.
               - Sau đó, giới thiệu các mẫu máy trong danh sách gợi ý như là "những lựa chọn phổ biến nhất" hoặc "đang được quan tâm nhất" tại cửa hàng.
               - Phân tích tại sao những máy này có thể thay thế (ví dụ: cùng tầm giá, cùng phân khúc).
            3. Phong cách: Thân thiện, chuyên nghiệp, không trả lời kiểu robot liệt kê khô khan. 
            4. Trình bày: Dùng Markdown, gạch đầu dòng rõ ràng nhưng phải có đoạn dẫn dắt và kết luận.
            
            Luôn trả lời bằng tiếng Việt.
            """;

            String fullPrompt =
                    system +
                            "\n\nDữ liệu tri thức chung:\n" + knowledgeBase +
                            "\n\nDữ liệu từ hệ thống:\n" + (context == null ? "" : context) +
                            "\n\nCâu hỏi của khách: " + question;


            ObjectNode payload = mapper.createObjectNode();
            payload.put("model", "gpt-4o-mini"); // hoặc model thích hợp
            ArrayNode messages = mapper.createArrayNode();
            ObjectNode sys = mapper.createObjectNode();
            sys.put("role", "system");
            sys.put("content", system);
            ObjectNode user = mapper.createObjectNode();
            user.put("role", "user");
            user.put("content", fullPrompt);
            messages.add(sys);
            messages.add(user);
            payload.set("messages", messages);
            payload.put("temperature", 0.2);

            RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "LLM trả về lỗi: " + response.code();
                }
                String res = response.body().string();
                JsonNode root = mapper.readTree(res);
                JsonNode content = root.at("/choices/0/message/content");
                if (content.isMissingNode()) {
                    // fallback: trả toàn bộ json
                    return res;
                }
                return content.asText();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Lỗi gọi LLM: " + e.getMessage();
        }
    }

    public String askLLMWithHistory(String question, String context, List<ChatHistory> history) {

        if (apiKey == null || apiKey.isBlank()) {
            return "OpenAI API Key chưa cấu hình.";
        }

        try {
            ArrayNode messages = mapper.createArrayNode();

            // SYSTEM MESSAGE
            messages.add(buildMessage("system", """
                Bạn là trợ lý tư vấn laptop cao cấp của LaptopStore.
                Hãy trả lời thân thiện, tự nhiên, dựa trên thông tin dưới đây.
                Nếu thiếu dữ liệu, hãy hỏi lại người dùng.
                Trình bày câu trả lời rõ ràng
                Dùng gạch đầu dòng khi liệt kê
                Mỗi sản phẩm tách thành từng khối
                Không viết thành đoạn văn dài
                Ưu tiên Markdown
                Luôn trả lời bằng tiếng Việt.
                """));

            // HISTORY (tối đa 10 lượt)
            for (ChatHistory h : history) {
                messages.add(buildMessage(h.getRole(), h.getMessage()));
            }

            // USER MESSAGE (có kèm context)
            String fullPrompt =
                    "Tri thức nền:\n" + knowledgeBase +
                            "\n\nDữ liệu hệ thống:\n" + (context == null ? "" : context) +
                            "\n\nCâu hỏi: " + question;

            messages.add(buildMessage("user", fullPrompt));

            // PAYLOAD
            ObjectNode payload = mapper.createObjectNode();
            payload.put("model", "gpt-4o-mini");
            payload.set("messages", messages);
            payload.put("temperature", 0.2);

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String res = response.body().string();
                JsonNode root = mapper.readTree(res);
                return root.at("/choices/0/message/content").asText();
            }

        } catch (Exception e) {
            return "Lỗi LLM: " + e.getMessage();
        }
    }

    private ObjectNode buildMessage(String role, String content) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

}
