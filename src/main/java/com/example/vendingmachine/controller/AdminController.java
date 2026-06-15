package com.example.vendingmachine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    // 관리자 페이지 (http://localhost:8080/admin 으로 접속 시 실행)
    // ADMIN 권한이 있어야만 접근 가능 (SecurityConfig에서 설정)
    @GetMapping("/admin")
    public String adminPage() {
        return "admin"; // templates/admin.html 파일을 찾아 화면에 띄워줍니다.
    }
}