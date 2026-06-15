package com.example.vendingmachine.controller;

import com.example.vendingmachine.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    private final ProductRepository productRepository;

    // 생성자를 통해 ProductRepository(DB 접근 객체)를 주입받습니다.
    public ViewController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. 기본 홈 화면 (http://localhost:8080/ 으로 접속)
    @GetMapping("/")
    public String home(Model model) {
        // DB에서 모든 상품 정보를 가져와 화면(index.html)으로 전달합니다.
        model.addAttribute("products", productRepository.findAll());
        return "index"; // templates/index.html 파일을 찾아 화면에 띄워줍니다.
    }

    // 2. 로그인 화면 (http://localhost:8080/login 으로 접속)
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html 파일을 찾아 화면에 띄워줍니다.
    }

    // 3. 회원가입 화면 (http://localhost:8080/signup 으로 접속)
    @GetMapping("/signup")
    public String signupPage() {
        return "signup"; // templates/signup.html 파일을 찾아 화면에 띄워줍니다.
    }
}