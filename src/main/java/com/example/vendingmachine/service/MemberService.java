package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.vendingmachine.repository.PurchaseHistoryRepository purchaseHistoryRepository;
    private final com.example.vendingmachine.repository.InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public GradeProgress getGradeProgress(Member member) {
        int score = getActivityScore(member);
        String currentGrade = getGrade(member);
        String nextGrade;
        int nextThreshold;
        int currentThreshold;

        if (score >= 100) {
            nextGrade = "MAX";
            nextThreshold = 100;
            currentThreshold = 100;
        } else if (score >= 50) {
            nextGrade = "DIAMOND";
            nextThreshold = 100;
            currentThreshold = 50;
        } else if (score >= 20) {
            nextGrade = "GOLD";
            nextThreshold = 50;
            currentThreshold = 20;
        } else {
            nextGrade = "SILVER";
            nextThreshold = 20;
            currentThreshold = 0;
        }

        int pointsToNext = Math.max(0, nextThreshold - score);
        double progress = currentGrade.equals("DIAMOND") ? 100.0 : 
                         ((double)(score - currentThreshold) / (nextThreshold - currentThreshold)) * 100;

        return new GradeProgress(score, currentGrade, nextGrade, pointsToNext, (int)progress);
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class GradeProgress {
        private int score;
        private String currentGrade;
        private String nextGrade;
        private int pointsToNext;
        private int progressPercentage;
    }

    public int getActivityScore(Member member) {
        if (member == null) return 0;
        String buyerName = member.getName() != null ? member.getName() : "";
        long purchaseCount = purchaseHistoryRepository.countByBuyerName(buyerName);
        long inquiryCount = inquiryRepository.countByUsername(member.getUsername());
        return (int) (purchaseCount * 10 + inquiryCount * 5);
    }

    public String getGrade(Member member) {
        if (member == null) return "BRONZE";
        int score = getActivityScore(member);
        if (score >= 100) return "DIAMOND";
        if (score >= 50) return "GOLD";
        if (score >= 20) return "SILVER";
        return "BRONZE";
    }

    // 등급별 할인율 (DIAMOND 20%, GOLD 10%, SILVER 5%)
    public double getDiscountRate(String grade) {
        if (grade == null) return 0.0;
        return switch (grade) {
            case "DIAMOND" -> 0.20;
            case "GOLD" -> 0.10;
            case "SILVER" -> 0.05;
            default -> 0.0;
        };
    }

    // 등급별 포인트 적립률 (DIAMOND 10%, GOLD 5%, 나머지 1%)
    public double getPointRate(String grade) {
        if (grade == null) return 0.01;
        return switch (grade) {
            case "DIAMOND" -> 0.10;
            case "GOLD" -> 0.05;
            default -> 0.01;
        };
    }

    public void addPoints(Member member, int points) {
        if (member == null) return;
        int currentPoints = member.getPoints() != null ? member.getPoints() : 0;
        member.setPoints(currentPoints + points);
        memberRepository.save(member);
    }

    public void join(Member member) {
        // [로그 추가] 가입 시도 확인
        System.out.println("가입 시도 확인! 이름: " + member.getUsername());

        // 1. 아이디 중복 체크
        memberRepository.findByUsername(member.getUsername()).ifPresent(m -> {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        });

        // 2. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);

        memberRepository.save(member);

        // [로그 추가] DB 저장 완료
        System.out.println("DB 저장 완료!");
    }

    public Member login(String username, String password) {
        // 1. DB에서 아이디로 회원 찾아보기
        Member member = memberRepository.findByUsername(username)
                .orElse(null); // 회원이 없으면 null 반환

        // 2. 회원이 없거나, 비밀번호가 일치하지 않으면 null 반환
        if (member == null || !passwordEncoder.matches(password, member.getPassword())) {
            return null; // 로그인 실패
        }

        // 3. 모두 통과하면 회원 정보 반환 (로그인 성공)
        return member;
    }

    // [관리자 전용] 모든 회원 목록 조회
    public java.util.List<Member> findAll() {
        return memberRepository.findAll();
    }

    // [관리자 전용] 회원 정보 수정 (메모 포함)
    public void updateMember(Long id, String name, String phoneNumber, String adminMemo) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setName(name);
        member.setPhoneNumber(phoneNumber);
        member.setAdminMemo(adminMemo);
        memberRepository.save(member);
    }

    // [관리자 전용] 회원 삭제 (강제 탈퇴)
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    // [관리자 전용] ID로 회원 찾기
    public Member findById(Long id) {
        return memberRepository.findById(id).orElse(null);
    }
}