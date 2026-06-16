package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PurchaseController {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    // 생성자를 통해 의존성 주입
    public PurchaseController(MemberRepository memberRepository,
                              ProductRepository productRepository,
                              PurchaseHistoryRepository purchaseHistoryRepository) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    @PostMapping("/purchase")
    public String buyProduct(@RequestParam(value = "productId", required = false) Long productId,
                             @RequestParam(value = "productName", required = false) String productName,
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
        
        // ID로 못 찾았거나 이름이 넘어왔다면 이름으로 다시 시도
        if (product == null && productName != null) {
            product = productRepository.findAll().stream()
                    .filter(p -> p.getName().equals(productName))
                    .findFirst().orElse(null);
        }

        // 3. 구매 처리
        if (member != null && product != null && product.getStock() > 0) {
            product.setStock(product.getStock() - 1);
            productRepository.save(product);

            PurchaseHistory history = new PurchaseHistory();
            history.setBuyerName(member.getName());
            history.setPhoneNumber(member.getPhoneNumber());
            history.setProductName(product.getName());
            purchaseHistoryRepository.save(history);
            
            System.out.println(">>> 구매 성공: " + product.getName() + " (구매자: " + member.getName() + ")");
        } else {
            System.out.println(">>> 구매 실패: 상품 없음 또는 재고 부족 (ID: " + productId + ", Name: " + productName + ")");
        }

        return "redirect:/";
    }
}