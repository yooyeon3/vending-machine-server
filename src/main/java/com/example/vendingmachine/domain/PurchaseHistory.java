package com.example.vendingmachine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_history")
@Getter @Setter
public class PurchaseHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName; // 구매한 상품명
    private Integer paidPrice;  // 실제 결제 금액
    private Integer earnedPoints; // 이번 구매로 적립된 포인트
    private String buyerName;   // 구매자 이름
    private String phoneNumber; // 구매자 전화번호

    private String pinCode;     // 발급된 PIN 번호
    private LocalDateTime expiryDate; // PIN 만료 일시
    private boolean isUsed = false;   // 사용 여부

    private LocalDateTime purchaseTime; // 구매한 시간

    // DB에 저장되기 직전에 현재 시간을 자동으로 기록해줍니다.
    @PrePersist
    public void prePersist() {
        this.purchaseTime = LocalDateTime.now();
    }
}