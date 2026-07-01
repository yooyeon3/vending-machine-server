package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.service.ChatService;
import com.example.vendingmachine.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderViewController {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final MemberService memberService;
    private final ChatService chatService;

    @GetMapping("/my")
    public String myOrders(HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/login";
        }

        // 현재 사용자의 주문 내역 조회 (이름 기준 - 현재 시스템 사양)
        List<PurchaseHistory> orders = purchaseHistoryRepository.findByBuyerName(loginMember.getName());
        
        // 내림차순 정렬 (최신순)
        orders.sort((a, b) -> b.getPurchaseTime().compareTo(a.getPurchaseTime()));

        // 등급 정보 조회
        MemberService.GradeProgress progress = memberService.getGradeProgress(loginMember);

        // 최애 음료 TOP 3 조회 (묶음 구매 'x2' 등 통합 로직 적용)
        java.util.Map<String, Integer> productCountMap = new java.util.HashMap<>();
        for (PurchaseHistory order : orders) {
            String name = order.getProductName();
            int qty = 1;
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(.*?)\\s*[xX]\\s*(\\d+)$").matcher(name);
            if (matcher.matches()) {
                name = matcher.group(1).trim();
                qty = Integer.parseInt(matcher.group(2));
            }
            productCountMap.put(name, productCountMap.getOrDefault(name, 0) + qty);
        }

        List<Map<String, Object>> topProducts = productCountMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", (long) e.getValue()))
                .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("progress", progress);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("totalSpent", orders.stream().mapToInt(o -> o.getPaidPrice() != null ? o.getPaidPrice() : 0).sum());
        model.addAttribute("totalEarnedPoints", orders.stream().mapToInt(o -> o.getEarnedPoints() != null ? o.getEarnedPoints() : 0).sum());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("now", java.time.LocalDateTime.now());
        model.addAttribute("unreadCount", chatService.countUnreadForUser(loginMember.getUsername()));

        return "order-history";
    }

    @GetMapping("/delivery/{id}")
    public String deliveryTrack(@PathVariable Long id, HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        PurchaseHistory history = purchaseHistoryRepository.findById(id).orElse(null);
        if (history == null || !history.getBuyerName().equals(loginMember.getName())) {
            return "redirect:/";
        }

        model.addAttribute("history", history);
        model.addAttribute("unreadCount", chatService.countUnreadForUser(loginMember.getUsername()));
        return "delivery-track";
    }
}
