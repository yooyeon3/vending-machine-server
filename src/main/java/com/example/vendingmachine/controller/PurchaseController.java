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

            // 💡 수정된 부분: member.getName()이 아닌 getUsername()을 사용해야 할 수 있습니다.
            // (Member 클래스에 name 필드가 있다면 getName() 유지, 아이디만 있다면 getUsername() 사용)
            history.setBuyerName(member.getUsername());

            history.setPhoneNumber(member.getPhoneNumber()); // 전화번호 저장
            history.setProductName(product.getName());       // 상품명 저장

            purchaseHistoryRepository.save(history); // DB에 저장! (시간은 @PrePersist로 자동 저장됨)
        }

        return "redirect:/"; // 구매 후 메인 화면으로 돌아감
    }
}