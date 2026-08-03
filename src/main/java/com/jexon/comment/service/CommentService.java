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
public class CommentService {
    private static final int MAX_PAGE_SIZE = 100;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CommentCreateResponse create(
            Long postId,
            Long memberId,
            CommentCreateRequest request
    ) {
        Post post = findPost(postId);
        String deniedMessage = "댓글을 작성할 권한이 없습니다.";
        Member member = findMember(memberId, deniedMessage);
        validateActiveMember(member, deniedMessage);

        Comment comment = Comment.createComment(
                request.getContent(),
                post,
                member
        );
        commentRepository.save(comment);

        return CommentCreateResponse.of(comment.getId());
    }

    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        findPost(postId);
        Pageable sortedPageable = createCommentPageable(pageable);

        return commentRepository.findAllByPostId(postId, sortedPageable)
                .map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse update(
            Long commentId,
            Long memberId,
            CommentUpdateRequest request
    ) {
        Comment comment = findComment(commentId);
        String deniedMessage = "댓글을 수정할 권한이 없습니다.";
        Member member = findMember(memberId, deniedMessage);
        validateActiveMember(member, deniedMessage);
        validateUpdatePermission(comment, memberId);

        comment.update(request.getContent());

        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long commentId, Long memberId) {
        Comment comment = findComment(commentId);
        String deniedMessage = "댓글을 삭제할 권한이 없습니다.";
        Member requester = findMember(memberId, deniedMessage);
        validateActiveMember(requester, deniedMessage);
        validateDeletePermission(comment, requester);

        commentRepository.delete(comment);
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Member findMember(Long memberId, String deniedMessage) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CommentPermissionDeniedException(deniedMessage));
    }

    private void validateActiveMember(Member member, String deniedMessage) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new CommentPermissionDeniedException(deniedMessage);
        }
    }

    private void validateUpdatePermission(Comment comment, Long memberId) {
        if (!comment.getWriter().getId().equals(memberId)) {
            throw new CommentPermissionDeniedException("댓글을 수정할 권한이 없습니다.");
        }
    }

    private void validateDeletePermission(Comment comment, Member requester) {
        boolean isWriter = comment.getWriter().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == MemberRole.ADMIN;

        if (!isWriter && !isAdmin) {
            throw new CommentPermissionDeniedException("댓글을 삭제할 권한이 없습니다.");
        }
    }

    private Pageable createCommentPageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }
}
