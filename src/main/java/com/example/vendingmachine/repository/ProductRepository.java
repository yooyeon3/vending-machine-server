package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository<관리할 엔티티 타입, 엔티티의 ID(PK) 타입>
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 끝입니다! 코드는 비어있지만, JpaRepository를 상속받는 것만으로도
    // 상품 저장(save), 전체 조회(findAll), ID로 조회(findById), 삭제(delete) 등의
    // 수많은 데이터베이스 기능이 자동으로 만들어집니다.
}