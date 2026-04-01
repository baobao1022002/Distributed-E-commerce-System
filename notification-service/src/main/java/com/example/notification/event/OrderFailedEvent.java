package com.example.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFailedEvent {
    private String orderId;
    private String customerId;
    private String customerEmail;
    private String failureReason;
}
