package com.jexon.news.domain;

import com.jexon.member.domain.Member;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NewsTest {
    private final Member writer = mock(Member.class);

    @Test
    void createNews() {
        News news = News.createNews(NewsType.NOTICE, "제목", "내용", writer);

        assertThat(news.getType()).isEqualTo(NewsType.NOTICE);
        assertThat(news.getTitle()).isEqualTo("제목");
        assertThat(news.getContent()).isEqualTo("내용");
        assertThat(news.getWriter()).isSameAs(writer);
    }

    @Test
    void createNewsPreservesWhitespace() {
        News news = News.createNews(NewsType.NOTICE, "  제목  ", "  내용  ", writer);

        assertThat(news.getTitle()).isEqualTo("  제목  ");
        assertThat(news.getContent()).isEqualTo("  내용  ");
    }

    @Test
    void createNewsAllowsMaximumLengths() {
        News news = News.createNews(NewsType.NOTICE, "a".repeat(150), "b".repeat(10000), writer);

        assertThat(news.getTitle()).hasSize(150);
        assertThat(news.getContent()).hasSize(10000);
    }

    @Test
    void updateNewsAndPreserveWriter() {
        News news = News.createNews(NewsType.NOTICE, "기존 제목", "기존 내용", writer);

        news.update(NewsType.EVENT, "수정 제목", "수정 내용");

        assertThat(news.getType()).isEqualTo(NewsType.EVENT);
        assertThat(news.getTitle()).isEqualTo("수정 제목");
        assertThat(news.getContent()).isEqualTo("수정 내용");
        assertThat(news.getWriter()).isSameAs(writer);
    }

    @Test
    void updateNewsPreservesWhitespace() {
        News news = News.createNews(NewsType.NOTICE, "기존 제목", "기존 내용", writer);

        news.update(NewsType.PATCH_NOTE, "  수정 제목  ", "  수정 내용  ");

        assertThat(news.getTitle()).isEqualTo("  수정 제목  ");
        assertThat(news.getContent()).isEqualTo("  수정 내용  ");
    }

    @Test
    void rejectNullType() {
        assertThatThrownBy(() -> News.createNews(null, "제목", "내용", writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 유형이 필요합니다.");
    }

    @Test
    void rejectNullAndBlankTitle() {
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, null, "내용", writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 제목을 입력해주세요.");
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, "   ", "내용", writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 제목을 입력해주세요.");
    }

    @Test
    void rejectTitleOverMaximumLength() {
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, "a".repeat(151), "내용", writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 제목은 150자 이하로 입력해주세요.");
    }

    @Test
    void rejectNullAndBlankContent() {
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, "제목", null, writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 내용을 입력해주세요.");
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, "제목", "   ", writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 내용을 입력해주세요.");
    }

    @Test
    void rejectContentOverMaximumLength() {
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, "제목", "a".repeat(10001), writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 내용은 10000자 이하로 입력해주세요.");
    }

    @Test
    void rejectNullWriter() {
        assertThatThrownBy(() -> News.createNews(NewsType.NOTICE, "제목", "내용", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새소식 작성자 정보가 필요합니다.");
    }

    @Test
    void invalidUpdatesPreserveExistingState() {
        News news = News.createNews(NewsType.NOTICE, "기존 제목", "기존 내용", writer);

        assertInvalidUpdatePreservesState(news, null, "수정 제목", "수정 내용");
        assertInvalidUpdatePreservesState(news, NewsType.EVENT, "   ", "수정 내용");
        assertInvalidUpdatePreservesState(news, NewsType.EVENT, "수정 제목", "a".repeat(10001));
    }

    private void assertInvalidUpdatePreservesState(
            News news,
            NewsType type,
            String title,
            String content
    ) {
        assertThatThrownBy(() -> news.update(type, title, content))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(news.getType()).isEqualTo(NewsType.NOTICE);
        assertThat(news.getTitle()).isEqualTo("기존 제목");
        assertThat(news.getContent()).isEqualTo("기존 내용");
        assertThat(news.getWriter()).isSameAs(writer);
    }
}
