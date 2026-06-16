package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {
    long countByBuyerName(String buyerName);
    // 기본적으로 제공되는 저장(save), 조회(findAll) 기능을 사용합니다.
}
