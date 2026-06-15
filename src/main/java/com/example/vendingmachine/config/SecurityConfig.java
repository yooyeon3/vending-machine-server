package com.example.vendingmachine.config;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MemberRepository memberRepository) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        // 💡 누구나 접근 가능한 페이지에 회원가입(/signup) 추가!
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // 💡 로그인 성공 시 관리자와 일반 유저를 나누어 처리
                        .successHandler((request, response, authentication) -> {
                            if (authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                                response.sendRedirect("/admin");
                            } else {
                                // 일반 유저는 DB에서 정보를 찾아 세션에 저장 (기존 index.html 연동)
                                String username = authentication.getName();
                                Member member = memberRepository.findByUsername(username).orElse(null);
                                request.getSession().setAttribute("loginMember", member);
                                response.sendRedirect("/");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        // 💡 html의 <a> 태그(GET 방식)로 로그아웃이 가능하도록 설정
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .permitAll()
                );
        return http.build();
    }

    // 💡 경비원이 유저를 찾는 방법 (DB 연동)
    @Bean
    public UserDetailsService userDetailsService(MemberRepository memberRepository) {
        return username -> {
            // 1. 관리자 계정은 하드코딩으로 바로 통과
            if ("admin".equals(username)) {
                return User.builder()
                        .username("admin")
                        .password(passwordEncoder().encode("admin"))
                        .roles("ADMIN")
                        .build();
            }

            // 2. 일반 회원은 DB(MemberRepository)에서 찾아보고 검증
            Member member = memberRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

            return User.builder()
                    .username(member.getUsername())
                    .password(member.getPassword())
                    .roles("USER")
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}