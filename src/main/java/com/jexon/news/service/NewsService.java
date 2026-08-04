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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {
    private static final int MAX_PAGE_SIZE = 100;

    private final NewsRepository newsRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public NewsCreateResponse create(Long memberId, NewsCreateRequest request) {
        String deniedMessage = "새소식을 작성할 권한이 없습니다.";
        Member member = findActiveAdmin(memberId, deniedMessage);

        News news = News.createNews(
                request.getType(),
                request.getTitle(),
                request.getContent(),
                member
        );
        newsRepository.save(news);

        return NewsCreateResponse.of(news.getId());
    }

    public Page<NewsListResponse> getNewsList(
            NewsType type,
            String keyword,
            Pageable pageable
    ) {
        Pageable sortedPageable = createNewsPageable(pageable);
        String normalizedKeyword = normalizeKeyword(keyword);

        return newsRepository.search(type, normalizedKeyword, sortedPageable)
                .map(NewsListResponse::from);
    }

    public NewsDetailResponse getNews(Long newsId) {
        News news = findNews(newsId);

        return NewsDetailResponse.from(news);
    }

    @Transactional
    public NewsDetailResponse update(
            Long newsId,
            Long memberId,
            NewsUpdateRequest request
    ) {
        News news = findNews(newsId);
        String deniedMessage = "새소식을 수정할 권한이 없습니다.";
        findActiveAdmin(memberId, deniedMessage);

        news.update(
                request.getType(),
                request.getTitle(),
                request.getContent()
        );

        return NewsDetailResponse.from(news);
    }

    @Transactional
    public void delete(Long newsId, Long memberId) {
        News news = findNews(newsId);
        String deniedMessage = "새소식을 삭제할 권한이 없습니다.";
        findActiveAdmin(memberId, deniedMessage);

        newsRepository.delete(news);
    }

    private News findNews(Long newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(NewsNotFoundException::new);
    }

    private Member findActiveAdmin(Long memberId, String deniedMessage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NewsPermissionDeniedException(deniedMessage));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new NewsPermissionDeniedException(deniedMessage);
        }
        if (member.getRole() != MemberRole.ADMIN) {
            throw new NewsPermissionDeniedException(deniedMessage);
        }

        return member;
    }

    private Pageable createNewsPageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}
