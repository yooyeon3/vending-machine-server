package com.example.vendingmachine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // 사용자가 http://localhost:8080/login 으로 접속하면 실행됩니다.
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates 폴더 안의 login.html 파일을 찾아서 화면에 띄워줍니다.
    }
}