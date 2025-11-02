package org.example.laptopstore.util.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    CANCELLED,
    PENDING_RETURNED,
    CONFIRMED_RETURNED,
    SHIPPED_RETURNED,
    RETURNED,
    REJECTED_RETURNED,
    COMPLETED;

}
