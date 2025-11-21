package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class RetrainService {
    public void notifyRetrain() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.postForObject("http://localhost:5000/api/retrain", null, String.class);
            System.out.println("Retrain triggered successfully.");
        } catch (Exception e) {
            System.err.println("Failed to trigger retrain: " + e.getMessage());
        }
    }
}
