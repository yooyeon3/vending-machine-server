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

        // 세션에서 로그인된 회원 정보 가져오기
        Member loginMember = (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login"; // 로그인이 안 되어있으면 로그인창으로
        }

        // 최신 회원 정보와 클릭한 상품 정보 DB 조회
        Member member = memberRepository.findById(loginMember.getId()).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);

        if (member != null && product != null) {
            PurchaseHistory history = new PurchaseHistory();

            // 💡 중요: member.getUsername()(아이디) 대신 member.getName()(실제이름)을 저장합니다!
            history.setBuyerName(member.getName());

            history.setPhoneNumber(member.getPhoneNumber());

            // 💡 DB에 등록된 실제 상품명(펩시 콜라 또는 레쓰비 마일드 커피)이 그대로 저장됩니다.
            history.setProductName(product.getName());

            purchaseHistoryRepository.save(history);
        }

        return "redirect:/";
    }
}