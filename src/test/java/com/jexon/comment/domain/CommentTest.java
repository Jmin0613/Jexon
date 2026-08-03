package com.jexon.comment.domain;

import com.jexon.member.domain.Member;
import com.jexon.post.domain.Post;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CommentTest {
    private final Post post = mock(Post.class);
    private final Member writer = mock(Member.class);

    @Test
    void createComment() {
        Comment comment = Comment.createComment("댓글 내용", post, writer);

        assertThat(comment.getContent()).isEqualTo("댓글 내용");
        assertThat(comment.getPost()).isSameAs(post);
        assertThat(comment.getWriter()).isSameAs(writer);
    }

    @Test
    void createCommentPreservesWhitespace() {
        Comment comment = Comment.createComment("  댓글 내용  ", post, writer);

        assertThat(comment.getContent()).isEqualTo("  댓글 내용  ");
    }

    @Test
    void createCommentAllowsOneThousandCharacters() {
        String content = "a".repeat(1000);

        Comment comment = Comment.createComment(content, post, writer);

        assertThat(comment.getContent()).hasSize(1000);
    }

    @Test
    void updateComment() {
        Comment comment = Comment.createComment("기존 내용", post, writer);

        comment.update("수정 내용");

        assertThat(comment.getContent()).isEqualTo("수정 내용");
    }

    @Test
    void rejectNullContent() {
        assertThatThrownBy(() -> Comment.createComment(null, post, writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 내용을 입력해주세요.");
    }

    @Test
    void rejectBlankContent() {
        assertThatThrownBy(() -> Comment.createComment("   ", post, writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 내용을 입력해주세요.");
    }

    @Test
    void rejectContentOverOneThousandCharacters() {
        assertThatThrownBy(() -> Comment.createComment("a".repeat(1001), post, writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 내용은 1000자 이하로 입력해주세요.");
    }

    @Test
    void rejectNullPost() {
        assertThatThrownBy(() -> Comment.createComment("댓글 내용", null, writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글이 작성될 게시글이 필요합니다.");
    }

    @Test
    void rejectNullWriter() {
        assertThatThrownBy(() -> Comment.createComment("댓글 내용", post, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 작성자가 필요합니다.");
    }

    @Test
    void rejectInvalidUpdateWithoutChangingContent() {
        Comment comment = Comment.createComment("기존 내용", post, writer);

        assertThatThrownBy(() -> comment.update("a".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 내용은 1000자 이하로 입력해주세요.");
        assertThat(comment.getContent()).isEqualTo("기존 내용");
    }
}
