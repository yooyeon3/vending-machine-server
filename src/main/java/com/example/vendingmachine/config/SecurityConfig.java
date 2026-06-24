package com.example.vendingmachine.config;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectProvider<MemberRepository> memberRepositoryProvider;

    public SecurityConfig(ObjectProvider<MemberRepository> memberRepositoryProvider) {
        this.memberRepositoryProvider = memberRepositoryProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/signup", "/find-id/**", "/find-pw/**", "/css/**", "/js/**", "/images/**", "/inquiry/**", "/ws/**").permitAll()
                        .requestMatchers("/api/orders/pending", "/api/orders/*/status", "/api/pin/verify", "/api/products").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            MemberRepository memberRepository = memberRepositoryProvider.getObject();
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                response.sendRedirect("/admin");
            } else {
                String username = authentication.getName();
                Member member = memberRepository.findByUsername(username).orElse(null);
                request.getSession().setAttribute("loginMember", member);
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            MemberRepository memberRepository = memberRepositoryProvider.getObject();
            if ("admin".equals(username)) {
                return User.builder()
                        .username("admin")
                        .password("admin") // :white_check_mark: 평문으로 수정
                        .roles("ADMIN")
                        .build();
            }

            Member member = memberRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

            return User.builder()
                    .username(member.getUsername())
                    .password(member.getPassword())
                    .roles("USER")
                    .build();
        };
    }

    // :white_check_mark: 평문 비교 허용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}