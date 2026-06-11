package com.example.vendingmachine.config;

import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // 스프링이 서버를 켤 때 이 클래스를 자동으로 인식하여 실행하도록 합니다.
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    // 방금 만든 ProductRepository를 주입받습니다.
    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 기존에 데이터가 없을 때만 초기 데이터를 넣도록 방어 코드를 작성합니다.
        if (productRepository.count() == 0) {

            // 1. 콜라 추가
            Product cola = Product.builder()
                    .name("콜라")
                    .price(1500)
                    .stock(10)
                    .build();

            // 2. 사이다 추가
            Product cider = Product.builder()
                    .name("사이다")
                    .price(1200)
                    .stock(8)
                    .build();

            // 3. 생수 추가
            Product water = Product.builder()
                    .name("생수")
                    .price(800)
                    .stock(15)
                    .build();

            // 데이터베이스에 저장하기
            productRepository.save(cola);
            productRepository.save(cider);
            productRepository.save(water);

            System.out.println("====== 자판기 초기 상품 데이터 세팅 완료! ======");
        }
    }
}