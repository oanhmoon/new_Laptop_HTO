package org.example.laptopstore.dto.response.productfavorite;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductFavoriteResponse {
    private Long id;
    private Long productOptionId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime createdAt;
}
