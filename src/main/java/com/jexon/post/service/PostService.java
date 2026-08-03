package com.jexon.post.service;

import com.jexon.comment.repository.CommentRepository;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import com.jexon.post.domain.Post;
import com.jexon.post.dto.request.PostCreateRequest;
import com.jexon.post.dto.request.PostUpdateRequest;
import com.jexon.post.dto.response.PostCreateResponse;
import com.jexon.post.dto.response.PostDetailResponse;
import com.jexon.post.dto.response.PostListResponse;
import com.jexon.post.exception.PostNotFoundException;
import com.jexon.post.exception.PostPermissionDeniedException;
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
public class PostService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostCreateResponse create(Long memberId, PostCreateRequest request) {
        String deniedMessage = "게시글을 작성할 권한이 없습니다.";
        Member member = findMember(memberId, deniedMessage);
        validateActiveMember(member, deniedMessage);

        Post post = Post.createPost(
                request.getTitle(),
                request.getContent(),
                member
        );
        postRepository.save(post);

        return PostCreateResponse.of(post.getId());
    }

    public Page<PostListResponse> getPosts(Pageable pageable) {
        Pageable sortedPageable = createPostPageable(pageable);

        return postRepository.findAllBy(sortedPageable)
                .map(PostListResponse::from);
    }

    public PostDetailResponse getPost(Long postId) {
        Post post = findPost(postId);

        return PostDetailResponse.from(post);
    }

    @Transactional
    public PostDetailResponse update(
            Long postId,
            Long memberId,
            PostUpdateRequest request
    ) {
        Post post = findPost(postId);
        String deniedMessage = "게시글을 수정할 권한이 없습니다.";
        Member member = findMember(memberId, deniedMessage);
        validateActiveMember(member, deniedMessage);
        validateUpdatePermission(post, memberId);

        post.update(request.getTitle(), request.getContent());

        return PostDetailResponse.from(post);
    }

    @Transactional
    public void delete(Long postId, Long memberId) {
        Post post = findPost(postId);
        String deniedMessage = "게시글을 삭제할 권한이 없습니다.";
        Member requester = findMember(memberId, deniedMessage);
        validateActiveMember(requester, deniedMessage);
        validateDeletePermission(post, requester);

        commentRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Member findMember(Long memberId, String deniedMessage) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new PostPermissionDeniedException(deniedMessage));
    }

    private void validateActiveMember(Member member, String deniedMessage) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new PostPermissionDeniedException(deniedMessage);
        }
    }

    private void validateUpdatePermission(Post post, Long memberId) {
        if (!post.getWriter().getId().equals(memberId)) {
            throw new PostPermissionDeniedException("게시글을 수정할 권한이 없습니다.");
        }
    }

    private void validateDeletePermission(Post post, Member requester) {
        boolean isWriter = post.getWriter().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == MemberRole.ADMIN;

        if (!isWriter && !isAdmin) {
            throw new PostPermissionDeniedException("게시글을 삭제할 권한이 없습니다.");
        }
    }

    private Pageable createPostPageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }
}
