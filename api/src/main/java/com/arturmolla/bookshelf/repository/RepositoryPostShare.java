package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityPostShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryPostShare extends JpaRepository<EntityPostShare, Long> {

    long countByPostId(Long postId);

    @Modifying
    @Query("DELETE FROM EntityPostShare s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EntityPostShare s WHERE s.post.id IN :postIds")
    void deleteAllByPostIdIn(@Param("postIds") List<Long> postIds);
}

