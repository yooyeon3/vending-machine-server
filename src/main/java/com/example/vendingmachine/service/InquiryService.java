package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    // 문의 저장
    public void save(Inquiry inquiry) {
        inquiryRepository.save(inquiry);
    }

    // 전체 문의 조회 (관리자용)
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
}