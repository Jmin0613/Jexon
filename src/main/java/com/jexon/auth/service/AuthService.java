package com.jexon.auth.service;

import com.jexon.auth.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    // uthenticationManager를 호출

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse){
        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getLoginId(),
                        request.getPassword()
                );

        // 인증 결과 받기
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // SecurityContext 생성하여 인증 사용자 정보 넣기
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 다음 요청에서도 유지되도록 저장
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }
}
