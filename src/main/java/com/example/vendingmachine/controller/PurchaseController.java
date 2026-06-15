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
    public String buyProduct(@RequestParam("productId") Long productId, HttpSession session) {

        // 1. 세션에서 현재 로그인된 회원 정보 확인
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/login"; // 비로그인 시 로그인 페이지로
        }

        // 2. DB에서 최신 회원 정보와 상품 정보 조회
        Member member = memberRepository.findById(loginMember.getId()).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);

        // 3. 상품이 존재하고 재고가 1개 이상일 때만 구매 프로세스 진행
        if (member != null && product != null && product.getStock() > 0) {

            // [구매 프로세스 1] 실시간 재고 1개 감소 및 DB 저장
            product.setStock(product.getStock() - 1);
            productRepository.save(product);

            // [구매 프로세스 2] 매출 통계 페이지를 위한 기록 저장
            PurchaseHistory history = new PurchaseHistory();
            history.setBuyerName(member.getName()); // 실제 이름 저장
            history.setPhoneNumber(member.getPhoneNumber()); // 전화번호 저장
            history.setProductName(product.getName()); // 상품명 저장
            purchaseHistoryRepository.save(history); // 최종적으로 DB에 매출 기록
        }

        // 4. 모든 작업이 완료되면 메인 화면으로 리다이렉트
        return "redirect:/";
    }
}