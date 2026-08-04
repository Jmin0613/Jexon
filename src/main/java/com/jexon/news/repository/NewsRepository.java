package com.jexon.news.repository;

import com.jexon.news.domain.News;
import com.jexon.news.domain.NewsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {
    @Query("""
            select n
            from News n
            where (:type is null or n.type = :type)
              and (:keyword is null or n.title like concat('%', :keyword, '%'))
            """)
    Page<News> search(
            @Param("type") NewsType type,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
