package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityUserProfilePic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryUserProfilePic extends JpaRepository<EntityUserProfilePic, Long> {

    Optional<EntityUserProfilePic> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}

