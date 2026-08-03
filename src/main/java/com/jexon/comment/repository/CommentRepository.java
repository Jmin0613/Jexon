package com.jexon.comment.repository;

import com.jexon.comment.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "writer")
    Page<Comment> findAllByPostId(Long postId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "writer")
    Optional<Comment> findById(Long commentId);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);
}
