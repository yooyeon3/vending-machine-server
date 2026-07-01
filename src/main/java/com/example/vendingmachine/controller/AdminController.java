package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final ChatService chatService;
    private final ProductRepository productRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    private void notifyStockChange() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://192.168.1.106:4000/api/stock-updated"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> System.out.println("📦 [재고 알림] Node.js 서버에 재고 변경 알림 전송 완료"))
                    .exceptionally(e -> {
                        System.out.println("⚠️ [재고 알림] Node.js 서버 알림 전송 실패: " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.out.println("⚠️ [재고 알림] Node.js 서버 알림 설정 실패: " + e.getMessage());
        }
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("chatCount", chatService.countUnreadForAdmin());
        model.addAttribute("products", productRepository.findAll());
        return "admin";
    }

    // 로봇 관제 시스템 페이지 연결
    @GetMapping("/admin/robot-control")
    public String robotControlPage() {
        return "robot-control";
    }

    @PostMapping("/admin/product/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam int price,
            @RequestParam int stock,
            @RequestParam(required = false, defaultValue = "/images/pepsi.png") String imageUrl) {

        Product product = Product.builder()
                .name(name)
                .price(price)
                .stock(stock)
                .imageUrl(imageUrl)
                .build();
        productRepository.save(product);
        notifyStockChange(); // ✅ 추가
        return "redirect:/admin";
    }

    @PostMapping("/admin/stock/update")
    public String updateStock(@RequestParam Long productId, @RequestParam int amount) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            int newStock = product.getStock() + amount;
            if (newStock < 0) newStock = 0;
            product.setStock(newStock);
            productRepository.save(product);
            notifyStockChange(); // ✅ 추가
        }
        return "redirect:/admin";
    }

    @GetMapping("/admin/statistics")
    public String salesStatistics(Model model) {
        List<PurchaseHistory> histories = purchaseHistoryRepository.findAll();
        model.addAttribute("histories", histories);

        int[] hourlySales = new int[24];
        for (PurchaseHistory history : histories) {
            if (history.getPurchaseTime() != null) {
                hourlySales[history.getPurchaseTime().getHour()]++;
            }
        }
        model.addAttribute("hourlySales", hourlySales);
        return "admin_statistics";
    }
}