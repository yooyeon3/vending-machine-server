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
    private final ProductRepository productRepository;

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

    // 💡 2. 상품 추가 (기존에 만드신 이미지 포함 로직 유지)
    @PostMapping("/admin/product/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam int price,
            @RequestParam int stock,
            @RequestParam(required = false, defaultValue = "/images/pepsi.png") String imageUrl) {

        // 이미지를 안 넣으면 기본 펩시 이미지로 세팅되게 방어
        Product product = Product.builder()
                .name(name)
                .price(price)
                .stock(stock)
                .imageUrl(imageUrl) // 이미지 경로 저장
                .build();
        productRepository.save(product);
        return "redirect:/admin";
    }

    // 💡 3. 재고 수정 (+ / - 버튼 클릭 시 동작, 최대 2개 제한 반영)
    @PostMapping("/admin/stock/update")
    public String updateStock(@RequestParam("productId") Long productId, @RequestParam("amount") int amount) {
        Product product = productRepository.findById(productId).orElse(null);

        if (product != null) {
            int newStock = product.getStock() + amount;

            // 💡 최소 0개, 최대 2개까지만 가능하도록 강력하게 제한
            if (newStock < 0) newStock = 0;
            if (newStock > 2) newStock = 2;

            product.setStock(newStock);
            productRepository.save(product);
        }
        return "redirect:/admin";
    }

    // (참고용) 기존 직접 입력 방식 수정 로직이 필요하다면 아래 주석을 풀고 사용하세요.
    /*
    @PostMapping("/admin/product/update")
    public String updateStockDirect(@RequestParam Long productId, @RequestParam int newStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.setStock(newStock);
        productRepository.save(product);
        return "redirect:/admin";
    }
    */

    // 4. 관리자 매출 통계 페이지
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