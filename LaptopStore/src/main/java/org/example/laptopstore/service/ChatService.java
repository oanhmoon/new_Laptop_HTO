//package org.example.laptopstore.service;
//
//import lombok.RequiredArgsConstructor;
//import org.example.laptopstore.entity.ProductOption;
//import org.example.laptopstore.entity.ProductVariant;
//import org.example.laptopstore.repository.ProductOptionRepository;
//import org.example.laptopstore.repository.ProductVariantRepository;
//import org.example.laptopstore.repository.UserViewHistoryRepository;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//@RequiredArgsConstructor
//public class ChatService {
//
//    private final PolicyService policyService;
//    private final ProductOptionRepository productOptionRepository;
//    private final AIService aiService;
//    private final UserViewHistoryRepository userViewHistoryRepository;
//    private final ProductVariantRepository productVariantRepository;
//
//
//    private BigDecimal extractPrice(String q) {
//        Pattern p = Pattern.compile("(\\d+)\\s*(triệu|tr|m)");
//        Matcher m = p.matcher(q);
//        if (m.find()) {
//            return BigDecimal.valueOf(Integer.parseInt(m.group(1)) * 1_000_000L);
//        }
//        return null;
//    }
//
//    private String extractColor(String q) {
//        if (q.contains("đen")) return "đen";
//        if (q.contains("trắng")) return "trắng";
//        if (q.contains("bạc")) return "bạc";
//        if (q.contains("xanh")) return "xanh";
//        if (q.contains("đỏ")) return "đỏ";
//        return null;
//    }
//
//    private String extractCpu(String q) {
//        if (q.contains("i3")) return "i3";
//        if (q.contains("i5")) return "i5";
//        if (q.contains("i7")) return "i7";
//        if (q.contains("i9")) return "i9";
//        if (q.contains("ryzen 5")) return "ryzen 5";
//        if (q.contains("ryzen 7")) return "ryzen 7";
//        return null;
//    }
//
//    private String extractGpu(String q) {
//        if (q.contains("rtx 3050")) return "3050";
//        if (q.contains("rtx 4050")) return "4050";
//        if (q.contains("rtx")) return "rtx";
//        if (q.contains("gtx")) return "gtx";
//        return null;
//    }
//
//    private String extractRam(String q) {
//        Pattern p = Pattern.compile("(\\d+)gb");
//        Matcher m = p.matcher(q);
//        if (m.find()) return m.group(1) + "gb";
//        return null;
//    }
//
//    private String extractBrand(String q) {
//        if (q.contains("asus")) return "asus";
//        if (q.contains("acer")) return "acer";
//        if (q.contains("dell")) return "dell";
//        if (q.contains("lenovo")) return "lenovo";
//        if (q.contains("hp")) return "hp";
//        if (q.contains("msi")) return "msi";
//        return null;
//    }
//
//    private String extractCategory(String q) {
//        if (q.contains("gaming")) return "gaming";
//        if (q.contains("văn phòng")) return "office";
//        if (q.contains("đồ họa")) return "creator";
//        return null;
//    }
//
//    private String formatOptions(List<ProductOption> list) {
//
//
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("Danh sách gợi ý (tối đa 10):\n");
//
//        int i = 1;
//        for (ProductOption o : list) {
//            sb.append(i++).append(". ")
//                    .append(o.getProduct().getName())
//                    .append(" | Giá: ").append(o.getPrice())
//                    .append(" | CPU: ").append(nullSafe(o.getCpu()))
//                    .append(" | GPU: ").append(nullSafe(o.getGpu()))
//                    .append(" | RAM: ").append(nullSafe(o.getRam()))
//                    .append(" | Màu: ")
//                    .append(
//                            o.getProductVariants() != null && !o.getProductVariants().isEmpty()
//                                    ? o.getProductVariants().stream()
//                                    .map(ProductVariant::getColor)
//                                    .reduce((a, b) -> a + ", " + b)
//                                    .orElse("-")
//                                    : "-"
//                    )
//                    .append("\n");
//
//            if (i > 10) break;
//        }
//
//        return sb.toString();
//    }
//
//
//
//    public String handleChat(String question, Long userId) {
//        // 1. Kiểm tra policy
//        String policyAnswer = policyService.searchInPolicy(question);
//        if (policyAnswer != null && !policyAnswer.isBlank()) {
//            // có thể cho LLM tóm tắt policy trước khi trả về
//            String summarized = aiService.askLLM("Tóm tắt ngắn gọn nội dung sau bằng tiếng Việt:\n\n" + policyAnswer, "");
//            return summarized;
//        }
//
//
//
//
//        // 2. Query DB
//        String dbContext = searchProductByQuestion(question);
//
//        // 3. Gọi LLM với context
//        String answer = aiService.askLLM(question, dbContext);
//        return answer;
//    }
//
//    private String getColorsInfo(List<ProductOption> list) {
//        if (list.isEmpty()) return "-";
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("Danh sách màu có sẵn:\n");
//
//        for (ProductOption o : list) {
//            List<ProductVariant> variants = productVariantRepository.findByProductOptionId(o.getId());
//
//            sb.append("• ").append(o.getProduct().getName()).append(": ");
//
//            if (variants.isEmpty()) {
//                sb.append("Không có thông tin màu.\n");
//            } else {
//                variants.forEach(v -> sb.append(v.getColor()).append(", "));
//                sb.setLength(sb.length() - 2); // xóa dấu phẩy cuối
//                sb.append("\n");
//            }
//        }
//
//        return sb.toString();
//    }
//
//
//    private String searchProductByQuestion(String question) {
//        String q = question.toLowerCase();
//        List<ProductOption> list = new ArrayList<>();
//
//        // 1. Giá
//        BigDecimal maxPrice = extractPrice(q);
//        if (maxPrice != null) {
//            list = productOptionRepository.findByPriceLessThanEqual(maxPrice);
//        }
//
//        // 2. Màu sắc
//        String color = extractColor(q);
//        if (color != null) {
//            list = productOptionRepository.findByVariantColor(color);
//        }
//
//        // 3. CPU
//        String cpu = extractCpu(q);
//        if (cpu != null) {
//            list = productOptionRepository.findByCpu(cpu);
//        }
//
//        // 4. GPU
//        String gpu = extractGpu(q);
//        if (gpu != null) {
//            list = productOptionRepository.findByGpu(gpu);
//        }
//
//        // 5. RAM
//        String ram = extractRam(q);
//        if (ram != null) {
//            list = productOptionRepository.findByRam(ram);
//        }
//
//        // 6. Thương hiệu
//        String brand = extractBrand(q);
//        if (brand != null) {
//            list = productOptionRepository.findByBrand(brand);
//        }
//
//        // 7. Category
//        String cat = extractCategory(q);
//        if (cat != null) {
//            list = productOptionRepository.findByCategory(cat);
//        }
//
//        // 8. Bán chạy
//        if (q.contains("bán chạy") || q.contains("xem nhiều") || q.contains("nhiều người xem")) {
//            list = userViewHistoryRepository.findMostViewedProducts();
//        }
//
//
//        // 9. Đánh giá cao
//        if (q.contains("đánh giá cao") || q.contains("tốt nhất") || q.contains("5 sao")) {
//            list = productOptionRepository.findTopRated();
//        }
//
//        if (list.isEmpty()) {
//            list = productOptionRepository.findTop10ByOrderByPriceAsc();
//        }
//
//        if (question.toLowerCase().contains("màu gì")
//                || question.toLowerCase().contains("có màu")) {
//
//            List<ProductOption> list = productOptionRepository.findTop10ByOrderByPriceAsc();
//            String colorContext = getColorsInfo(list);
//
//            return aiService.askLLM(
//                    "Dựa trên dữ liệu sau, trả lời câu hỏi của user:\n\n" + colorContext,
//                    question
//            );
//        }
//
//        return formatOptions(list);
//    }
//
//
//    private String nullSafe(String s) {
//        return s == null ? "-" : s;
//    }
//}

package org.example.laptopstore.service;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.entity.ChatHistory;
import org.example.laptopstore.entity.ProductOption;
import org.example.laptopstore.entity.ProductVariant;
import org.example.laptopstore.repository.ChatHistoryRepository;
import org.example.laptopstore.repository.ProductOptionRepository;
import org.example.laptopstore.repository.ProductVariantRepository;
import org.example.laptopstore.repository.UserViewHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final PolicyService policyService;
    private final ProductOptionRepository productOptionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserViewHistoryRepository userViewHistoryRepository;
    private final AIService aiService;
    private final ChatHistoryRepository chatHistoryRepository;



    // 1 EXTRACT USER INTENT

    private BigDecimal extractPrice(String q) {
        Pattern p = Pattern.compile("(\\d+)\\s*(triệu|tr|m)");
        Matcher m = p.matcher(q);
        if (m.find()) {
            return BigDecimal.valueOf(Integer.parseInt(m.group(1)) * 1_000_000L);
        }
        return null;
    }
    private void saveAIHistory(Long userId, String answer) {
        chatHistoryRepository.save(ChatHistory.builder()
                .userId(userId)
                .role("assistant")
                .message(answer)
                .createdAt(LocalDateTime.now())
                .build());
    }


    private String extractColor(String q) {
        if (q.contains("đen")) return "đen";
        if (q.contains("trắng")) return "trắng";
        if (q.contains("bạc")) return "bạc";
        if (q.contains("xanh")) return "xanh";
        if (q.contains("đỏ")) return "đỏ";
        return null;
    }

    private String extractCpu(String q) {
        if (q.contains("i3")) return "i3";
        if (q.contains("i5")) return "i5";
        if (q.contains("i7")) return "i7";
        if (q.contains("i9")) return "i9";
        if (q.contains("ryzen 5")) return "ryzen 5";
        if (q.contains("ryzen 7")) return "ryzen 7";
        return null;
    }

    private String extractGpu(String q) {
        if (q.contains("rtx 3050")) return "3050";
        if (q.contains("rtx 4050")) return "4050";
        if (q.contains("rtx")) return "rtx";
        if (q.contains("gtx")) return "gtx";
        return null;
    }

    private String extractRam(String q) {
        Pattern p = Pattern.compile("(\\d+)gb");
        Matcher m = p.matcher(q);
        if (m.find()) return m.group(1) + "gb";
        return null;
    }

    private String extractBrand(String q) {
        if (q.contains("asus")) return "asus";
        if (q.contains("acer")) return "acer";
        if (q.contains("dell")) return "dell";
        if (q.contains("lenovo")) return "lenovo";
        if (q.contains("hp")) return "hp";
        if (q.contains("msi")) return "msi";
        return null;
    }

    private String extractCategory(String q) {
        if (q.contains("gaming")) return "gaming";
        if (q.contains("văn phòng")) return "office";
        if (q.contains("đồ họa")) return "creator";
        return null;
    }


    // 2 FORMAT PRODUCT LIST

    private String formatOptions(List<ProductOption> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("Danh sách gợi ý (tối đa 10):\n");

        int i = 1;
        for (ProductOption o : list) {
            sb.append(i++).append(". ")
                    .append(o.getProduct().getName())
                    .append(" | Giá: ").append(o.getPrice())
                    .append(" | CPU: ").append(nullSafe(o.getCpu()))
                    .append(" | GPU: ").append(nullSafe(o.getGpu()))
                    .append(" | RAM: ").append(nullSafe(o.getRam()))
                    .append(" | Màu: ");

            if (o.getProductVariants() != null && !o.getProductVariants().isEmpty()) {
                String colors = o.getProductVariants()
                        .stream()
                        .map(ProductVariant::getColor)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-");
                sb.append(colors);
            } else sb.append("-");

            sb.append("\n");

            if (i > 10) break;
        }

        return sb.toString();
    }

    private String nullSafe(String s) {
        return s == null ? "-" : s;
    }


    // 3 GET COLOR INFO

    private String getColorsInfo(List<ProductOption> list) {
        if (list.isEmpty()) return "Không tìm thấy sản phẩm.";

        StringBuilder sb = new StringBuilder();
        sb.append("Danh sách màu của từng sản phẩm:\n");

        for (ProductOption o : list) {
            sb.append("• ").append(o.getProduct().getName()).append(": ");

            List<ProductVariant> variants =
                    productVariantRepository.findByOption_Id(o.getId());


            if (variants.isEmpty()) {
                sb.append("Không có thông tin màu.\n");
            } else {
                String colors = variants.stream()
                        .map(ProductVariant::getColor)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-");
                sb.append(colors).append("\n");
            }
        }

        return sb.toString();
    }


    // 4 MAIN CHAT ENTRY

    public String handleChat(String question, Long userId) {

        // Lưu câu hỏi vào history
        chatHistoryRepository.save(ChatHistory.builder()
                .userId(userId)
                .role("user")
                .message(question)
                .createdAt(LocalDateTime.now())
                .build()
        );


        String lower = question.toLowerCase();

        // 1 POLICY CHECK
        String policyAnswer = policyService.searchInPolicy(question);
        if (policyAnswer != null && !policyAnswer.isBlank()) {
            return aiService.askLLM(
                    "Tóm tắt ngắn gọn nội dung chính sách sau bằng tiếng Việt:\n" + policyAnswer,
                    ""
            );
        }

        // 2 "MÀU GÌ" / "CÓ MÀU"
        if (lower.contains("màu gì") || lower.contains("có màu")) {

            // Tìm sản phẩm theo tên trong câu hỏi
            List<ProductOption> matched =
                    productOptionRepository.searchByNameLike("%" + question + "%");

            if (matched.isEmpty()) {
                matched = productOptionRepository.findTop10ByOrderByPriceAsc();
            }

            String colorInfo = getColorsInfo(matched);

            return aiService.askLLM(
                    "Thông tin màu sắc:\n" + colorInfo,
                    question
            );
        }

        // 3 NORMAL PRODUCT SEARCH
        String dbContext = searchProductByQuestion(question);

        // 4 SEND TO AI
        //return aiService.askLLM(question, dbContext);
        String answer = aiService.askLLMWithHistory(
                question,
                dbContext,
                chatHistoryRepository.findLast10(userId)
        );

        saveAIHistory(userId, answer);
        return answer;

    }


    // 5. PRODUCT SEARCH LOGIC

    private String searchProductByQuestion(String question) {
        String q = question.toLowerCase();
        List<ProductOption> list = new ArrayList<>();

        // 1. Giá
        BigDecimal maxPrice = extractPrice(q);
        if (maxPrice != null) {
            list = productOptionRepository.findByPriceLessThanEqual(maxPrice);
        }

        // 2. Màu sắc (filter bằng ProductVariant)
        String color = extractColor(q);
        if (color != null) {
            list = productOptionRepository.findByVariantColor(color);
        }

        // 3. CPU
        String cpu = extractCpu(q);
        if (cpu != null) {
            list = productOptionRepository.findByCpu(cpu);
        }

        // 4. GPU
        String gpu = extractGpu(q);
        if (gpu != null) {
            list = productOptionRepository.findByGpu(gpu);
        }

        // 5. RAM
        String ram = extractRam(q);
        if (ram != null) {
            list = productOptionRepository.findByRam(ram);
        }

        // 6. Thương hiệu
        String brand = extractBrand(q);
        if (brand != null) {
            list = productOptionRepository.findByBrand(brand);
        }

        // 7. Category
        String cat = extractCategory(q);
        if (cat != null) {
            list = productOptionRepository.findByCategory(cat);
        }

        // 8. Bán chạy / xem nhiều
        if (q.contains("bán chạy") || q.contains("xem nhiều") || q.contains("nhiều người xem")) {
            list = userViewHistoryRepository.findMostViewedProducts();
        }

        if (list.isEmpty()) {
            list = productOptionRepository.findTop10ByOrderByPriceAsc();
        }

        return formatOptions(list);
    }
}

