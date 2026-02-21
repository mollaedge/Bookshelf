package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryAppFeedback extends JpaRepository<EntityAppFeedback, Long> {

    Page<EntityAppFeedback> findAllByStatus(AppFeedbackStatus status, Pageable pageable);

    Page<EntityAppFeedback> findAllByCreatedBy(Long userId, Pageable pageable);
}

