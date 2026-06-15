package com.example.vendingmachine.config;

import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // 스프링이 서버를 켤 때 이 클래스를 자동으로 인식하여 실행하도록 합니다.
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    // ProductRepository를 주입받습니다.
    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 데이터베이스에 등록된 상품이 하나도 없을 때만 초기화를 진행합니다.
        // 순서대로 저장하면 자동으로 ID가 1, 2로 부여됩니다.
        if (productRepository.count() == 0) {

            // 1. 펩시 콜라 (자동으로 ID 1 부여)
            Product pepsi = Product.builder()
                    .name("펩시 콜라")
                    .price(1500)
                    .stock(10)
                    .build();
            productRepository.save(pepsi);
            System.out.println("====== [1번 상품] 펩시 콜라 세팅 완료 ======");

            // 2. 레쓰비 마일드 커피 (자동으로 ID 2 부여)
            Product letsbe = Product.builder()
                    .name("레쓰비 마일드 커피")
                    .price(1200)
                    .stock(10)
                    .build();
            productRepository.save(letsbe);
            System.out.println("====== [2번 상품] 레쓰비 마일드 커피 세팅 완료 ======");
        }
    }
}