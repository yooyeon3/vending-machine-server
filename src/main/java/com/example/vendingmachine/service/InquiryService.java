package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.domain.InquiryReply;
import com.example.vendingmachine.repository.InquiryRepository;
import com.example.vendingmachine.repository.InquiryReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // 전체 문의 조회
    public List<Inquiry> findAll() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    // 내 문의 조회
    public List<Inquiry> findByUsername(String username) {
        return inquiryRepository.findByUsernameOrderByCreatedAtDesc(username);
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

    // 관리자용 - 답글 안 달린 문의 개수
    public long countUnanswered() {
        return inquiryRepository.countByRepliesEmpty();
    }

    // 사용자용 - 안읽은 관리자 답글 개수
    public long countUnreadReplies(String username) {
        return inquiryReplyRepository.countByInquiry_UsernameAndAdminReplyTrueAndIsReadFalse(username);
    }

    // 관리자용 - 사용자가 남긴 안읽은 답글 개수
    public long countUnreadUserReplies() {
        return inquiryReplyRepository.countByAdminReplyFalseAndIsReadFalse();
    }

    // 사용자용 - 안읽은 관리자 답글이 있는 문의 ID 집합 (빨간 점 표시용)
    public Set<Long> getUnreadInquiryIdsForUser(String username) {
        return inquiryReplyRepository.findByInquiry_UsernameAndAdminReplyTrueAndIsReadFalse(username)
                .stream()
                .map(r -> r.getInquiry().getId())
                .collect(Collectors.toSet());
    }

    // 관리자용 - 미응답 or 사용자 답글 미읽음 문의 ID 집합 (빨간 점 표시용)
    public Set<Long> getUnreadInquiryIdsForAdmin() {
        Set<Long> ids = new HashSet<>();
        inquiryRepository.findByRepliesEmpty().forEach(i -> ids.add(i.getId()));
        inquiryReplyRepository.findByAdminReplyFalseAndIsReadFalse().forEach(r -> ids.add(r.getInquiry().getId()));
        return ids;
    }

    // 답글 읽음 처리 (양방향: 관리자 읽으면 사용자 답글 읽음, 사용자 읽으면 관리자 답글 읽음)
    public void markRepliesAsRead(Inquiry inquiry, String username) {
        List<InquiryReply> replies = inquiryReplyRepository.findByInquiryOrderByCreatedAtAsc(inquiry);
        if (username.equals("admin")) {
            for (InquiryReply reply : replies) {
                if (!reply.isAdminReply() && !reply.isRead()) {
                    reply.setRead(true);
                }
            }
        } else {
            for (InquiryReply reply : replies) {
                if (reply.isAdminReply() && !reply.isRead()) {
                    reply.setRead(true);
                }
            }
        }
    }
}