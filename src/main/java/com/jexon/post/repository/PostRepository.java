package com.jexon.post.repository;

import com.jexon.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "writer")
    Page<Post> findAllBy(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "writer")
    Optional<Post> findById(Long postId);
}
