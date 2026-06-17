package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.repository.InquiryRepository;
import com.example.vendingmachine.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/member")
public class AdminMemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final InquiryRepository inquiryRepository;

    @GetMapping
    public String memberListPage(Model model) {
        List<Member> members = memberService.findAll();
        
        // DB의 모든 상품 정보를 가져와 가격 맵 생성
        Map<String, Integer> productPrices = productRepository.findAll().stream()
                .collect(Collectors.toMap(
                    com.example.vendingmachine.domain.Product::getName,
                    com.example.vendingmachine.domain.Product::getPrice,
                    (existing, replacement) -> existing // 중복 이름 발생 시 기존 값 유지
                ));
        
        // 기본 상품들 보정 (혹시 이름이 바뀌었을 경우 대비)
        productPrices.putIfAbsent("펩시 콜라", 1500);
        productPrices.putIfAbsent("레쓰비 마일드 커피", 1200);

        int totalRevenue = 0;
        int diamondCount = 0;

        List<Map<String, Object>> memberStats = members.stream().map(member -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("info", member);
            
            String buyerName = (member.getName() != null) ? member.getName() : "";
            List<com.example.vendingmachine.domain.PurchaseHistory> histories = purchaseHistoryRepository.findByBuyerName(buyerName);
            
            int totalSpent = histories.stream()
                    .mapToInt(h -> h.getPaidPrice() != null ? h.getPaidPrice() : productPrices.getOrDefault(h.getProductName(), 0))
                    .sum();
            
            stat.put("purchaseCount", histories.size());
            stat.put("totalSpent", totalSpent);
            stat.put("inquiryCount", (int) inquiryRepository.countByUsername(member.getUsername()));
            stat.put("activityScore", memberService.getActivityScore(member));
            stat.put("grade", memberService.getGrade(member));

            return stat;
        }).collect(Collectors.toList());

        // 통계 합계 계산
        for (Map<String, Object> stat : memberStats) {
            totalRevenue += (int) stat.get("totalSpent");
            if ("DIAMOND".equals(stat.get("grade"))) {
                diamondCount++;
            }
        }

        model.addAttribute("members", memberStats);
        model.addAttribute("totalCount", members.size());
        model.addAttribute("diamondCount", diamondCount);
        model.addAttribute("totalRevenue", totalRevenue);
        
        return "admin-member";
    }

    @PostMapping("/update")
    public String updateMember(@RequestParam Long id, 
                               @RequestParam String name, 
                               @RequestParam String phoneNumber,
                               @RequestParam(required = false) String adminMemo) {
        memberService.updateMember(id, name, phoneNumber, adminMemo);
        return "redirect:/admin/member";
    }

    @PostMapping("/delete/{id}")
    public String deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return "redirect:/admin/member";
    }
}
