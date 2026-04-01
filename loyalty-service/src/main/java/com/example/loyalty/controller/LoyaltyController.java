package com.example.loyalty.controller;

import com.example.loyalty.dto.RedeemRequest;
import com.example.loyalty.entity.PointTransaction;
import com.example.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {
    private final LoyaltyService loyaltyService;

    @GetMapping("/{userId}/points")
    public ResponseEntity<Map<String, Object>> getPoints(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "currentPoints", loyaltyService.getCurrentPoints(userId)
        ));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<PointTransaction>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(loyaltyService.getHistory(userId));
    }

    @PostMapping("/{userId}/redeem")
    public ResponseEntity<Map<String, Object>> redeem(@PathVariable String userId,
                                                      @Valid @RequestBody RedeemRequest request) {
        loyaltyService.redeemPoints(userId, request.getPoints());
        return ResponseEntity.ok(Map.of(
                "message", "Redeem success",
                "userId", userId,
                "currentPoints", loyaltyService.getCurrentPoints(userId)
        ));
    }

    @GetMapping("/{userId}/tier")
    public ResponseEntity<Map<String, Object>> getTier(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "tier", loyaltyService.getTier(userId).name()
        ));
    }

//    @PostMapping("/referrals/generate")
//    public ResponseEntity<Map<String, String>> generateReferral(@RequestParam String userId) {
//        return ResponseEntity.ok(Map.of("code", loyaltyService.generateReferralCode(userId)));
//    }
//
//    @GetMapping("/referrals/{code}/validate")
//    public ResponseEntity<Map<String, Object>> validateReferral(@PathVariable String code) {
//        return ResponseEntity.ok(Map.of(
//                "code", code,
//                "valid", loyaltyService.validateReferralCode(code)
//        ));
//    }
}
