package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.domain.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {

    // 특정 문의의 답글 조회
    List<InquiryReply> findByInquiryOrderByCreatedAtAsc(Inquiry inquiry);

    // 안읽은 답글 개수 (사용자용 - 관리자가 남긴 답글 중 미읽음)
    long countByInquiry_UsernameAndAdminReplyTrueAndIsReadFalse(String username);

    // 안읽은 답글 개수 (관리자용 - 사용자가 남긴 답글 중 미읽음)
    long countByAdminReplyFalseAndIsReadFalse();

    // 사용자별 안읽은 관리자 답글 목록 (문의 ID 추출용)
    List<InquiryReply> findByInquiry_UsernameAndAdminReplyTrueAndIsReadFalse(String username);

    // 관리자용 - 안읽은 사용자 답글 목록 (문의 ID 추출용)
    List<InquiryReply> findByAdminReplyFalseAndIsReadFalse();

}