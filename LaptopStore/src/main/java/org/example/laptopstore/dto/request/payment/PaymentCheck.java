package org.example.laptopstore.dto.request.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCheck {
    @NotNull
    private BigDecimal amount;

    @NotNull
    private Long orderId;

    @NotNull
    private Long userId;

    @NotNull
    private Integer type;


}
