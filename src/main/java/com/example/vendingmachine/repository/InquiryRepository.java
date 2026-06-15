package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 전체 문의 최신순 조회
    List<Inquiry> findAllByOrderByCreatedAtDesc();

    // 특정 사용자 문의 조회
    List<Inquiry> findByUsernameOrderByCreatedAtDesc(String username);
}