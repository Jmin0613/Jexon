package com.jexon.news.service;

import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import com.jexon.news.domain.News;
import com.jexon.news.domain.NewsType;
import com.jexon.news.dto.request.NewsCreateRequest;
import com.jexon.news.dto.request.NewsUpdateRequest;
import com.jexon.news.dto.response.NewsCreateResponse;
import com.jexon.news.dto.response.NewsDetailResponse;
import com.jexon.news.dto.response.NewsListResponse;
import com.jexon.news.exception.NewsNotFoundException;
import com.jexon.news.exception.NewsPermissionDeniedException;
import com.jexon.news.repository.NewsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NewsCreateRequest createRequest;
    @Mock
    private NewsUpdateRequest updateRequest;
    @InjectMocks
    private NewsService newsService;

    @Test
    void createNewsByActiveAdmin() {
        Member admin = member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
        given(createRequest.getType()).willReturn(NewsType.NOTICE);
        given(createRequest.getTitle()).willReturn("점검 안내");
        given(createRequest.getContent()).willReturn("점검 내용");
        doAnswer(invocation -> {
            News savedNews = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedNews, "id", 10L);
            return savedNews;
        }).when(newsRepository).save(any(News.class));

        NewsCreateResponse response = newsService.create(1L, createRequest);

        verify(memberRepository).findById(1L);
        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(captor.capture());
        News saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NewsType.NOTICE);
        assertThat(saved.getTitle()).isEqualTo("점검 안내");
        assertThat(saved.getContent()).isEqualTo("점검 내용");
        assertThat(saved.getWriter()).isSameAs(admin);
        assertThat(response.getNewsId()).isEqualTo(10L);
    }

    @Test
    void rejectCreateForMissingOrUnauthorizedMember() {
        assertCreateDenied(null);
        assertCreateDenied(member(1L, MemberStatus.SUSPENDED, MemberRole.ADMIN));
        assertCreateDenied(member(1L, MemberStatus.WITHDRAWN, MemberRole.ADMIN));
        assertCreateDenied(member(1L, MemberStatus.ACTIVE, MemberRole.USER));
    }

    @Test
    void getNewsListAppliesFiltersMaximumSizeAndStableSort() {
        News news = news(10L, member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN));
        given(newsRepository.search(eq(NewsType.NOTICE), eq("점검"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(news)));
        Pageable clientPageable = PageRequest.of(3, 500, Sort.by("title"));

        Page<NewsListResponse> response = newsService.getNewsList(
                NewsType.NOTICE,
                "  점검  ",
                clientPageable
        );

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(newsRepository).search(eq(NewsType.NOTICE), eq("점검"), captor.capture());
        Pageable applied = captor.getValue();
        assertThat(applied.getPageNumber()).isEqualTo(3);
        assertThat(applied.getPageSize()).isEqualTo(100);
        assertThat(applied.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(applied.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(applied.getSort().getOrderFor("title")).isNull();
        assertThat(response.getContent().getFirst().getNewsId()).isEqualTo(10L);
    }

    @Test
    void getNewsListNormalizesNullAndBlankKeywords() {
        given(newsRepository.search(any(), any(), any())).willReturn(Page.empty());

        newsService.getNewsList(null, null, PageRequest.of(0, 20));
        newsService.getNewsList(null, "   ", PageRequest.of(0, 20));

        verify(newsRepository, org.mockito.Mockito.times(2))
                .search(eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getNewsListKeepsPageSizeWithinMaximum() {
        given(newsRepository.search(any(), any(), any())).willReturn(Page.empty());

        newsService.getNewsList(null, null, PageRequest.of(2, 30));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(newsRepository).search(eq(null), eq(null), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void getNewsWithoutMemberLookup() {
        News news = news(10L, member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN));
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));

        NewsDetailResponse response = newsService.getNews(10L);

        assertThat(response.getNewsId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("기존 제목");
        verifyNoInteractions(memberRepository);
    }

    @Test
    void rejectGetWhenNewsDoesNotExist() {
        given(newsRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.getNews(10L))
                .isInstanceOf(NewsNotFoundException.class);
        verifyNoInteractions(memberRepository);
    }

    @Test
    void updateNewsByDifferentActiveAdminWithoutSavingAgain() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN);
        Member requester = member(2L, MemberStatus.ACTIVE, MemberRole.ADMIN);
        News news = news(10L, writer);
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));
        given(memberRepository.findById(2L)).willReturn(Optional.of(requester));
        given(updateRequest.getType()).willReturn(NewsType.EVENT);
        given(updateRequest.getTitle()).willReturn("수정 제목");
        given(updateRequest.getContent()).willReturn("수정 내용");

        NewsDetailResponse response = newsService.update(10L, 2L, updateRequest);

        var order = inOrder(newsRepository, memberRepository);
        order.verify(newsRepository).findById(10L);
        order.verify(memberRepository).findById(2L);
        assertThat(news.getType()).isEqualTo(NewsType.EVENT);
        assertThat(news.getTitle()).isEqualTo("수정 제목");
        assertThat(news.getContent()).isEqualTo("수정 내용");
        assertThat(news.getWriter()).isSameAs(writer);
        assertThat(response.getNewsId()).isEqualTo(10L);
        verify(newsRepository, never()).save(any());
    }

    @Test
    void rejectUpdateWhenNewsDoesNotExistBeforeMemberLookup() {
        given(newsRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.update(10L, 2L, updateRequest))
                .isInstanceOf(NewsNotFoundException.class);
        verifyNoInteractions(memberRepository);
        verify(newsRepository, never()).save(any());
    }

    @Test
    void rejectUpdateForMissingOrUnauthorizedMemberWithoutChangingNews() {
        assertUpdateDenied(null);
        assertUpdateDenied(member(2L, MemberStatus.SUSPENDED, MemberRole.ADMIN));
        assertUpdateDenied(member(2L, MemberStatus.WITHDRAWN, MemberRole.ADMIN));
        assertUpdateDenied(member(2L, MemberStatus.ACTIVE, MemberRole.USER));
    }

    @Test
    void deleteNewsByDifferentActiveAdmin() {
        News news = news(10L, member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN));
        Member requester = member(2L, MemberStatus.ACTIVE, MemberRole.ADMIN);
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));
        given(memberRepository.findById(2L)).willReturn(Optional.of(requester));

        newsService.delete(10L, 2L);

        verify(newsRepository).delete(news);
    }

    @Test
    void rejectDeleteWhenNewsDoesNotExistBeforeMemberLookup() {
        given(newsRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.delete(10L, 2L))
                .isInstanceOf(NewsNotFoundException.class);
        verifyNoInteractions(memberRepository);
        verify(newsRepository, never()).delete(any());
    }

    @Test
    void rejectDeleteForMissingOrUnauthorizedMember() {
        assertDeleteDenied(null);
        assertDeleteDenied(member(2L, MemberStatus.SUSPENDED, MemberRole.ADMIN));
        assertDeleteDenied(member(2L, MemberStatus.WITHDRAWN, MemberRole.ADMIN));
        assertDeleteDenied(member(2L, MemberStatus.ACTIVE, MemberRole.USER));
    }

    private void assertCreateDenied(Member requester) {
        given(memberRepository.findById(1L)).willReturn(Optional.ofNullable(requester));

        assertThatThrownBy(() -> newsService.create(1L, createRequest))
                .isInstanceOf(NewsPermissionDeniedException.class)
                .hasMessage("새소식을 작성할 권한이 없습니다.");
        verify(newsRepository, never()).save(any());
    }

    private void assertUpdateDenied(Member requester) {
        News news = news(10L, member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN));
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));
        given(memberRepository.findById(2L)).willReturn(Optional.ofNullable(requester));

        assertThatThrownBy(() -> newsService.update(10L, 2L, updateRequest))
                .isInstanceOf(NewsPermissionDeniedException.class)
                .hasMessage("새소식을 수정할 권한이 없습니다.");
        assertThat(news.getType()).isEqualTo(NewsType.NOTICE);
        assertThat(news.getTitle()).isEqualTo("기존 제목");
        assertThat(news.getContent()).isEqualTo("기존 내용");
        verify(newsRepository, never()).save(any());
    }

    private void assertDeleteDenied(Member requester) {
        News news = news(10L, member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN));
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));
        given(memberRepository.findById(2L)).willReturn(Optional.ofNullable(requester));

        assertThatThrownBy(() -> newsService.delete(10L, 2L))
                .isInstanceOf(NewsPermissionDeniedException.class)
                .hasMessage("새소식을 삭제할 권한이 없습니다.");
        verify(newsRepository, never()).delete(any());
    }

    private static Member member(Long id, MemberStatus status, MemberRole role) {
        Member member = Member.createMember("user" + id, "encoded-password", "nickname" + id,
                "user" + id + "@example.com", "사용자", "01000000000");
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }

    private static News news(Long id, Member writer) {
        News news = News.createNews(NewsType.NOTICE, "기존 제목", "기존 내용", writer);
        ReflectionTestUtils.setField(news, "id", id);
        ReflectionTestUtils.setField(news, "createdAt", LocalDateTime.of(2026, 8, 4, 12, 0));
        ReflectionTestUtils.setField(news, "updatedAt", LocalDateTime.of(2026, 8, 4, 12, 0));
        return news;
    }
}
