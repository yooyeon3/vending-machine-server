package com.example.vendingmachine.config;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component // 스프링이 서버를 켤 때 이 클래스를 자동으로 인식하여 실행하도록 합니다.
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 필요한 Repository와 Encoder를 주입받습니다.
    public DataInitializer(ProductRepository productRepository, MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // 상품 데이터 초기화
        productRepository.deleteAll(); 
        
        Product p1 = Product.builder()
                .name("펩시 콜라")
                .price(1500)
                .stock(10)
                .imageUrl("/images/pepsi.jpg")
                .build();
        productRepository.save(p1);

        Product p2 = Product.builder()
                .name("레쓰비 마일드 커피")
                .price(1200)
                .stock(10)
                .imageUrl("/images/letsbe.jpg")
                .build();
        productRepository.save(p2);
        
        System.out.println("====== 상품 데이터 동기화 완료 ======");

        // 테스트용 회원 데이터 초기화 (아이디/비번 찾기 테스트용)
        if (memberRepository.findByUsername("testuser").isEmpty()) {
            Member testMember = new Member();
            testMember.setUsername("testuser");
            testMember.setPassword(passwordEncoder.encode("password123"));
            testMember.setName("테스트");
            testMember.setPhoneNumber("010-1234-5678");
            testMember.setPoints(1000);
            memberRepository.save(testMember);
            System.out.println("====== 테스트용 회원 데이터(testuser) 생성 완료 ======");
        }
    }
}