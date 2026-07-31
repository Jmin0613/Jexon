package com.jexon.auth.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.AuthService;
import com.jexon.member.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    // 로그인 상태 확인
    @GetMapping("/me")
    public ResponseEntity<Long> me(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(userDetails.getMemberId());
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse){
        authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok().build();
    }

    // 로그아웃

}
