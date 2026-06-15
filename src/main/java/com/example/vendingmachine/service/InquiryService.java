package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.domain.InquiryReply;
import com.example.vendingmachine.repository.InquiryRepository;
import com.example.vendingmachine.repository.InquiryReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;

    // 문의 저장
    public void save(Inquiry inquiry) {
        inquiryRepository.save(inquiry);
    }

    // 전체 문의 조회 (관리자 + 전체 목록용)
    public List<Inquiry> findAll() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    // 내 문의 조회 (사용자용)
    public List<Inquiry> findByUsername(String username) {
        return inquiryRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    // 문의 개수 (관리자 알림용)
    public long count() {
        return inquiryRepository.count();
    }

    // 문의 단건 조회
    public Inquiry findById(Long id) {
        return inquiryRepository.findById(id).orElseThrow();
    }

    // 답글 저장
    public void saveReply(InquiryReply reply) {
        inquiryReplyRepository.save(reply);
    }

    // 특정 문의 답글 조회
    public List<InquiryReply> findReplies(Inquiry inquiry) {
        return inquiryReplyRepository.findByInquiryOrderByCreatedAtAsc(inquiry);
    }
}