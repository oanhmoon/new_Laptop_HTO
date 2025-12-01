package org.example.laptopstore.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String refundReason;
    private String refundImageUrl;
    private String refundVideoUrl;
    public OrderResponse(Long orderId) {
        this.orderId = orderId;
    }
}
