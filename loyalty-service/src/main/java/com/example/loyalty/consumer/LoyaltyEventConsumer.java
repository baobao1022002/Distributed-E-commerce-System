package com.example.loyalty.consumer;

import com.example.loyalty.entity.PointTransaction;
import com.example.loyalty.event.OrderCompletedEvent;
import com.example.loyalty.service.LoyaltyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyEventConsumer {
    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-completed", groupId = "loyalty-service")
    public void consumeOrderCompleted(String orderCompletedEventString) throws JsonProcessingException {
        OrderCompletedEvent orderCompletedEvent = objectMapper.readValue(orderCompletedEventString, OrderCompletedEvent.class);

        log.info("Received order-completed for user={}, order={}", orderCompletedEvent.getCustomerId(), orderCompletedEvent.getOrderId());
        loyaltyService.earnPointsFromOrder(
                orderCompletedEvent.getCustomerId(),
                orderCompletedEvent.getOrderId(),
                orderCompletedEvent.getFinalAmount(),
                orderCompletedEvent.getCustomerEmail()
        );
    }

}
