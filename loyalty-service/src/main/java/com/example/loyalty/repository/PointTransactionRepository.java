package com.example.loyalty.repository;

import com.example.loyalty.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, String> {
    List<PointTransaction> findByUserId(String userId);
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
    List<PointTransaction> findByUserIdAndExpiresAtBefore(String userId, LocalDateTime expiresAt);
}
