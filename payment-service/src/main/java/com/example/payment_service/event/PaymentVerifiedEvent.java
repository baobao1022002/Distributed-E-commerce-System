package com.example.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerifiedEvent {
    private String orderId;
    private String correlationId;
    private String paymentId;
    private BigDecimal amount;
    private boolean success;
    private String failureReason;
}

