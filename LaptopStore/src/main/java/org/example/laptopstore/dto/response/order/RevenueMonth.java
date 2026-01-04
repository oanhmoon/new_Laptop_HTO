package org.example.laptopstore.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RevenueMonth {

    private Integer month;
    private BigDecimal revenue;
    private Long products;
    private Long customers;

}
