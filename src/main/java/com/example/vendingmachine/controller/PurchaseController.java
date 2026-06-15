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

    public PurchaseController(MemberRepository memberRepository,
                              ProductRepository productRepository,
                              PurchaseHistoryRepository purchaseHistoryRepository) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    @PostMapping("/purchase")
    public String buyProduct(@RequestParam("productId") Long productId, HttpSession session) {

        // 💡 index.html의 로그인 시스템에 맞춰 세션에서 회원 정보를 꺼냅니다.
        Member loginMember = (Member) session.getAttribute("loginMember");

        // 로그인이 안 되어 있다면 로그인 페이지로 리다이렉트
        if (loginMember == null) {
            return "redirect:/login";
        }

        // 세션에 있는 정보로 DB에서 최신 회원 정보와 상품 정보를 조회합니다.
        Member member = memberRepository.findById(loginMember.getId()).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);

        if (member != null && product != null) {
            // 매출 내역 객체 생성 및 저장
            PurchaseHistory history = new PurchaseHistory();
            history.setBuyerName(member.getName()); // 실제 이름 저장
            history.setPhoneNumber(member.getPhoneNumber()); // 전화번호 저장
            history.setProductName(product.getName()); // 상품명 저장

            purchaseHistoryRepository.save(history); // 💡 이제 성공적으로 DB에 저장됩니다.
        }

        return "redirect:/";
    }
}