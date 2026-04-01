package com.example.loyalty.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    private String orderId;
    private String correlationId;
    private String customerId;
    private String customerEmail;
    private BigDecimal finalAmount;
    private String paymentId;
}
