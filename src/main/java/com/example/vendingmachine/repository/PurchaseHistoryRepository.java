package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {
    long countByBuyerName(String buyerName);
    List<PurchaseHistory> findByBuyerName(String buyerName);
    List<PurchaseHistory> findByBuyerNameAndPurchaseTimeAfterOrderByPurchaseTimeDesc(String buyerName, LocalDateTime since);

    @Query("SELECT p.productName, COUNT(p) as count FROM PurchaseHistory p WHERE p.buyerName = :buyerName GROUP BY p.productName ORDER BY count DESC")
    List<Object[]> findTopProductsByBuyerName(@Param("buyerName") String buyerName, Pageable pageable);
}
