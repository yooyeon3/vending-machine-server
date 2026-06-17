package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ApiController {

    private final ProductRepository productRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public ApiController(ProductRepository productRepository,
                         PurchaseHistoryRepository purchaseHistoryRepository) {
        this.productRepository = productRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    @GetMapping("/api/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    // 배달 상태 조회 - 나중에 실제 로봇 좌표로 교체
    @GetMapping("/api/delivery/status")
    public Map<String, Object> getDeliveryStatus(HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            return Map.of("active", false);
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(6);
        List<PurchaseHistory> recent = purchaseHistoryRepository
                .findByBuyerNameAndPurchaseTimeAfterOrderByPurchaseTimeDesc(loginMember.getName(), since);

        if (recent.isEmpty()) {
            return Map.of("active", false);
        }

        PurchaseHistory latest = recent.get(0);
        long elapsed = ChronoUnit.SECONDS.between(latest.getPurchaseTime(), LocalDateTime.now());
        int totalSec = 300; // 5분 배달 시뮬레이션 (로봇 연동 시 실제 좌표로 교체)
        int progress = (int) Math.min(100, elapsed * 100L / totalSec);

        // 출발지(8,12) → 목적지(85,80): 로봇 연동 시 실제 좌표로 교체
        double startX = 8.0, startY = 12.0, endX = 85.0, endY = 80.0;
        double t = progress / 100.0;

        long remaining = Math.max(0, totalSec - elapsed);
        String eta = (remaining / 60) + "분 " + (remaining % 60) + "초";

        Map<String, Object> result = new HashMap<>();
        result.put("active", true);
        result.put("progress", progress);
        result.put("arrived", progress >= 100);
        result.put("productName", latest.getProductName());
        result.put("robotX", startX + (endX - startX) * t);
        result.put("robotY", startY + (endY - startY) * t);
        result.put("destX", endX);
        result.put("destY", endY);
        result.put("eta", eta);
        return result;
    }
}