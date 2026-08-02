package com.jexon.post.domain;

import com.jexon.global.entity.BaseTimeEntity;
import com.jexon.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private Member writer;

    private Post(String title, String content, Member writer) {
        validateTitle(title);
        validateContent(content);
        validateWriter(writer);

        this.title = title;
        this.content = content;
        this.writer = writer;
    }

    public static Post createPost(String title, String content, Member writer) {
        return new Post(title, content, writer);
    }

    public void update(String title, String content) {
        validateTitle(title);
        validateContent(content);

        this.title = title;
        this.content = content;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("게시글 제목을 입력해주세요.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("게시글 제목은 100자 이하로 입력해주세요.");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("게시글 내용을 입력해주세요.");
        }
        if (content.length() > 5000) {
            throw new IllegalArgumentException("게시글 내용은 5000자 이하로 입력해주세요.");
        }
    }

    private static void validateWriter(Member writer) {
        if (writer == null) {
            throw new IllegalArgumentException("게시글 작성자 정보가 필요합니다.");
        }
    }
}
