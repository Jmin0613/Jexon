package com.jexon.post.service;

import com.jexon.comment.repository.CommentRepository;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import com.jexon.post.domain.Post;
import com.jexon.post.exception.PostPermissionDeniedException;
import com.jexon.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceCommentDeletionTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CommentRepository commentRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, memberRepository, commentRepository);
    }

    @Test
    void deleteCommentsBeforeDeletingPost() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Post post = post(10L, writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(writer));
        given(commentRepository.deleteAllByPostId(10L)).willReturn(2);

        postService.delete(10L, 1L);

        InOrder inOrder = inOrder(postRepository, memberRepository, commentRepository);
        inOrder.verify(postRepository).findById(10L);
        inOrder.verify(memberRepository).findById(1L);
        inOrder.verify(commentRepository).deleteAllByPostId(10L);
        inOrder.verify(postRepository).delete(post);
    }

    @Test
    void deletePostEvenWhenThereAreNoComments() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Post post = post(10L, writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(writer));
        given(commentRepository.deleteAllByPostId(10L)).willReturn(0);

        postService.delete(10L, 1L);

        verify(postRepository).delete(post);
    }

    @Test
    void adminCanDeleteOtherMembersPost() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Member admin = member(2L, MemberStatus.ACTIVE, MemberRole.ADMIN);
        Post post = post(10L, writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.findById(2L)).willReturn(Optional.of(admin));

        postService.delete(10L, 2L);

        InOrder inOrder = inOrder(commentRepository, postRepository);
        inOrder.verify(commentRepository).deleteAllByPostId(10L);
        inOrder.verify(postRepository).delete(post);
    }

    @Test
    void doNotDeleteAnythingWhenRequesterHasNoPermission() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Member requester = member(2L, MemberStatus.ACTIVE, MemberRole.USER);
        Post post = post(10L, writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.findById(2L)).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> postService.delete(10L, 2L))
                .isInstanceOf(PostPermissionDeniedException.class);
        verify(commentRepository, never()).deleteAllByPostId(any());
        verify(postRepository, never()).delete(any());
    }

    @Test
    void doNotDeleteAnythingWhenRequesterIsInactive() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Member requester = member(1L, MemberStatus.SUSPENDED, MemberRole.USER);
        Post post = post(10L, writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> postService.delete(10L, 1L))
                .isInstanceOf(PostPermissionDeniedException.class);
        verify(commentRepository, never()).deleteAllByPostId(any());
        verify(postRepository, never()).delete(any());
    }

    private static Member member(Long id, MemberStatus status, MemberRole role) {
        Member member = Member.createMember("user" + id, "encoded-password", "nickname" + id,
                "user" + id + "@example.com", "사용자", "01000000000");
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }

    private static Post post(Long id, Member writer) {
        Post post = Post.createPost("제목", "내용", writer);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
