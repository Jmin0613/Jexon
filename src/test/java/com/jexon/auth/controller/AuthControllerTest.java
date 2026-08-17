package com.jexon.auth.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.AuthService;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void authenticatedUserCanGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(authentication(authenticationFor(MemberRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.loginId").value("user1"))
                .andExpect(jsonPath("$.nickname").value("사용자"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void authenticatedAdminCanGetAdminRole() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(authentication(authenticationFor(MemberRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void unauthenticatedUserCannotGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(MemberRole role) {
        Member member = Member.createMember(
                "user1",
                "encoded-password",
                "사용자",
                "user@example.com",
                "사용자",
                "01012345678"
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member, "role", role);
        CustomUserDetails principal = new CustomUserDetails(member);

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.copyOf(principal.getAuthorities())
        );
    }
}
