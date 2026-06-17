package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Product;
import com.example.vendingmachine.repository.ProductRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController // 💡 @Controller와 달리 화면이 아닌 '데이터(JSON)'를 반환합니다.
@CrossOrigin(origins = "*") // 💡 minho님의 앱에서 접근할 수 있도록 허용(CORS 해결)
public class ApiController {

    private final ProductRepository productRepository;

    public ApiController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 앱에서 상품 목록을 요청할 때 응답하는 주소
    @GetMapping("/api/products")
    public List<Product> getProducts() {
        return productRepository.findAll(); // DB의 상품 목록을 JSON 형태로 앱에 전송
    }
}