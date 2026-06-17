package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 1. 로그인 화면 보여주기
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 2. 회원가입 화면 보여주기
    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("member", new Member());
        return "signup";
    }

    // 3. 회원가입 처리하기
    @PostMapping("/signup")
    public String signup(@ModelAttribute Member member, Model model) {
        System.out.println("=== 회원가입 컨트롤러 요청 도착 ===");
        System.out.println("아이디: " + member.getUsername());

        try {
            memberService.join(member);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("member", member);
            return "signup";
        }

        System.out.println("=== 서비스 로직 실행 후 리다이렉트 ===");
        return "redirect:/login";
    }

    // 4. 아이디 찾기 화면
    @GetMapping("/find-id")
    public String findIdPage() {
        return "find-id";
    }

    // 5. 아이디 찾기 처리
    @PostMapping("/find-id")
    public String findId(String name, String phoneNumber, Model model) {
        System.out.println(">>> [아이디 찾기] 이름: " + name + ", 전화번호: " + phoneNumber);
        String username = memberService.findUsername(name, phoneNumber);
        
        if (username == null) {
            System.out.println(">>> [아이디 찾기] 결과 없음");
            model.addAttribute("error", "일치하는 회원 정보가 없습니다. 이름과 전화번호를 다시 확인해주세요.");
            model.addAttribute("typedName", name);
            model.addAttribute("typedPhone", phoneNumber);
            return "find-id";
        }
        
        System.out.println(">>> [아이디 찾기] 성공: " + username);
        model.addAttribute("username", username);
        return "find-id";
    }

    // 6. 비밀번호 찾기(재설정) 화면
    @GetMapping("/find-pw")
    public String findPwPage() {
        return "find-pw";
    }

    // 7. 비밀번호 찾기 (1단계: 본인 확인)
    @PostMapping("/find-pw/verify")
    public String verifyForPw(String username, String phoneNumber, Model model) {
        System.out.println(">>> [비밀번호 찾기-인증] 아이디: " + username + ", 전화번호: " + phoneNumber);
        
        // 부작용 없는 전용 검증 메서드 사용
        boolean isVerified = memberService.verifyMember(username, phoneNumber);
        
        if (!isVerified) {
            model.addAttribute("error", "일치하는 회원 정보가 없습니다. 아이디와 전화번호를 다시 확인해주세요.");
            return "find-pw";
        }
        
        // 인증 성공 시, 다음 단계(비밀번호 재설정)로 이동하기 위한 정보 전달
        model.addAttribute("verifiedUsername", username);
        model.addAttribute("verifiedPhone", phoneNumber);
        model.addAttribute("step", 2);
        return "find-pw";
    }

    // 8. 비밀번호 찾기 (2단계: 새 비밀번호 설정)
    @PostMapping("/find-pw/reset")
    public String resetPw(String username, String phoneNumber, String newPassword, Model model) {
        System.out.println(">>> [비밀번호 찾기-재설정] 아이디: " + username);
        boolean success = memberService.resetPassword(username, phoneNumber, newPassword);
        
        if (!success) {
            model.addAttribute("error", "비밀번호 재설정에 실패했습니다. 다시 시도해주세요.");
            return "find-pw";
        }
        
        return "redirect:/login?resetSuccess=true";
    }

    // 9. 마이페이지 (메인)
    @GetMapping("/mypage")
    public String myPage(HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        // 최신 회원 정보 동기화
        Member member = memberService.findById(loginMember.getId());
        session.setAttribute("loginMember", member);

        // 등급 정보 조회
        MemberService.GradeProgress progress = memberService.getGradeProgress(member);
        
        model.addAttribute("member", member);
        model.addAttribute("progress", progress);
        
        return "mypage";
    }

    // 10. 프로필 수정 처리
    @PostMapping("/mypage/update")
    public String updateProfile(@RequestParam String name, @RequestParam String phoneNumber, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        memberService.updateProfile(loginMember.getId(), name, phoneNumber);
        return "redirect:/mypage?updateSuccess=true";
    }

    // 11. 비밀번호 변경 처리
    @PostMapping("/mypage/password")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String newPassword, 
                                 HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        boolean success = memberService.changePassword(loginMember.getId(), oldPassword, newPassword);
        if (!success) {
            return "redirect:/mypage?pwError=true";
        }
        return "redirect:/mypage?pwSuccess=true";
    }

    // 12. 회원 탈퇴 처리
    @PostMapping("/mypage/withdraw")
    public String withdraw(HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember != null) {
            memberService.withdraw(loginMember.getId());
            session.invalidate(); // 세션 만료
        }
        return "redirect:/?withdraw=true";
    }
}