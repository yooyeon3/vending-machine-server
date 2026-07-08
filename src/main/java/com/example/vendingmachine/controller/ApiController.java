package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.service.KioskStateService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
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
    private final MemberRepository memberRepository;
    private final KioskStateService kioskStateService;

    public ApiController(ProductRepository productRepository,
                         PurchaseHistoryRepository purchaseHistoryRepository,
                         MemberRepository memberRepository,
                         KioskStateService kioskStateService) {
        this.productRepository = productRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
        this.memberRepository = memberRepository;
        this.kioskStateService = kioskStateService;
    }

    @PostMapping("/api/kiosk/busy")
    public Map<String, Object> setKioskBusy() {
        kioskStateService.setBusy();
        return Map.of("success", true);
    }

    @PostMapping("/api/kiosk/free")
    public Map<String, Object> setKioskFree() {
        kioskStateService.setFree();
        return Map.of("success", true);
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
                .findByBuyerNameAndPurchaseTimeAfterOrderByPurchaseTimeAsc(loginMember.getName(), since)
                .stream()
                .filter(o -> !o.isUsed() && o.getDeliveryStatus() != PurchaseHistory.DeliveryStatus.DELIVERED)
                .collect(Collectors.toList());

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

        PurchaseHistory first = activeBatch.get(0);
        double endX = first.getDestX() != null ? first.getDestX() : 0.0;
        double endY = first.getDestY() != null ? first.getDestY() : 0.0;
        double startX = 0.0, startY = 0.0; // 로봇 시작 위치 (가정)
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

    // Pi 3 전용 - 배치 처리 포함 배달 상태
    @GetMapping("/api/robot/delivery/status")
    public Map<String, Object> getRobotDeliveryStatus() {
        final int DELIVERY_SECS = 60;
        final int CYCLE_SECS = 120;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusSeconds(CYCLE_SECS);

        List<PurchaseHistory> orders = purchaseHistoryRepository
                .findByDeliveryStatusAndPurchaseTimeAfterOrderByPurchaseTimeAsc(
                        PurchaseHistory.DeliveryStatus.PENDING, since)
                .stream()
                .filter(o -> !o.isUsed())
                .collect(Collectors.toList());

        if (orders.isEmpty()) return Map.of("active", false, "state", "IDLE");

        // 가장 오래된 주문 기준으로 시간 계산 (배치 대표)
        PurchaseHistory first = orders.get(0);
        long elapsed = ChronoUnit.SECONDS.between(first.getPurchaseTime(), now);
        boolean arrived = elapsed >= DELIVERY_SECS;
        int progress = (int) Math.min(100, elapsed * 100L / DELIVERY_SECS);
        long remaining = Math.max(0, DELIVERY_SECS - elapsed);
        String eta = arrived ? "도착!" : (remaining / 60) + "분 " + (remaining % 60) + "초";

        // 배치 내 상품 요약
        Map<String, Long> counts = orders.stream()
                .collect(Collectors.groupingBy(PurchaseHistory::getProductName, Collectors.counting()));
        String productSummary = counts.entrySet().stream()
                .map(e -> e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : ""))
                .collect(Collectors.joining(", "));

        // 대표 PIN (첫 주문)
        double t = Math.min(1.0, (double) elapsed / DELIVERY_SECS);
        double startX = 8.0, startY = 12.0, endX = 85.0, endY = 80.0;

        Map<String, Object> result = new HashMap<>();
        result.put("active",      true);
        result.put("state",       arrived ? "ARRIVED" : "MOVING");
        result.put("arrived",     arrived);
        result.put("progress",    progress);
        result.put("eta",         eta);
        result.put("orderId",     first.getId());
        result.put("pinCode",     first.getPinCode());
        result.put("productName", productSummary);
        result.put("robotX",      startX + (endX - startX) * t);
        result.put("robotY",      startY + (endY - startY) * t);
        result.put("destX",       endX);
        result.put("destY",       endY);
        return result;
    }

    // 도착 후 미수령 시 자동 취소 및 환불
    @PostMapping("/api/delivery/cancel")
    public ResponseEntity<Map<String, Object>> cancelDelivery(HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null)
            return ResponseEntity.status(401).body(Map.of("success", false));

        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        List<PurchaseHistory> orders = purchaseHistoryRepository
                .findByBuyerNameAndPurchaseTimeAfterOrderByPurchaseTimeAsc(loginMember.getName(), since)
                .stream()
                .filter(o -> !o.isUsed() && o.getDeliveryStatus() == PurchaseHistory.DeliveryStatus.PENDING)
                .collect(Collectors.toList());

        if (orders.isEmpty())
            return ResponseEntity.ok(Map.of("success", false, "message", "취소할 주문이 없습니다."));

        Member member = memberRepository.findById(loginMember.getId()).orElse(null);

        for (PurchaseHistory order : orders) {
            // 재고 복구 (예: "펩시 콜라 x2, 레쓰비 마일드 커피" 파싱)
            for (String part : order.getProductName().split(", ")) {
                int qty = 1;
                String name = part.trim();
                if (name.contains(" x")) {
                    int idx = name.lastIndexOf(" x");
                    try {
                        qty = Integer.parseInt(name.substring(idx + 2));
                        name = name.substring(0, idx);
                    } catch (NumberFormatException ignored) {}
                }
                final String productName = name;
                final int productQty = qty;
                productRepository.findAll().stream()
                        .filter(p -> p.getName().equals(productName))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setStock(p.getStock() + productQty);
                            productRepository.save(p);
                        });
            }

            // 포인트 환불: 사용 포인트 복구 + 적립 포인트 회수
            if (member != null) {
                int current = member.getPoints() != null ? member.getPoints() : 0;
                int used    = order.getUsedPoints()   != null ? order.getUsedPoints()   : 0;
                int earned  = order.getEarnedPoints() != null ? order.getEarnedPoints() : 0;
                member.setPoints(current + used - earned);
                memberRepository.save(member);
                session.setAttribute("loginMember", member);
            }

            order.setUsed(true);
            order.setDeliveryStatus(PurchaseHistory.DeliveryStatus.CANCELLED);
            purchaseHistoryRepository.save(order);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    // Pi 3가 배출할 주문 조회 (PIN 인증 완료된 것)
    @GetMapping("/api/robot/dispensing")
    public List<Map<String, Object>> getDispensingOrders() {
        return purchaseHistoryRepository
                .findByDeliveryStatus(PurchaseHistory.DeliveryStatus.DISPENSING)
                .stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(),
                        "productName", o.getProductName()
                ))
                .collect(Collectors.toList());
    }

    // Pi 3 배출 완료 처리
    @PostMapping("/api/robot/dispensing/{id}/done")
    public Map<String, Object> dispensingDone(@PathVariable Long id) {
        PurchaseHistory h = purchaseHistoryRepository.findById(id).orElse(null);
        if (h == null) return Map.of("success", false);
        h.setDeliveryStatus(PurchaseHistory.DeliveryStatus.DELIVERED);
        purchaseHistoryRepository.save(h);
        return Map.of("success", true);
    }

    // --- 로봇 배달 연동 API (Node.js 백엔드로 위임) ---

    @PostMapping("/api/delivery/queue")
    public Map<String, Object> joinQueue(@RequestParam Long orderId, @RequestParam Double destX, @RequestParam Double destY) {
        PurchaseHistory order = purchaseHistoryRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setDestX(destX);
            order.setDestY(destY);
            order.setDeliveryStatus(PurchaseHistory.DeliveryStatus.DELIVERING); // 상태를 바로 배달중으로 둡니다(Node가 실제 제어).
            purchaseHistoryRepository.save(order);
            
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                // Web은 ROS 좌표(미터)를 사용하지만, Node.js 서버(App 기준)는 픽셀 좌표를 기대합니다.
                // 픽셀 좌표로 변환하여 민호님의 서버와 규격을 맞춥니다.
                double pixelX = (destX - (-10.0)) / 0.05;
                double pixelY = 384.0 - ((destY - (-10.0)) / 0.05);
                
                Map<String, Object> targetPos = Map.of("x", pixelX, "y", pixelY);
                Map<String, Object> body = Map.of("targetPos", targetPos, "productId", orderId.toString());
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity("http://192.168.0.70:4000/api/web/call-robot", entity, Map.class);
                
                return Map.of("success", true, "nodeResponse", response.getBody() != null ? response.getBody() : "");
            } catch (Exception e) {
                e.printStackTrace();
                return Map.of("success", false, "message", "Node.js 서버 연동 실패");
            }
        }
        return Map.of("success", false, "message", "주문을 찾을 수 없습니다.");
    }
}