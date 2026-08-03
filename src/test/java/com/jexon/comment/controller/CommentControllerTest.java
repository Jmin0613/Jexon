package com.jexon.comment.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.auth.service.CustomUserDetailsService;
import com.jexon.comment.domain.Comment;
import com.jexon.comment.dto.request.CommentCreateRequest;
import com.jexon.comment.dto.request.CommentUpdateRequest;
import com.jexon.comment.dto.response.CommentCreateResponse;
import com.jexon.comment.dto.response.CommentResponse;
import com.jexon.comment.exception.CommentNotFoundException;
import com.jexon.comment.exception.CommentPermissionDeniedException;
import com.jexon.comment.service.CommentService;
import com.jexon.global.config.SecurityConfig;
import com.jexon.global.exception.GlobalExceptionHandler;
import com.jexon.global.security.CustomAccessDeniedHandler;
import com.jexon.global.security.CustomAuthenticationEntryPoint;
import com.jexon.member.domain.Member;
import com.jexon.post.domain.Post;
import com.jexon.post.exception.PostNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createComment() throws Exception {
        given(commentService.create(eq(10L), eq(1L), any(CommentCreateRequest.class)))
                .willReturn(CommentCreateResponse.of(100L));

        mockMvc.perform(post("/api/posts/10/comments")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"댓글 내용"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.commentId").value(100L));
    }

    @Test
    void rejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/posts/10/comments")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/posts/10/comments")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCreateReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"댓글 내용"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getCommentsIsPublic() throws Exception {
        CommentResponse response = commentResponse(100L, 1L, "댓글 내용");
        given(commentService.getComments(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(100L))
                .andExpect(jsonPath("$.content[0].content").value("댓글 내용"));
    }

    @Test
    void getCommentsUsesDefaultPageSizeTwenty() throws Exception {
        given(commentService.getComments(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/posts/10/comments"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(commentService).getComments(eq(10L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void updateComment() throws Exception {
        given(commentService.update(eq(100L), eq(1L), any(CommentUpdateRequest.class)))
                .willReturn(commentResponse(100L, 1L, "수정 내용"));

        mockMvc.perform(put("/api/comments/100")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정 내용"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(100L))
                .andExpect(jsonPath("$.content").value("수정 내용"));
    }

    @Test
    void rejectInvalidUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/comments/100")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedUpdateAndDeleteReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정 내용"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/comments/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/api/comments/100")
                        .with(authenticatedMember(1L)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(commentService).delete(100L, 1L);
    }

    @Test
    void commentNotFoundIsConvertedToCommonErrorResponse() throws Exception {
        given(commentService.update(eq(100L), eq(1L), any(CommentUpdateRequest.class)))
                .willThrow(new CommentNotFoundException());

        mockMvc.perform(put("/api/comments/100")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정 내용"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }

    @Test
    void commentPermissionDeniedIsConvertedToCommonErrorResponse() throws Exception {
        given(commentService.update(eq(100L), eq(1L), any(CommentUpdateRequest.class)))
                .willThrow(new CommentPermissionDeniedException("댓글을 수정할 권한이 없습니다."));

        mockMvc.perform(put("/api/comments/100")
                        .with(authenticatedMember(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정 내용"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("댓글을 수정할 권한이 없습니다."));
    }

    @Test
    void missingPostIsConvertedToCommonErrorResponse() throws Exception {
        given(commentService.getComments(eq(10L), any(Pageable.class)))
                .willThrow(new PostNotFoundException());

        mockMvc.perform(get("/api/posts/10/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }

    private static RequestPostProcessor authenticatedMember(Long memberId) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(principal.getMemberId()).willReturn(memberId);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(authentication);
    }

    private static CommentResponse commentResponse(Long commentId, Long writerId, String content) {
        Member writer = Member.createMember("user" + writerId, "encoded-password", "작성자",
                "writer@example.com", "사용자", "01000000000");
        ReflectionTestUtils.setField(writer, "id", writerId);
        Post post = Post.createPost("제목", "내용", writer);
        ReflectionTestUtils.setField(post, "id", 10L);
        Comment comment = Comment.createComment(content, post, writer);
        ReflectionTestUtils.setField(comment, "id", commentId);
        return CommentResponse.from(comment);
    }
}
