package com.jexon.news.controller;

import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import com.jexon.member.domain.Member;
import com.jexon.news.domain.News;
import com.jexon.news.domain.NewsType;
import com.jexon.news.dto.response.NewsDetailResponse;
import com.jexon.news.dto.response.NewsListResponse;
import com.jexon.news.exception.NewsNotFoundException;
import com.jexon.news.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class NewsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getNewsListIsPublicAndUsesDefaultPageSizeTwenty() throws Exception {
        NewsListResponse response = NewsListResponse.from(news(10L));
        given(newsService.getNewsList(eq(null), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsId").value(10L))
                .andExpect(jsonPath("$.content[0].type").value("NOTICE"))
                .andExpect(jsonPath("$.content[0].title").value("점검 안내"));

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(newsService).getNewsList(eq(null), eq(null), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void passTypeKeywordAndPageableWithoutNormalization() throws Exception {
        given(newsService.getNewsList(eq(NewsType.NOTICE), eq("점검"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/news")
                        .queryParam("type", "NOTICE")
                        .queryParam("keyword", "점검")
                        .queryParam("page", "2")
                        .queryParam("size", "30"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(newsService).getNewsList(eq(NewsType.NOTICE), eq("점검"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void getNewsIsPublicAndDoesNotExposeWriter() throws Exception {
        given(newsService.getNews(10L)).willReturn(NewsDetailResponse.from(news(10L)));

        mockMvc.perform(get("/api/news/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(10L))
                .andExpect(jsonPath("$.type").value("NOTICE"))
                .andExpect(jsonPath("$.title").value("점검 안내"))
                .andExpect(jsonPath("$.content").value("점검 내용"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.writer").doesNotExist())
                .andExpect(jsonPath("$.writerId").doesNotExist())
                .andExpect(jsonPath("$.writerNickname").doesNotExist());
    }

    @Test
    void newsNotFoundIsConvertedToCommonErrorResponse() throws Exception {
        given(newsService.getNews(10L)).willThrow(new NewsNotFoundException());

        mockMvc.perform(get("/api/news/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("새소식을 찾을 수 없습니다."));
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
