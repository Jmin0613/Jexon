package com.jexon.comment.service;

import com.jexon.comment.domain.Comment;
import com.jexon.comment.dto.request.CommentCreateRequest;
import com.jexon.comment.dto.request.CommentUpdateRequest;
import com.jexon.comment.dto.response.CommentCreateResponse;
import com.jexon.comment.dto.response.CommentResponse;
import com.jexon.comment.exception.CommentNotFoundException;
import com.jexon.comment.exception.CommentPermissionDeniedException;
import com.jexon.comment.repository.CommentRepository;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import com.jexon.post.domain.Post;
import com.jexon.post.exception.PostNotFoundException;
import com.jexon.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CommentCreateRequest createRequest;
    @Mock
    private CommentUpdateRequest updateRequest;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository, memberRepository);
    }

    @Test
    void createComment() {
        Member member = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Post post = post(10L, member);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(createRequest.getContent()).willReturn("댓글 내용");
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        CommentCreateResponse response = commentService.create(10L, 1L, createRequest);

        assertThat(response.getCommentId()).isEqualTo(100L);
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("댓글 내용");
        assertThat(captor.getValue().getPost()).isSameAs(post);
        assertThat(captor.getValue().getWriter()).isSameAs(member);
    }

    @Test
    void rejectCreateWhenPostDoesNotExist() {
        given(postRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(10L, 1L, createRequest))
                .isInstanceOf(PostNotFoundException.class);
        verify(memberRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void rejectCreateWhenMemberDoesNotExist() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post(10L, member(2L, MemberStatus.ACTIVE, MemberRole.USER))));
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(10L, 1L, createRequest))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessage("댓글을 작성할 권한이 없습니다.");
        verify(commentRepository, never()).save(any());
    }

    @Test
    void rejectCreateWhenMemberIsSuspended() {
        assertCreateDeniedFor(MemberStatus.SUSPENDED);
    }

    @Test
    void rejectCreateWhenMemberIsWithdrawn() {
        assertCreateDeniedFor(MemberStatus.WITHDRAWN);
    }

    @Test
    void getCommentsAppliesStableLatestSortAndMaximumSize() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Post post = post(10L, writer);
        Comment comment = comment(100L, post, writer, "댓글");
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findAllByPostId(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));
        Pageable clientPageable = PageRequest.of(3, 200, Sort.by("content"));

        Page<CommentResponse> response = commentService.getComments(10L, clientPageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getCommentId()).isEqualTo(100L);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        InOrder inOrder = inOrder(postRepository, commentRepository);
        inOrder.verify(postRepository).findById(10L);
        inOrder.verify(commentRepository).findAllByPostId(org.mockito.ArgumentMatchers.eq(10L), captor.capture());
        Pageable applied = captor.getValue();
        assertThat(applied.getPageNumber()).isEqualTo(3);
        assertThat(applied.getPageSize()).isEqualTo(100);
        assertThat(applied.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(applied.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(applied.getSort().getOrderFor("content")).isNull();
    }

    @Test
    void rejectGetCommentsWhenPostDoesNotExist() {
        given(postRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComments(10L, PageRequest.of(0, 20)))
                .isInstanceOf(PostNotFoundException.class);
        verify(commentRepository, never()).findAllByPostId(anyLong(), any());
    }

    @Test
    void updateCommentByWriterWithoutSavingAgain() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Comment comment = comment(100L, post(10L, writer), writer, "기존 내용");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(memberRepository.findById(1L)).willReturn(Optional.of(writer));
        given(updateRequest.getContent()).willReturn("수정 내용");

        CommentResponse response = commentService.update(100L, 1L, updateRequest);

        assertThat(response.getContent()).isEqualTo("수정 내용");
        assertThat(comment.getContent()).isEqualTo("수정 내용");
        verify(commentRepository, never()).save(any());
    }

    @Test
    void rejectUpdateByOtherMember() {
        assertUpdateDenied(member(2L, MemberStatus.ACTIVE, MemberRole.USER));
    }

    @Test
    void rejectUpdateByAdminWhoIsNotWriter() {
        assertUpdateDenied(member(2L, MemberStatus.ACTIVE, MemberRole.ADMIN));
    }

    @Test
    void rejectUpdateByInactiveMember() {
        assertInactiveUpdateDenied(MemberStatus.SUSPENDED);
        assertInactiveUpdateDenied(MemberStatus.WITHDRAWN);
    }

    @Test
    void rejectUpdateWhenCommentDoesNotExist() {
        given(commentRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(100L, 1L, updateRequest))
                .isInstanceOf(CommentNotFoundException.class);
        verify(memberRepository, never()).findById(anyLong());
    }

    @Test
    void deleteCommentByWriter() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Comment comment = comment(100L, post(10L, writer), writer, "댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(memberRepository.findById(1L)).willReturn(Optional.of(writer));

        commentService.delete(100L, 1L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteOtherMembersCommentByAdmin() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Member admin = member(2L, MemberStatus.ACTIVE, MemberRole.ADMIN);
        Comment comment = comment(100L, post(10L, writer), writer, "댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(memberRepository.findById(2L)).willReturn(Optional.of(admin));

        commentService.delete(100L, 2L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void rejectDeleteByOtherMember() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Member requester = member(2L, MemberStatus.ACTIVE, MemberRole.USER);
        Comment comment = comment(100L, post(10L, writer), writer, "댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(memberRepository.findById(2L)).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> commentService.delete(100L, 2L))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessage("댓글을 삭제할 권한이 없습니다.");
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void rejectDeleteByInactiveMember() {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Comment comment = comment(100L, post(10L, writer), writer, "댓글");
        for (MemberStatus status : List.of(MemberStatus.SUSPENDED, MemberStatus.WITHDRAWN)) {
            Member requester = member(1L, status, MemberRole.USER);
            given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
            given(memberRepository.findById(1L)).willReturn(Optional.of(requester));

            assertThatThrownBy(() -> commentService.delete(100L, 1L))
                    .isInstanceOf(CommentPermissionDeniedException.class)
                    .hasMessage("댓글을 삭제할 권한이 없습니다.");
        }
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void rejectDeleteWhenCommentDoesNotExist() {
        given(commentRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(100L, 1L))
                .isInstanceOf(CommentNotFoundException.class);
        verify(commentRepository, never()).delete(any());
    }

    private void assertCreateDeniedFor(MemberStatus status) {
        Member member = member(1L, status, MemberRole.USER);
        given(postRepository.findById(10L)).willReturn(Optional.of(post(10L, member)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> commentService.create(10L, 1L, createRequest))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessage("댓글을 작성할 권한이 없습니다.");
        verify(commentRepository, never()).save(any());
    }

    private void assertUpdateDenied(Member requester) {
        Member writer = member(1L, MemberStatus.ACTIVE, MemberRole.USER);
        Comment comment = comment(100L, post(10L, writer), writer, "기존 내용");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(memberRepository.findById(requester.getId())).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> commentService.update(100L, requester.getId(), updateRequest))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessage("댓글을 수정할 권한이 없습니다.");
        assertThat(comment.getContent()).isEqualTo("기존 내용");
        verify(commentRepository, never()).save(any());
    }

    private void assertInactiveUpdateDenied(MemberStatus status) {
        Member writer = member(1L, status, MemberRole.USER);
        Comment comment = comment(100L, post(10L, writer), writer, "기존 내용");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(memberRepository.findById(1L)).willReturn(Optional.of(writer));

        assertThatThrownBy(() -> commentService.update(100L, 1L, updateRequest))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessage("댓글을 수정할 권한이 없습니다.");
        assertThat(comment.getContent()).isEqualTo("기존 내용");
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

    private static Comment comment(Long id, Post post, Member writer, String content) {
        Comment comment = Comment.createComment(content, post, writer);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
