package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
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
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final InquiryRepository inquiryRepository;

    @GetMapping
    public String memberListPage(Model model) {
        List<Member> members = memberService.findAll();
        
        Map<String, Integer> productPrices = new HashMap<>();
        productPrices.put("펩시 콜라", 1500);
        productPrices.put("레쓰비 마일드 커피", 1200);

        int totalRevenue = 0;
        int diamondCount = 0;

        List<Map<String, Object>> memberStats = members.stream().map(member -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("info", member);
            
            String buyerName = (member.getName() != null) ? member.getName() : "";
            List<com.example.vendingmachine.domain.PurchaseHistory> histories = purchaseHistoryRepository.findByBuyerName(buyerName);
            
            int totalSpent = histories.stream()
                    .mapToInt(h -> productPrices.getOrDefault(h.getProductName(), 0))
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
