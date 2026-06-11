package com.example.vendingmachine.domain; // 본인의 패키지명과 다르면 맨 윗줄은 기존 것을 유지하세요.

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // 이 클래스가 데이터베이스의 테이블 역할을 한다는 표시입니다.
@Getter // 모든 필드의 Getter 메서드를 자동으로 만들어줍니다. (Lombok)
@NoArgsConstructor // 기본 생성자를 자동으로 만들어줍니다. (JPA 필수)
public class Product {

    @Id // 이 필드가 테이블의 고유 키(PK)임을 나타냅니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 1, 2, 3... 번호를 DB가 알아서 자동으로 매깁니다.
    private Long id;

    private String name;  // 상품 이름 (예: 콜라, 사이다)
    private int price;    // 상품 가격
    private int stock;    // 남은 재고 수량

    // 상품을 처음 생성할 때 사용할 빌더 패턴
    @Builder
    public Product(String name, int price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // 나중에 음료수가 뽑혔을 때(주문 시) 재고를 1개 줄이는 핵심 로직
    public void decreaseStock() {
        if (this.stock > 0) {
            this.stock--;
        } else {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
    }
}