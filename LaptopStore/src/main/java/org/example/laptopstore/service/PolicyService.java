package org.example.laptopstore.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Service
public class PolicyService {

    private String policyText = "";

    @PostConstruct
    public void init() {
        try {
            // load tất cả file pdf trong resources/policies
            File dir = new File("src/main/resources/policies");
            if (!dir.exists()) {
                return;
            }
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null) {
                StringBuilder sb = new StringBuilder();
                for (File f : files) {
                    sb.append(loadPdf(f)).append("\n\n");
                }
                policyText = sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadPdf(File file) throws IOException {
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public String searchInPolicy(String question) {
        if (policyText == null || policyText.isBlank()) return null;
        String q = question.toLowerCase(Locale.ROOT);
        // căn bản: nếu chứa từ khóa bảo hành / đổi trả / hoàn tiền / giao hàng
        if (q.contains("bảo hành") || q.contains("đổi trả") || q.contains("hoàn tiền") || q.contains("giao hàng")) {
            return extractRelevant(policyText, q);
        }
        return null;
    }

    private String extractRelevant(String text, String q) {
        String lower = text.toLowerCase(Locale.ROOT);
        // tìm từ khóa mạnh nhất trong câu
        String[] keys = new String[] {"bảo hành", "đổi trả", "hoàn tiền", "giao hàng", "bảo hành chính sách", "trả hàng"};
        int idx = -1;
        for (String k : keys) {
            idx = lower.indexOf(k);
            if (idx >= 0) break;
        }
        if (idx < 0) {
            // fallback: tìm first word
            String[] words = q.split("\\s+");
            for (String w : words) {
                idx = lower.indexOf(w);
                if (idx >= 0) break;
            }
        }
        if (idx < 0) {
            // trả đoạn tóm tắt đầu
            return text.length() > 800 ? text.substring(0, 800) : text;
        }
        int start = Math.max(0, idx - 400);
        int end = Math.min(text.length(), idx + 800);
        String snippet = text.substring(start, end);
        return snippet;
    }
}
