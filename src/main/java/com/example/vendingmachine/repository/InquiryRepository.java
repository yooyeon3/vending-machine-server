package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    long countByUsername(String username);

    // 전체 문의 최신순 조회
    List<Inquiry> findAllByOrderByCreatedAtDesc();

    // 특정 사용자 문의 조회
    List<Inquiry> findByUsernameOrderByCreatedAtDesc(String username);

    // 관리자가 아직 답글 안 단 문의 개수
    long countByRepliesEmpty();

    // 답글 없는 문의 목록 (관리자 빨간 점용)
    List<Inquiry> findByRepliesEmpty();
}
