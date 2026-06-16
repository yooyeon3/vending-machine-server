package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {
    long countByBuyerName(String buyerName);
    List<PurchaseHistory> findByBuyerName(String buyerName);
}
