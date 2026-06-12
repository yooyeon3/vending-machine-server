package com.example.vendingmachine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        // 누구나 접근 가능한 페이지
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        // /admin 으로 시작하는 페이지는 ADMIN 권한만 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 나머지는 로그인한 사람만 접근 가능
                        // 나머지는 로그인한 사람만 접근 가능
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // 로그인 페이지 경로
                        .loginPage("/login")
                        // 로그인 성공 시 역할에 따라 이동 (admin → /admin, 일반유저 → /)
                        .successHandler((request, response, authentication) -> {
                            if (authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                                response.sendRedirect("/admin");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        // 로그아웃 후 홈으로 이동
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // admin 계정 생성 (아이디: admin, 비밀번호: admin, 권한: ADMIN)
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호 암호화 (BCrypt 방식)
        return new BCryptPasswordEncoder();
    }
}