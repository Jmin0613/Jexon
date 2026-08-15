package com.jexon.downloadhistory.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.downloadhistory.dto.response.DownloadSummaryResponse;
import com.jexon.downloadhistory.service.DownloadStatisticsService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDownloadStatisticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminDownloadStatisticsControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean DownloadStatisticsService service;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void summarySecurityAndResponse() throws Exception {
        mockMvc.perform(get("/api/admin/download-statistics/summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/download-statistics/summary").with(member("USER")))
                .andExpect(status().isForbidden());

        given(service.getSummary(1L)).willReturn(DownloadSummaryResponse.of(1234L));
        mockMvc.perform(get("/api/admin/download-statistics/summary").with(member("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDownloads").value(1234));
        verify(service).getSummary(1L);
    }

    @Test
    void versionsSecurityAndEmptyResponse() throws Exception {
        mockMvc.perform(get("/api/admin/download-statistics/versions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/download-statistics/versions").with(member("USER")))
                .andExpect(status().isForbidden());
        given(service.getVersionStatistics(1L)).willReturn(List.of());
        mockMvc.perform(get("/api/admin/download-statistics/versions").with(member("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dailySecurityAndEmptyResponse() throws Exception {
        mockMvc.perform(get("/api/admin/download-statistics/daily"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/download-statistics/daily").with(member("USER")))
                .andExpect(status().isForbidden());
        given(service.getDailyStatistics(1L)).willReturn(List.of());
        mockMvc.perform(get("/api/admin/download-statistics/daily").with(member("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private static RequestPostProcessor member(String role) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(principal.getMemberId()).willReturn(1L);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));
    }
}
