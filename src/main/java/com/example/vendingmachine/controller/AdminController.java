package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public AdminController(PurchaseHistoryRepository purchaseHistoryRepository) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }

    @GetMapping("/admin/statistics")
    public String salesStatistics(Model model) {
        List<PurchaseHistory> histories = purchaseHistoryRepository.findAll();
        model.addAttribute("histories", histories);

        // 💡 자바스크립트 에러 방지를 위해 서버에서 0시~23시 통계를 미리 계산합니다.
        int[] hourlySales = new int[24];
        for (PurchaseHistory history : histories) {
            if (history.getPurchaseTime() != null) {
                int hour = history.getPurchaseTime().getHour(); // 구매 시간(Hour) 추출
                hourlySales[hour]++;
            }
        }
        // 계산 완료된 배열을 모델에 담아 전송
        model.addAttribute("hourlySales", hourlySales);

        return "admin_statistics";
    }
}