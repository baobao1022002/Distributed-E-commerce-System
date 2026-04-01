package com.example.payment_service.consumer;

import com.example.payment_service.event.PaymentVerifiedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentVerifyConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "payment-verify", groupId = "payment-service")
    public void handlePaymentVerify(String payload) throws JsonProcessingException {
        PaymentVerifiedEvent request = objectMapper.readValue(payload, PaymentVerifiedEvent.class);
        MDC.put("correlationId", request.getCorrelationId());
        MDC.put("orderId", request.getOrderId());
        MDC.put("action", "PAYMENT_VERIFY");

        try {
            log.info("Received payment-verify request: orderId={}, amount={}", request.getOrderId(), request.getAmount());

            // Simple validation stub: amount must be > 0
            boolean validAmount = request.getAmount() != null && request.getAmount().signum() >= 0;

            PaymentVerifiedEvent response;
            if (validAmount) {
                String paymentId = UUID.randomUUID().toString();
                response = new PaymentVerifiedEvent(
                        request.getOrderId(),
                        request.getCorrelationId(),
                        paymentId,
                        request.getAmount(),
                        true,
                        null
                );
                log.info("Payment approved for orderId={}, paymentId={}", request.getOrderId(), paymentId);
            } else {
                response = new PaymentVerifiedEvent(
                        request.getOrderId(),
                        request.getCorrelationId(),
                        null,
                        request.getAmount(),
                        false,
                        "Invalid payment amount"
                );
                log.warn("Payment rejected for orderId={} due to invalid amount", request.getOrderId());
            }

            kafkaTemplate.send("payment-verified", response);
        } catch (Exception ex) {
            log.error("Error processing payment-verify for orderId={}", request.getOrderId(), ex);
            PaymentVerifiedEvent response = new PaymentVerifiedEvent(
                    request.getOrderId(),
                    request.getCorrelationId(),
                    null,
                    request.getAmount(),
                    false,
                    "Exception during payment verification: " + ex.getMessage()
            );
            kafkaTemplate.send("payment-verified", response);
        } finally {
            MDC.clear();
        }
    }
}

