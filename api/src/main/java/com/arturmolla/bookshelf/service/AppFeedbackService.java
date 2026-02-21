package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.AppFeedbackDto;
import com.arturmolla.bookshelf.model.dto.AppFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.CommentDto;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedbackComment;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryAppFeedback;
import com.arturmolla.bookshelf.service.mapper.MapperAppFeedback;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class AppFeedbackService {

    private static final String FEEDBACK_NOT_FOUND = "App feedback not found with id: ";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final RepositoryAppFeedback repositoryAppFeedback;
    private final MapperAppFeedback mapperAppFeedback;

    public AppFeedbackDto save(AppFeedbackRequest request, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = mapperAppFeedback.toEntity(request);
        EntityAppFeedback saved = repositoryAppFeedback.save(feedback);
        return mapperAppFeedback.toDto(saved, user.getId());
    }

    public PageResponse<AppFeedbackDto> getAll(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityAppFeedback> feedbacks = repositoryAppFeedback.findAll(pageable);
        List<AppFeedbackDto> content = feedbacks.stream()
                .map(f -> mapperAppFeedback.toDto(f, user.getId()))
                .toList();
        return new PageResponse<>(
                content,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }

    public AppFeedbackDto getById(Long id, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        return mapperAppFeedback.toDto(feedback, user.getId());
    }

    public AppFeedbackDto upvote(Long id, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        if (feedback.getUpvotedBy().contains(user.getId())) {
            feedback.getUpvotedBy().remove(user.getId());
        } else {
            feedback.getUpvotedBy().add(user.getId());
        }
        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), user.getId());
    }

    public AppFeedbackDto edit(Long id, AppFeedbackRequest request, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        boolean isAdmin = isAdmin(connectedUser);
        if (!isAdmin && !Objects.equals(feedback.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can only edit your own feedback.");
        }
        feedback.setTitle(request.title());
        feedback.setDescription(request.description());
        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), user.getId());
    }

    public AppFeedbackDto addComment(Long id, CommentDto commentDto, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        EntityAppFeedbackComment comment = EntityAppFeedbackComment.builder()
                .authorId(user.getId())
                .authorName(user.getFullName())
                .message(commentDto.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
        feedback.getComments().add(comment);
        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), user.getId());
    }

    public void delete(Long id, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        boolean isAdmin = isAdmin(connectedUser);
        if (!isAdmin && !Objects.equals(feedback.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can only delete your own feedback.");
        }
        repositoryAppFeedback.delete(feedback);
    }

    public AppFeedbackDto changeStatus(Long id, AppFeedbackStatus status, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        if (!isAdmin(connectedUser)) {
            throw new OperationNotPermittedException("Only admins can change the status of a feedback.");
        }
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        feedback.setStatus(status);
        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), user.getId());
    }

    public PageResponse<AppFeedbackDto> getMyFeedbacks(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityAppFeedback> feedbacks = repositoryAppFeedback.findAllByCreatedBy(user.getId(), pageable);
        List<AppFeedbackDto> content = feedbacks.stream()
                .map(f -> mapperAppFeedback.toDto(f, user.getId()))
                .toList();
        return new PageResponse<>(
                content,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }

    public PageResponse<AppFeedbackDto> getAllByStatus(AppFeedbackStatus status, int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityAppFeedback> feedbacks = repositoryAppFeedback.findAllByStatus(status, pageable);
        List<AppFeedbackDto> content = feedbacks.stream()
                .map(f -> mapperAppFeedback.toDto(f, user.getId()))
                .toList();
        return new PageResponse<>(
                content,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ADMIN_ROLE));
    }
}
