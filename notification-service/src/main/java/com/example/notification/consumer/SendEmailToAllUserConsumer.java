package com.example.notification.consumer;

import com.example.notification.event.EmailNotificationEvent;
import com.example.notification.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendEmailToAllUserConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "email-notifications",
            groupId = "email-sender",
            concurrency = "20"
    )
    public void consumeEmailBatch(String payload) throws JsonProcessingException {

        EmailNotificationEvent event = objectMapper.readValue(payload, EmailNotificationEvent.class);
        emailService.sendEmail(event.getEmail(), event.getSubject(),
                event.getContent(), event.getType(), event.getUserId());
    }
}