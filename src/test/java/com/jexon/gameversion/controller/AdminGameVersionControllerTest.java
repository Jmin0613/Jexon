package com.jexon.gameversion.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.gameversion.domain.*;
import com.jexon.gameversion.dto.request.*;
import com.jexon.gameversion.dto.response.*;
import com.jexon.gameversion.exception.*;
import com.jexon.gameversion.service.GameVersionService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminGameVersionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminGameVersionControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean GameVersionService service;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void createByAdmin() throws Exception {
        given(service.create(eq(1L), any(GameVersionCreateRequest.class))).willReturn(GameVersionCreateResponse.from(entity()));
        mockMvc.perform(post("/api/admin/game-versions").with(member("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(validCreate()))
                .andExpect(status().isCreated()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.gameVersionId").value(10)).andExpect(jsonPath("$.version").value("v1.0.0"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        verify(service).create(eq(1L), any(GameVersionCreateRequest.class));
    }

    @Test
    void listWithDefaultsAndFilter() throws Exception {
        given(service.getAdminGameVersions(eq(1L), any(), any(Pageable.class))).willReturn(Page.empty());
        mockMvc.perform(get("/api/admin/game-versions").with(member("ADMIN"))).andExpect(status().isOk());
        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAdminGameVersions(eq(1L), eq(null), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        mockMvc.perform(get("/api/admin/game-versions?status=DRAFT&page=2&size=30").with(member("ADMIN"))).andExpect(status().isOk());
        verify(service).getAdminGameVersions(eq(1L), eq(GameVersionStatus.DRAFT), argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 30));
    }

    @Test
    void detailDoesNotExposeLockVersion() throws Exception {
        given(service.getAdminGameVersion(1L, 10L)).willReturn(GameVersionDetailResponse.from(entity()));
        mockMvc.perform(get("/api/admin/game-versions/10").with(member("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameVersionId").value(10)).andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.lockVersion").doesNotExist());
    }

    @Test
    void partialUpdates() throws Exception {
        given(service.update(eq(1L), eq(10L), any(GameVersionUpdateRequest.class))).willReturn(GameVersionDetailResponse.from(entity()));
        mockMvc.perform(put("/api/admin/game-versions/10").with(member("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"수정된 게임 버전 제목\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/game-versions/10").with(member("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"수정된 게임 버전 상세 설명입니다.\"}")).andExpect(status().isOk());
    }

    @Test
    void releaseWithoutBody() throws Exception {
        GameVersion released = entity(); released.release(LocalDateTime.of(2026, 8, 8, 13, 0));
        given(service.release(1L, 10L)).willReturn(GameVersionReleaseResponse.from(released));
        mockMvc.perform(post("/api/admin/game-versions/10/release").with(member("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RELEASED"));
        verify(service).release(1L, 10L);
    }

    @Test
    void rejectInvalidCreateRequests() throws Exception {
        String[] requests = {"{}", "{\"version\":\"1.0.0\",\"title\":\"Jexon 정식 출시 버전\",\"description\":\"Jexon 게임 정식 출시 설명입니다.\"}",
                createJson("v1.0.0", "123456789", "Jexon 게임 정식 출시 설명입니다."), createJson("v1.0.0", "a".repeat(101), "Jexon 게임 정식 출시 설명입니다."),
                createJson("v1.0.0", "Jexon 정식 출시 버전", "123456789"), createJson("v1.0.0", "Jexon 정식 출시 버전", "a".repeat(501))};
        for (String request : requests) mockMvc.perform(post("/api/admin/game-versions").with(member("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
    }

    @Test
    void rejectInvalidUpdateRequests() throws Exception {
        for (String request : new String[]{"{}", "{\"title\":\"   \"}", "{\"title\":\"123456789\"}", "{\"description\":\"   \"}", "{\"description\":\"123456789\"}"})
            mockMvc.perform(put("/api/admin/game-versions/10").with(member("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
    }

    @Test
    void securityProtectsRepresentativeEndpoints() throws Exception {
        mockMvc.perform(post("/api/admin/game-versions").contentType(MediaType.APPLICATION_JSON).content(validCreate())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/game-versions").with(member("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/game-versions/10").with(member("USER")).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정된 게임 버전 제목\"}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/game-versions/10/release").with(member("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void convertsDomainExceptions() throws Exception {
        given(service.create(eq(1L), any())).willThrow(new DuplicateGameVersionException());
        mockMvc.perform(post("/api/admin/game-versions").with(member("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(validCreate()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        given(service.getAdminGameVersion(1L, 10L)).willThrow(new GameVersionNotFoundException());
        mockMvc.perform(get("/api/admin/game-versions/10").with(member("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        given(service.release(1L, 10L)).willThrow(new InvalidGameVersionStateException("상태 전이 불가"));
        mockMvc.perform(post("/api/admin/game-versions/10/release").with(member("ADMIN"))).andExpect(status().isConflict());
    }

    private static RequestPostProcessor member(String role) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class); given(principal.getMemberId()).willReturn(1L);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    private static String validCreate() { return createJson("v1.0.0", "Jexon 정식 출시 버전", "Jexon 게임 클라이언트 최초 정식 출시 버전입니다."); }
    private static String createJson(String v, String t, String d) { return "{\"version\":\"" + v + "\",\"title\":\"" + t + "\",\"description\":\"" + d + "\"}"; }
    private static GameVersion entity() { GameVersion e = GameVersion.createGameVersion("v1.0.0", "Jexon 정식 출시 버전", "Jexon 게임 클라이언트 최초 정식 출시 버전입니다."); ReflectionTestUtils.setField(e, "id", 10L); ReflectionTestUtils.setField(e, "createdAt", LocalDateTime.of(2026,8,8,12,0)); ReflectionTestUtils.setField(e, "updatedAt", LocalDateTime.of(2026,8,8,12,0)); return e; }
}
