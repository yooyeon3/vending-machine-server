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

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("chatCount", chatService.countUnreadForAdmin());
        model.addAttribute("products", productRepository.findAll());
        return "admin";
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
        return "redirect:/admin";
    }

    @PostMapping("/admin/stock/update")
    public String updateStock(@RequestParam Long productId, @RequestParam int amount) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            int newStock = product.getStock() + amount;
            if (newStock < 0) newStock = 0;
            if (newStock > 2) newStock = 2;
            product.setStock(newStock);
            productRepository.save(product);
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
