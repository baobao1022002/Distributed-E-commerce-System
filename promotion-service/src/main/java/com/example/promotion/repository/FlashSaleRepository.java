package com.example.promotion.repository;

import com.example.promotion.entity.FlashSale;
import com.example.promotion.entity.FlashSale.FlashSaleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, String> {
    List<FlashSale> findByStatusAndStartTimeBeforeAndEndTimeAfter(
            FlashSaleStatus status, LocalDateTime start, LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FlashSale f WHERE f.name = :name")
    Optional<FlashSale> findByNameWithLock(@Param("name") String name);

    Optional<FlashSale> findByName(String name);
}
