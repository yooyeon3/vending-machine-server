package com.example.vendingmachine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // 기본 홈화면 (http://localhost:8080/ 로 접속 시 실행)
    @GetMapping("/")
    public String home() {
        return "index"; // templates/index.html 파일을 찾아 화면에 띄워줍니다.
    }

    // 로그인 화면 (http://localhost:8080/login 으로 접속 시 실행)
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html 파일을 찾아 화면에 띄워줍니다.
    }
}