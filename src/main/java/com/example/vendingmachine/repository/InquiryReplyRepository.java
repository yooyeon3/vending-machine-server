package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.domain.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {

    // 특정 문의의 답글 조회
    List<InquiryReply> findByInquiryOrderByCreatedAtAsc(Inquiry inquiry);

    // 안읽은 답글 개수 (사용자용)
    long countByInquiry_UsernameAndAdminReplyTrueAndReadFalse(String username);

}