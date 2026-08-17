package com.jexon.member.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.dto.response.AdminMemberStatusUpdateResponse;
import com.jexon.member.service.AdminMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMemberController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminMemberControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AdminMemberService service;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void listRequiresAdminAndSupportsStatusFilter() throws Exception {
        mockMvc.perform(get("/api/admin/members")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/members").with(member("USER"))).andExpect(status().isForbidden());
        given(service.getMembers(eq(1L), eq(MemberStatus.ACTIVE), any())).willReturn(Page.empty());
        mockMvc.perform(get("/api/admin/members?status=ACTIVE").with(member("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(service).getMembers(eq(1L), eq(MemberStatus.ACTIVE), any());
    }

    @Test
    void updateRequiresAdminAndReturnsChangedStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/members/2/status").contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/members/2/status").with(member("USER")).contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden());
        Member target = Member.createMember("user", "encoded", "nickname", "user@example.com", "회원", "01000000000");
        ReflectionTestUtils.setField(target, "id", 2L); ReflectionTestUtils.setField(target, "status", MemberStatus.SUSPENDED); ReflectionTestUtils.setField(target, "role", MemberRole.USER);
        given(service.updateStatus(1L, 2L, MemberStatus.SUSPENDED)).willReturn(AdminMemberStatusUpdateResponse.from(target));
        mockMvc.perform(patch("/api/admin/members/2/status").with(member("ADMIN")).contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.memberId").value(2)).andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    private static RequestPostProcessor member(String role) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(principal.getMemberId()).willReturn(1L);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
