package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryNotification extends JpaRepository<EntityNotification, Long> {

    Page<EntityNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE EntityNotification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    void markAllReadByRecipientId(Long recipientId);

    void deleteByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("DELETE FROM EntityNotification n WHERE n.recipient.id = :recipientId")
    void deleteAllByRecipientId(Long recipientId);
}

