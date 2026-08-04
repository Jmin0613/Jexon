package com.jexon.news.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import com.jexon.member.domain.Member;
import com.jexon.news.domain.News;
import com.jexon.news.domain.NewsType;
import com.jexon.news.dto.request.NewsCreateRequest;
import com.jexon.news.dto.request.NewsUpdateRequest;
import com.jexon.news.dto.response.NewsCreateResponse;
import com.jexon.news.dto.response.NewsDetailResponse;
import com.jexon.news.exception.NewsNotFoundException;
import com.jexon.news.exception.NewsPermissionDeniedException;
import com.jexon.news.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminNewsController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class AdminNewsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createNewsByAdmin() throws Exception {
        given(newsService.create(eq(1L), any(NewsCreateRequest.class)))
                .willReturn(NewsCreateResponse.of(10L));

        mockMvc.perform(post("/api/admin/news")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.newsId").value(10L));

        verify(newsService).create(eq(1L), any(NewsCreateRequest.class));
    }

    @Test
    void updateNewsByAdmin() throws Exception {
        given(newsService.update(eq(10L), eq(1L), any(NewsUpdateRequest.class)))
                .willReturn(NewsDetailResponse.from(news(10L)));

        mockMvc.perform(put("/api/admin/news/10")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(10L))
                .andExpect(jsonPath("$.type").value("NOTICE"))
                .andExpect(jsonPath("$.title").value("점검 안내"));

        verify(newsService).update(eq(10L), eq(1L), any(NewsUpdateRequest.class));
    }

    @Test
    void deleteNewsByAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/news/10")
                        .with(authenticatedMember(1L, "ADMIN")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(newsService).delete(10L, 1L);
    }

    @Test
    void rejectInvalidCreateRequests() throws Exception {
        assertInvalidPost("{\"title\":\"제목\",\"content\":\"내용\"}");
        assertInvalidPost("{\"type\":\"NOTICE\",\"title\":\"   \",\"content\":\"내용\"}");
        assertInvalidPost("{\"type\":\"NOTICE\",\"title\":\"" + "a".repeat(151) + "\",\"content\":\"내용\"}");
        assertInvalidPost("{\"type\":\"NOTICE\",\"title\":\"제목\"}");
        assertInvalidPost("{\"type\":\"NOTICE\",\"title\":\"제목\",\"content\":\"" + "a".repeat(10001) + "\"}");
    }

    @Test
    void rejectInvalidUpdateRequests() throws Exception {
        assertInvalidPut("{\"title\":\"제목\",\"content\":\"내용\"}");
        assertInvalidPut("{\"type\":\"NOTICE\",\"title\":\"   \",\"content\":\"내용\"}");
        assertInvalidPut("{\"type\":\"NOTICE\",\"title\":\"" + "a".repeat(151) + "\",\"content\":\"내용\"}");
        assertInvalidPut("{\"type\":\"NOTICE\",\"title\":\"제목\"}");
        assertInvalidPut("{\"type\":\"NOTICE\",\"title\":\"제목\",\"content\":\"" + "a".repeat(10001) + "\"}");
    }

    @Test
    void unauthenticatedWritesReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/news/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/news/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userWritesReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/news")
                        .with(authenticatedMember(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/news/10")
                        .with(authenticatedMember(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/news/10")
                        .with(authenticatedMember(1L, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void newsNotFoundIsConvertedToCommonErrorResponse() throws Exception {
        given(newsService.update(eq(10L), eq(1L), any(NewsUpdateRequest.class)))
                .willThrow(new NewsNotFoundException());

        mockMvc.perform(put("/api/admin/news/10")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("새소식을 찾을 수 없습니다."));
    }

    @Test
    void permissionDeniedMessagesAreConvertedToCommonErrorResponse() throws Exception {
        given(newsService.create(eq(1L), any(NewsCreateRequest.class)))
                .willThrow(new NewsPermissionDeniedException("새소식을 작성할 권한이 없습니다."));
        given(newsService.update(eq(10L), eq(1L), any(NewsUpdateRequest.class)))
                .willThrow(new NewsPermissionDeniedException("새소식을 수정할 권한이 없습니다."));
        org.mockito.Mockito.doThrow(new NewsPermissionDeniedException("새소식을 삭제할 권한이 없습니다."))
                .when(newsService).delete(10L, 1L);

        mockMvc.perform(post("/api/admin/news")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("새소식을 작성할 권한이 없습니다."));
        mockMvc.perform(put("/api/admin/news/10")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("새소식을 수정할 권한이 없습니다."));
        mockMvc.perform(delete("/api/admin/news/10")
                        .with(authenticatedMember(1L, "ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("새소식을 삭제할 권한이 없습니다."));
    }

    private void assertInvalidPost(String json) throws Exception {
        mockMvc.perform(post("/api/admin/news")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private void assertInvalidPut(String json) throws Exception {
        mockMvc.perform(put("/api/admin/news/10")
                        .with(authenticatedMember(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor authenticatedMember(Long memberId, String role) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(principal.getMemberId()).willReturn(memberId);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return authentication(authentication);
    }

    private static String validRequest() {
        return """
                {"type":"NOTICE","title":"점검 안내","content":"점검 내용"}
                """;
    }

    private static News news(Long id) {
        Member writer = Member.createMember("admin", "encoded-password", "관리자",
                "admin@example.com", "관리자", "01000000000");
        ReflectionTestUtils.setField(writer, "id", 1L);
        News news = News.createNews(NewsType.NOTICE, "점검 안내", "점검 내용", writer);
        ReflectionTestUtils.setField(news, "id", id);
        ReflectionTestUtils.setField(news, "createdAt", LocalDateTime.of(2026, 8, 4, 12, 0));
        ReflectionTestUtils.setField(news, "updatedAt", LocalDateTime.of(2026, 8, 4, 13, 0));
        return news;
    }
}
