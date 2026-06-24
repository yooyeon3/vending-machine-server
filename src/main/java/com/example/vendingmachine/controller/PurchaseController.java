package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.security.SecureRandom;

@Controller
public class PurchaseController {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final com.example.vendingmachine.service.MemberService memberService;

    // PIN 생성을 위한 문자열 및 랜덤 객체
    private static final String PIN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    // 생성자를 통해 의존성 주입
    public PurchaseController(MemberRepository memberRepository,
                              ProductRepository productRepository,
                              PurchaseHistoryRepository purchaseHistoryRepository,
                              com.example.vendingmachine.service.MemberService memberService) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
        this.memberService = memberService;
    }

    @PostMapping("/purchase")
    public String buyProduct(@RequestParam(value = "productId", required = false) Long productId,
                             @RequestParam(value = "productName", required = false) String productName,
                             @RequestParam(value = "usedPoints", defaultValue = "0") int usedPoints,
                             HttpSession session) {

        // 1. 세션 확인
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        Member member = memberRepository.findById(loginMember.getId()).orElse(null);
        Product product = null;

        // 2. ID 또는 이름으로 상품 찾기 (정합성 강화)
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
        }
        
        if (product == null && productName != null) {
            product = productRepository.findAll().stream()
                    .filter(p -> p.getName().equals(productName))
                    .findFirst().orElse(null);
        }

        // 3. 구매 처리
        if (member != null && product != null && product.getStock() > 0) {
            // 등급에 따른 혜택 계산
            String grade = memberService.getGrade(member);
            double discountRate = memberService.getDiscountRate(grade);
            double pointRate = memberService.getPointRate(grade);

            int originalPrice = product.getPrice();
            int discountAmount = (int) (originalPrice * discountRate);
            int basePrice = originalPrice - discountAmount;
            
            // 포인트 사용 처리
            int actualUsedPoints = Math.min(usedPoints, basePrice); // 결제 금액을 초과하여 포인트를 사용할 수 없음
            int memberCurrentPoints = member.getPoints() != null ? member.getPoints() : 0;
            actualUsedPoints = Math.min(actualUsedPoints, memberCurrentPoints); // 보유 포인트를 초과할 수 없음

            int finalPrice = basePrice - actualUsedPoints;
            int earnedPoints = (int) (finalPrice * pointRate);

            // 데이터 반영
            product.setStock(product.getStock() - 1);
            productRepository.save(product);

            // 포인트 차감 및 적립
            member.setPoints(memberCurrentPoints - actualUsedPoints + earnedPoints);
            memberRepository.save(member);
            
            // 세션 정보 갱신 (포인트 등)
            session.setAttribute("loginMember", member);

            // PIN 생성 (XXXX-XXXX 형식)
            String pinCode = generatePin();

            PurchaseHistory history = new PurchaseHistory();
            history.setBuyerName(member.getName());
            history.setPhoneNumber(member.getPhoneNumber());
            history.setProductName(product.getName());
            history.setPaidPrice(finalPrice);
            history.setEarnedPoints(earnedPoints);
            history.setPinCode(pinCode);
            history.setExpiryDate(LocalDateTime.now().plusDays(1)); // 유효기간 1일
            
            PurchaseHistory savedHistory = purchaseHistoryRepository.save(history);
            
            System.out.println(">>> 구매 성공: " + product.getName() + 
                               " (PIN: " + pinCode + ", 결제: " + finalPrice + "원, 포인트사용: " + actualUsedPoints + ")");
            
            return "redirect:/purchase/success?id=" + savedHistory.getId();
        } else {
            System.out.println(">>> 구매 실패: 상품 없음 또는 재고 부족 (ID: " + productId + ", Name: " + productName + ")");
        }

        return "redirect:/";
    }

    @GetMapping("/purchase/success")
    public String purchaseSuccess(@RequestParam(value = "id", required = false) Long id,
                                 @RequestParam(value = "ids", required = false) String ids,
                                 HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        if (ids != null && !ids.isEmpty()) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            List<PurchaseHistory> histories = purchaseHistoryRepository.findAllById(idList);
            
            // 권한 체크 및 필터링
            histories = histories.stream()
                    .filter(h -> h.getBuyerName().equals(loginMember.getName()))
                    .collect(Collectors.toList());
            
            if (histories.isEmpty()) return "redirect:/";
            
            model.addAttribute("histories", histories);
            model.addAttribute("history", histories.get(0)); // 호환성 유지
            return "purchase-success";
        } else if (id != null) {
            PurchaseHistory history = purchaseHistoryRepository.findById(id).orElse(null);
            if (history == null || !history.getBuyerName().equals(loginMember.getName())) {
                return "redirect:/";
            }
            model.addAttribute("history", history);
            model.addAttribute("histories", Arrays.asList(history));
            return "purchase-success";
        }

        return "redirect:/";
    }

    // 묶음 주문용 내부 DTO
    public static class BatchPurchaseRequest {
        private List<ItemRequest> items;
        private int usedPoints;
        public List<ItemRequest> getItems() { return items; }
        public void setItems(List<ItemRequest> items) { this.items = items; }
        public int getUsedPoints() { return usedPoints; }
        public void setUsedPoints(int usedPoints) { this.usedPoints = usedPoints; }

        public static class ItemRequest {
            private String name;
            private int qty;
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public int getQty() { return qty; }
            public void setQty(int qty) { this.qty = qty; }
        }
    }

    @PostMapping("/purchase/batch")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buyBatch(
            @RequestBody BatchPurchaseRequest request,
            HttpSession session) {

        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        Member member = memberRepository.findById(loginMember.getId()).orElse(null);
        if (member == null)
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "회원 정보를 찾을 수 없습니다."));

        // 1단계: 유효성 검사 + 데이터 수집
        LinkedHashMap<Product, Integer> purchaseMap = new LinkedHashMap<>();
        List<String> productLabels = new ArrayList<>();
        int totalOriginalPrice = 0;

        for (BatchPurchaseRequest.ItemRequest item : request.getItems()) {
            if (item.getQty() <= 0) continue;
            Product product = productRepository.findAll().stream()
                    .filter(p -> p.getName().equals(item.getName()))
                    .findFirst().orElse(null);
            if (product == null)
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", item.getName() + " 상품을 찾을 수 없습니다."));
            if (product.getStock() < item.getQty())
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", item.getName() + " 재고가 부족합니다."));

            purchaseMap.put(product, item.getQty());
            productLabels.add(product.getName() + (item.getQty() > 1 ? " x" + item.getQty() : ""));
            totalOriginalPrice += product.getPrice() * item.getQty();
        }

        if (purchaseMap.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "선택된 상품이 없습니다."));

        // 2단계: 가격 계산
        String grade = memberService.getGrade(member);
        double discountRate = memberService.getDiscountRate(grade);
        double pointRate = memberService.getPointRate(grade);

        int discountAmount = (int) (totalOriginalPrice * discountRate);
        int basePrice = totalOriginalPrice - discountAmount;
        int memberCurrentPoints = member.getPoints() != null ? member.getPoints() : 0;
        int actualUsedPoints = Math.min(request.getUsedPoints(), Math.min(basePrice, memberCurrentPoints));
        int finalPrice = basePrice - actualUsedPoints;
        int earnedPoints = (int) (finalPrice * pointRate);

        // 3단계: 재고 차감
        for (Map.Entry<Product, Integer> entry : purchaseMap.entrySet()) {
            Product p = entry.getKey();
            p.setStock(p.getStock() - entry.getValue());
            productRepository.save(p);
        }

        // 4단계: 포인트 반영
        member.setPoints(memberCurrentPoints - actualUsedPoints + earnedPoints);
        memberRepository.save(member);
        session.setAttribute("loginMember", member);

        // 5단계: 주문 내역 1건 + PIN 1개 생성
        String pinCode = generatePin();
        String combinedName = String.join(", ", productLabels);

        PurchaseHistory history = new PurchaseHistory();
        history.setBuyerName(member.getName());
        history.setPhoneNumber(member.getPhoneNumber());
        history.setProductName(combinedName);
        history.setPaidPrice(finalPrice);
        history.setEarnedPoints(earnedPoints);
        history.setPinCode(pinCode);
        history.setExpiryDate(LocalDateTime.now().plusDays(1));

        PurchaseHistory saved = purchaseHistoryRepository.save(history);

        System.out.println(">>> 묶음 주문 완료: [" + combinedName + "] PIN: " + pinCode + " / " + finalPrice + "원");

        return ResponseEntity.ok(Map.of("success", true, "id", saved.getId()));
    }

    // 라파3가 PIN 조회하는 API
    @GetMapping("/api/pin/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPin(@PathVariable Long id) {
        PurchaseHistory history = purchaseHistoryRepository.findById(id).orElse(null);
        if (history == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "pinCode", history.getPinCode(),
                "productName", history.getProductName()
        ));
    }

    // 라파3가 새 주문(PENDING) 폴링하는 API
    @GetMapping("/api/orders/pending")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPendingOrders() {
        List<PurchaseHistory> pending = purchaseHistoryRepository
                .findByDeliveryStatus(PurchaseHistory.DeliveryStatus.PENDING);
        List<Map<String, Object>> result = pending.stream().map(h -> Map.<String, Object>of(
                "id", h.getId(),
                "productName", h.getProductName(),
                "buyerName", h.getBuyerName(),
                "pinCode", h.getPinCode(),
                "purchaseTime", h.getPurchaseTime().toString()
        )).toList();
        return ResponseEntity.ok(result);
    }

    // 라파3가 배달 상태 업데이트하는 API
    @PostMapping("/api/orders/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        PurchaseHistory history = purchaseHistoryRepository.findById(id).orElse(null);
        if (history == null) return ResponseEntity.notFound().build();
        try {
            history.setDeliveryStatus(PurchaseHistory.DeliveryStatus.valueOf(body.get("status")));
            purchaseHistoryRepository.save(history);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "유효하지 않은 상태값"));
        }
    }

    // 고객이 PIN 입력했을때 확인하는 API
    @PostMapping("/api/pin/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPin(@RequestBody Map<String, String> body) {
        String pinCode = body.get("pinCode");
        PurchaseHistory history = purchaseHistoryRepository.findByPinCode(pinCode);
        if (history == null)
            return ResponseEntity.ok(Map.of("success", false, "message", "존재하지 않는 PIN"));
        if (history.isUsed())
            return ResponseEntity.ok(Map.of("success", false, "message", "이미 사용된 PIN"));
        if (history.getExpiryDate() != null && history.getExpiryDate().isBefore(LocalDateTime.now()))
            return ResponseEntity.ok(Map.of("success", false, "message", "만료된 PIN"));

        history.setUsed(true);
        history.setDeliveryStatus(PurchaseHistory.DeliveryStatus.DISPENSING);
        purchaseHistoryRepository.save(history);
        return ResponseEntity.ok(Map.of("success", true, "productName", history.getProductName()));
    }

    private String generatePin() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            sb.append(PIN_CHARS.charAt(random.nextInt(PIN_CHARS.length())));
        }
        return sb.toString();
    }
}