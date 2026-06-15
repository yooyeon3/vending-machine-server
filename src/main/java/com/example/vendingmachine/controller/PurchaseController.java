package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class PurchaseController {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    // 생성자를 통해 Repository(DB 접근 객체)들을 연결합니다.
    public PurchaseController(MemberRepository memberRepository,
                              ProductRepository productRepository,
                              PurchaseHistoryRepository purchaseHistoryRepository) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    // 사용자 화면에서 "구매하기" 버튼을 눌렀을 때 실행되는 주소 (/purchase)
    @PostMapping("/purchase")
    public String buyProduct(@RequestParam("productId") Long productId, Principal principal) {

        // 1. 누가 샀는지 찾기 (현재 로그인한 사용자의 아이디를 가져옵니다)
        String username = principal.getName();
        // DB에서 해당 사용자 정보를 꺼내옵니다 (memberRepository에 findByUsername이 구현되어 있어야 합니다)
        Member member = memberRepository.findByUsername(username).orElse(null);

        // 2. 뭘 샀는지 찾기 (클릭한 상품의 번호로 상품 정보를 꺼내옵니다)
        Product product = productRepository.findById(productId).orElse(null);

        // 사용자와 상품이 모두 정상적으로 존재할 때만 구매 기록을 저장합니다.
        if (member != null && product != null) {

            // 💡 3. 질문자님이 작성해주신 '매출 통계 저장 로직'이 여기에 들어갑니다! 💡
            PurchaseHistory history = new PurchaseHistory();
            history.setBuyerName(member.getUsername());
            history.setPhoneNumber(member.getPhoneNumber()); // Member 테이블에 전화번호가 있어야 함
            history.setProductName(product.getName());

            purchaseHistoryRepository.save(history); // DB에 구매 이력 저장!

            // (참고) 나중에 여기에 사용자의 잔액(포인트)을 깎거나 상품 재고를 줄이는 코드를 추가할 수 있습니다.
        }

        // 구매가 완료되면 다시 메인 화면(자판기 화면)으로 돌아갑니다.
        return "redirect:/";
    }
}