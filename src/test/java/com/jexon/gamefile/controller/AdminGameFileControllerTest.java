package com.jexon.gamefile.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.dto.response.GameFileUploadResponse;
import com.jexon.gamefile.service.GameFileUploadService;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminGameFileController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class AdminGameFileControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean GameFileUploadService service;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void uploadsMultipartFileAsAdminAndReturnsCreated() throws Exception {
        MockMultipartFile file = zipFile();
        given(service.upload(eq(1L), eq(15L), same(file)))
                .willReturn(GameFileUploadResponse.from(gameFile()));

        mockMvc.perform(multipart("/api/admin/game-versions/15/file")
                        .file(file)
                        .with(member("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.gameFileId").value(20L))
                .andExpect(jsonPath("$.gameVersionId").value(15L))
                .andExpect(jsonPath("$.originalFileName").value("game.zip"))
                .andExpect(jsonPath("$.extension").value("zip"))
                .andExpect(jsonPath("$.fileSize").value(4L))
                .andExpect(jsonPath("$.checksum").value("a".repeat(64)))
                .andExpect(jsonPath("$.storageKey").doesNotExist());

        verify(service).upload(eq(1L), eq(15L), same(file));
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/game-versions/15/file").file(zipFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUserRole() throws Exception {
        mockMvc.perform(multipart("/api/admin/game-versions/15/file")
                        .file(zipFile())
                        .with(member("USER")))
                .andExpect(status().isForbidden());
    }

    private static MockMultipartFile zipFile() {
        return new MockMultipartFile(
                "file",
                "game.zip",
                "application/zip",
                new byte[]{0x50, 0x4B, 0x03, 0x04}
        );
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

    private static GameFile gameFile() {
        GameVersion gameVersion = GameVersion.createGameVersion(
                "v1.0.0",
                "Jexon 정식 출시 버전",
                "Jexon 게임 클라이언트 정식 출시 버전입니다."
        );
        ReflectionTestUtils.setField(gameVersion, "id", 15L);
        GameFile gameFile = GameFile.createGameFile(
                gameVersion,
                "game.zip",
                "game-files/15/id.zip",
                "zip",
                "application/zip",
                4L,
                "a".repeat(64)
        );
        ReflectionTestUtils.setField(gameFile, "id", 20L);
        return gameFile;
    }
}
