package com.jexon.gameversion.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.dto.response.GameFileDownloadResponse;
import com.jexon.gamefile.service.GameFileDownloadService;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.dto.response.LatestGameVersionResponse;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.service.GameVersionService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameVersionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class GameVersionControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean GameVersionService service;
    @MockitoBean GameFileDownloadService downloadService;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void anonymousCanGetLatestGameVersion() throws Exception {
        given(service.getLatestGameVersion()).willReturn(response());

        mockMvc.perform(get("/api/game-versions/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameVersionId").value(10))
                .andExpect(jsonPath("$.version").value("v1.2.3"))
                .andExpect(jsonPath("$.title").value("Jexon 정식 출시 버전"))
                .andExpect(jsonPath("$.description").value("Jexon 게임 클라이언트 정식 출시 버전입니다."))
                .andExpect(jsonPath("$.releasedAt").value("2026-08-14T12:00:00"))
                .andExpect(jsonPath("$.gameFileId").value(20))
                .andExpect(jsonPath("$.originalFileName").value("jexon-v1.2.3.zip"))
                .andExpect(jsonPath("$.fileSize").value(1024))
                .andExpect(jsonPath("$.checksum").value("a".repeat(64)))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.contentType").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.lockVersion").doesNotExist());
        verify(service).getLatestGameVersion();
    }

    @Test
    void userCanGetLatestGameVersion() throws Exception {
        given(service.getLatestGameVersion()).willReturn(response());
        mockMvc.perform(get("/api/game-versions/latest").with(member("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanGetLatestGameVersion() throws Exception {
        given(service.getLatestGameVersion()).willReturn(response());
        mockMvc.perform(get("/api/game-versions/latest").with(member("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void returnNotFoundWhenLatestGameVersionDoesNotExist() throws Exception {
        given(service.getLatestGameVersion()).willThrow(new GameVersionNotFoundException());
        mockMvc.perform(get("/api/game-versions/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void adminApiRemainsProtected() throws Exception {
        mockMvc.perform(get("/api/admin/game-versions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/game-versions").with(member("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCanDownloadLatestGameVersion() throws Exception {
        byte[] content = "download content".getBytes(StandardCharsets.UTF_8);
        given(downloadService.downloadLatest()).willReturn(downloadResponse(content));

        mockMvc.perform(get("/api/game-versions/latest/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Length", "16"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("jexon-v1.2.3.zip")))
                .andExpect(content().bytes(content))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("game-files/10"))));
        verify(downloadService).downloadLatest();
    }

    @Test
    void userCanDownloadLatestGameVersion() throws Exception {
        given(downloadService.downloadLatest()).willReturn(downloadResponse(new byte[]{1}));
        mockMvc.perform(get("/api/game-versions/latest/download").with(member("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDownloadLatestGameVersion() throws Exception {
        given(downloadService.downloadLatest()).willReturn(downloadResponse(new byte[]{1}));
        mockMvc.perform(get("/api/game-versions/latest/download").with(member("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void downloadReturnsNotFoundWhenReleasedVersionDoesNotExist() throws Exception {
        given(downloadService.downloadLatest()).willThrow(new GameVersionNotFoundException());
        mockMvc.perform(get("/api/game-versions/latest/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private static RequestPostProcessor member(String role) {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));
    }

    private static LatestGameVersionResponse response() {
        GameVersion gameVersion = GameVersion.createGameVersion(
                "v1.2.3", "Jexon 정식 출시 버전", "Jexon 게임 클라이언트 정식 출시 버전입니다."
        );
        ReflectionTestUtils.setField(gameVersion, "id", 10L);
        gameVersion.release(LocalDateTime.of(2026, 8, 14, 12, 0));
        GameFile gameFile = GameFile.createGameFile(
                gameVersion, "jexon-v1.2.3.zip", "game-versions/10/jexon.zip", ".zip",
                "application/zip", 1024L, "a".repeat(64)
        );
        ReflectionTestUtils.setField(gameFile, "id", 20L);
        return LatestGameVersionResponse.of(gameVersion, gameFile);
    }

    private static GameFileDownloadResponse downloadResponse(byte[] content) {
        return new GameFileDownloadResponse(
                "jexon-v1.2.3.zip",
                (long) content.length,
                new ByteArrayInputStream(content)
        );
    }
}
