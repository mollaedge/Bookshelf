package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityUserWallpaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryUserWallpaper extends JpaRepository<EntityUserWallpaper, Long> {

    Optional<EntityUserWallpaper> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}

