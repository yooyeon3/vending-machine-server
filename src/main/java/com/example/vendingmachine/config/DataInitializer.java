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

        // 1번 상품 (펩시 콜라) 처리
        Product pepsi = productRepository.findById(1L).orElse(null);
        if (pepsi == null) {
            pepsi = Product.builder()
                    .name("펩시 콜라")
                    .price(1500)
                    .stock(2)
                    .imageUrl("/images/pepsi.jpg")
                    .build();
        } else {
            // 이미 존재한다면 이름과 이미지만 강제로 업데이트 (콜라 -> 펩시 콜라)
            // Product 엔티티에 @Setter가 없으므로 필드 수정을 위해 Reflection이나 다른 방식을 써야할 수 있지만,
            // 현재 엔티티 구조상 Builder나 직접 필드 접근이 필요함. 
            // 여기서는 새 객체를 생성하거나 기존 객체의 이름을 바꿔서 저장함.
            pepsi = Product.builder()
                    .name("펩시 콜라")
                    .price(1500)
                    .stock(pepsi.getStock())
                    .imageUrl("/images/pepsi.jpg")
                    .build();
            // ID 유지를 위해 수동 설정이 필요할 수 있음 (JPA 특성상 동일 ID로 save하면 update됨)
        }
        // JPA에서 ID 1번으로 강제 저장하기 위해 엔티티 구조를 고려하여 처리
        // Product 클래스에 name 등을 수정할 메서드가 없으므로, Repository를 통해 확실히 처리하거나
        // 간단하게 기존 데이터를 삭제 후 재생성하는 방식을 택할 수도 있음.
        
        // 더 확실한 방법: 기존 1, 2번 데이터를 삭제하고 다시 생성 (데이터 정합성 통일)
        productRepository.deleteAll(); 
        
        Product p1 = Product.builder()
                .name("펩시 콜라")
                .price(1500)
                .stock(2)
                .imageUrl("/images/pepsi.jpg")
                .build();
        productRepository.save(p1);

        Product p2 = Product.builder()
                .name("레쓰비 마일드 커피")
                .price(1200)
                .stock(2)
                .imageUrl("/images/letsbe.jpg")
                .build();
        productRepository.save(p2);
        
        System.out.println("====== 상품 데이터 (펩시 콜라, 레쓰비 마일드 커피) 동기화 완료 ======");
    }
}