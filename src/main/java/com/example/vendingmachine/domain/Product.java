package com.example.vendingmachine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int price;
    private int stock;
    private String imageUrl; // 💡 사진 경로를 저장할 변수 추가!

    @Builder
    public Product(String name, int price, int stock, String imageUrl) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
    }

    // 재고 차감 로직 (일반 유저 구매용)
    public void decreaseStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stock--;
    }

    // 💡 관리자용: 재고 수량을 수정하는 메서드
    public void setStock(int stock) {
        this.stock = stock;
    }
}