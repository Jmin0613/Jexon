package com.jexon.global.config;

import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 비로그인 접근 허용
                        .requestMatchers(
                                "/api/members/signup", "/api/auth/login"
                        ).permitAll()

                        // 게시글 및 게시글별 댓글 조회 허용
                        .requestMatchers(
                                HttpMethod.GET, "/api/posts", "/api/posts/**"
                        ).permitAll()

                        // 새소식 조회 허용
                        .requestMatchers(
                                HttpMethod.GET, "/api/news", "/api/news/**"
                        ).permitAll()

                        // 관리자 전용
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // 그 외 요청은 로그인 필요
                        .anyRequest().authenticated()
                )

                // 로그인 접근 처리 및 권한 부족
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)
                        )
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // 회원 정보 조회 → CustomUserDetailsService 사용
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);

        // 비밀번호 비교 → BCryptPasswordEncoder 사용
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // 로그인 요청을 담은 Authentication을 받아 인증하고, 성공하면 권한 정보가 포함된 인증 완료 객체를 반환
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        // 세션 저장용 Repository 등록
        return new HttpSessionSecurityContextRepository();
    }

}
