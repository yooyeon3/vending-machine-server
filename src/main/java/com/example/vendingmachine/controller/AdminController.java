package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final ProductRepository productRepository; // 💡 상품 DB 연동을 위해 추가됨

    // 두 개의 Repository를 모두 생성자로 주입받습니다.
    public AdminController(PurchaseHistoryRepository purchaseHistoryRepository,
                           ProductRepository productRepository) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
        this.productRepository = productRepository;
    }

    // 1. 관리자 메인 페이지 (재고 관리)
    @GetMapping("/admin")
    public String adminPage(Model model) {
        // DB에 있는 모든 상품(펩시, 레쓰비 등)을 꺼내서 화면에 넘겨줍니다.
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "admin";
    }

    // 💡 2. 재고 + / - 업데이트 기능 (새로 추가된 핵심 로직)
    @PostMapping("/admin/stock/update")
    public String updateStock(@RequestParam("productId") Long productId,
                              @RequestParam("amount") int amount) { // amount는 1 또는 -1

        // 클릭한 상품을 DB에서 찾습니다.
        Product product = productRepository.findById(productId).orElse(null);

        if (product != null) {
            // 현재 재고에 amount(+1 또는 -1)를 더합니다.
            int newStock = product.getStock() + amount;

            // 재고가 마이너스가 되지 않도록 방어
            if (newStock < 0) {
                newStock = 0;
            }

            product.setStock(newStock); // 새 재고 세팅
            productRepository.save(product); // DB에 덮어쓰기(저장)!
        }

        // 저장이 완료되면 다시 관리자 메인화면으로 새로고침
        return "redirect:/admin";
    }

    // 3. 관리자 매출 통계 페이지 (기존 코드 유지)
    @GetMapping("/admin/statistics")
    public String salesStatistics(Model model) {
        List<PurchaseHistory> histories = purchaseHistoryRepository.findAll();
        model.addAttribute("histories", histories);

        int[] hourlySales = new int[24];
        for (PurchaseHistory history : histories) {
            if (history.getPurchaseTime() != null) {
                int hour = history.getPurchaseTime().getHour();
                hourlySales[hour]++;
            }
        }
        model.addAttribute("hourlySales", hourlySales);

        return "admin_statistics";
    }
}