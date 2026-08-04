package com.jexon.news.domain;

import com.jexon.global.entity.BaseTimeEntity;
import com.jexon.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "news")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class News extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NewsType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private Member writer;

    private News(NewsType type, String title, String content, Member writer) {
        validateType(type);
        validateTitle(title);
        validateContent(content);
        validateWriter(writer);

        this.type = type;
        this.title = title;
        this.content = content;
        this.writer = writer;
    }

    public static News createNews(
            NewsType type,
            String title,
            String content,
            Member writer
    ) {
        return new News(type, title, content, writer);
    }

    public void update(NewsType type, String title, String content) {
        validateType(type);
        validateTitle(title);
        validateContent(content);

        this.type = type;
        this.title = title;
        this.content = content;
    }

    private static void validateType(NewsType type) {
        if (type == null) {
            throw new IllegalArgumentException("새소식 유형이 필요합니다.");
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("새소식 제목을 입력해주세요.");
        }
        if (title.length() > 150) {
            throw new IllegalArgumentException("새소식 제목은 150자 이하로 입력해주세요.");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("새소식 내용을 입력해주세요.");
        }
        if (content.length() > 10000) {
            throw new IllegalArgumentException("새소식 내용은 10000자 이하로 입력해주세요.");
        }
    }

    private static void validateWriter(Member writer) {
        if (writer == null) {
            throw new IllegalArgumentException("새소식 작성자 정보가 필요합니다.");
        }
    }
}
