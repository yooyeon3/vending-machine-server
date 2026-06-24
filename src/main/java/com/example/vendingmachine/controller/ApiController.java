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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (loginMember == null) return Map.of("active", false);

        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        List<PurchaseHistory> orders = purchaseHistoryRepository
                .findByBuyerNameAndPurchaseTimeAfterOrderByPurchaseTimeAsc(loginMember.getName(), since);

        if (orders.isEmpty()) return Map.of("active", false);

        // 로봇 일정 시뮬레이션: 5분 배달 + 2분 복귀 = 7분 사이클
        final int DELIVERY_SECS = 60;
        final int CYCLE_SECS = 120;
        LocalDateTime now = LocalDateTime.now();

        // 로봇 사이클(7분) 안에 들어온 주문은 같은 배달 묶음으로 처리
        // (로봇이 이동 중이면 추가 주문도 함께 가져옴)
        List<List<PurchaseHistory>> batches = new ArrayList<>();
        List<PurchaseHistory> group = new ArrayList<>();
        LocalDateTime cycleEnd = orders.get(0).getPurchaseTime().plusSeconds(CYCLE_SECS);
        group.add(orders.get(0));
        for (int i = 1; i < orders.size(); i++) {
            LocalDateTime orderTime = orders.get(i).getPurchaseTime();
            if (!orderTime.isAfter(cycleEnd)) {
                group.add(orders.get(i));
            } else {
                batches.add(new ArrayList<>(group));
                group.clear();
                group.add(orders.get(i));
                cycleEnd = orderTime.plusSeconds(CYCLE_SECS);
            }
        }
        batches.add(group);

        List<LocalDateTime> deliveryStarts = new ArrayList<>();
        LocalDateTime robotFreeAt = null;
        for (List<PurchaseHistory> batch : batches) {
            LocalDateTime batchTime = batch.get(0).getPurchaseTime();
            LocalDateTime start = (robotFreeAt == null || batchTime.isAfter(robotFreeAt))
                    ? batchTime : robotFreeAt;
            deliveryStarts.add(start);
            robotFreeAt = start.plusSeconds(CYCLE_SECS);
        }

        // 현재 진행 중인 배달 묶음 찾기
        int activeIdx = -1;
        for (int i = 0; i < batches.size(); i++) {
            LocalDateTime start = deliveryStarts.get(i);
            if (!now.isBefore(start) && now.isBefore(start.plusSeconds(CYCLE_SECS))) {
                activeIdx = i;
                break;
            }
        }
        if (activeIdx == -1) return Map.of("active", false);

        List<PurchaseHistory> activeBatch = batches.get(activeIdx);
        LocalDateTime deliveryStart = deliveryStarts.get(activeIdx);
        long elapsed = ChronoUnit.SECONDS.between(deliveryStart, now);
        boolean arrived = elapsed >= DELIVERY_SECS;
        int progress = (int) Math.min(100, elapsed * 100L / DELIVERY_SECS);

        // 품목별 갯수 요약 (ex. "콜라 x2, 사이다")
        Map<String, Long> counts = activeBatch.stream()
                .collect(Collectors.groupingBy(PurchaseHistory::getProductName, Collectors.counting()));
        String productSummary = counts.entrySet().stream()
                .map(e -> e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : ""))
                .collect(Collectors.joining(", "));

        // 대기 큐
        int queueCount = batches.size() - activeIdx - 1;
        String nextProductName = queueCount > 0
                ? batches.get(activeIdx + 1).get(0).getProductName() : null;

        long remaining = Math.max(0, DELIVERY_SECS - elapsed);
        String eta = arrived ? "도착!" : (remaining / 60) + "분 " + (remaining % 60) + "초";

        // 출발지(8,12) → 목적지(85,80): 로봇 연동 시 실제 좌표로 교체
        double startX = 8.0, startY = 12.0, endX = 85.0, endY = 80.0;
        double t = Math.min(1.0, (double) elapsed / DELIVERY_SECS);

        Map<String, Object> result = new HashMap<>();
        result.put("active", true);
        result.put("progress", progress);
        result.put("arrived", arrived);
        result.put("productName", productSummary);
        result.put("queueCount", queueCount);
        result.put("nextProductName", nextProductName);
        result.put("robotX", startX + (endX - startX) * t);
        result.put("robotY", startY + (endY - startY) * t);
        result.put("destX", endX);
        result.put("destY", endY);
        result.put("eta", eta);
        return result;
    }

    // Pi 3 전용 - 세션 없이 최근 주문 기반으로 동일한 시뮬레이션
    @GetMapping("/api/robot/delivery/status")
    public Map<String, Object> getRobotDeliveryStatus() {
        final int DELIVERY_SECS = 60;
        final int CYCLE_SECS = 120;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusSeconds(CYCLE_SECS);

        List<PurchaseHistory> orders = purchaseHistoryRepository
                .findByDeliveryStatusAndPurchaseTimeAfterOrderByPurchaseTimeAsc(
                        PurchaseHistory.DeliveryStatus.PENDING, since);

        if (orders.isEmpty()) return Map.of("active", false, "state", "IDLE");

        PurchaseHistory order = orders.get(0);
        long elapsed = ChronoUnit.SECONDS.between(order.getPurchaseTime(), now);
        boolean arrived = elapsed >= DELIVERY_SECS;
        int progress = (int) Math.min(100, elapsed * 100L / DELIVERY_SECS);
        long remaining = Math.max(0, DELIVERY_SECS - elapsed);
        String eta = arrived ? "도착!" : (remaining / 60) + "분 " + (remaining % 60) + "초";

        double t = Math.min(1.0, (double) elapsed / DELIVERY_SECS);
        double startX = 8.0, startY = 12.0, endX = 85.0, endY = 80.0;

        Map<String, Object> result = new HashMap<>();
        result.put("active",      true);
        result.put("state",       arrived ? "ARRIVED" : "MOVING");
        result.put("arrived",     arrived);
        result.put("progress",    progress);
        result.put("eta",         eta);
        result.put("orderId",     order.getId());
        result.put("pinCode",     order.getPinCode());
        result.put("productName", order.getProductName());
        result.put("robotX",      startX + (endX - startX) * t);
        result.put("robotY",      startY + (endY - startY) * t);
        result.put("destX",       endX);
        result.put("destY",       endY);
        return result;
    }
}