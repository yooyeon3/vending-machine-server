package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.domain.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {

    // 특정 문의의 답글 조회
    List<InquiryReply> findByInquiryOrderByCreatedAtAsc(Inquiry inquiry);

    // 안읽은 답글 개수 (사용자용 - 관리자가 남긴 답글 중 미읽음)
    long countByInquiry_UsernameAndAdminReplyTrueAndReadFalse(String username);

    // 안읽은 답글 개수 (관리자용 - 사용자가 남긴 답글 중 미읽음)
    long countByAdminReplyFalseAndReadFalse();

}