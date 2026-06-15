package com.example.vendingmachine.controller;

import com.example.vendingmachine.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    private final ProductRepository productRepository;

    public ViewController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. 기본 홈 화면 (/)
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "index";
    }

    // 로그인 페이지는 MemberController에서 담당하므로 여기서는 삭제했습니다.
    // 회원가입 페이지는 MemberController에서 담당하므로 여기서는 삭제했습니다.
}