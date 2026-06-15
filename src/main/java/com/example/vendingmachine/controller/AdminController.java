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

    // 생성자 주입 방식을 사용하여 Repository를 가져옵니다. (스프링 부트 추천 방식)
    public AdminController(PurchaseHistoryRepository purchaseHistoryRepository) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    // 1. 관리자 메인 페이지 (http://localhost:8080/admin)
    // ADMIN 권한이 있어야만 접근 가능 (SecurityConfig에서 설정됨)
    @GetMapping("/admin")
    public String adminPage() {
        return "admin"; // src/main/resources/templates/admin.html 파일을 보여줍니다.
    }

    // 2. 관리자 매출 통계 페이지 (http://localhost:8080/admin/statistics)
    @GetMapping("/admin/statistics")
    public String salesStatistics(Model model) {
        // DB에서 모든 구매 내역을 가져옵니다.
        List<PurchaseHistory> histories = purchaseHistoryRepository.findAll();

        // 타임리프 화면(HTML)으로 데이터를 넘겨줍니다.
        model.addAttribute("histories", histories);

        return "admin_statistics"; // src/main/resources/templates/admin_statistics.html 파일을 보여줍니다.
    }
}