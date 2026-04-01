package com.example.notification.repository;

import com.example.notification.entity.EmailMessage;
import com.example.notification.entity.EmailMessage.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, String> {
    List<EmailMessage> findByStatus(EmailStatus status);
}
