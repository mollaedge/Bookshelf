package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.DtoFeedbackResponse;
import com.arturmolla.bookshelf.model.entity.EntityFeedback;
import com.arturmolla.bookshelf.model.enums.NotificationType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryBook;
import com.arturmolla.bookshelf.repository.RepositoryFeedback;
import com.arturmolla.bookshelf.service.mapper.MapperFeedback;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ServiceFeedback {

    private final RepositoryBook repositoryBook;
    private final MapperFeedback mapperFeedback;
    private final RepositoryFeedback repositoryFeedback;
    private final ServiceNotification serviceNotification;

    public Long saveFeedback(DtoFeedbackRequest request, Authentication connectedUser) {
        var book = repositoryBook.findById(request.bookId())
                .orElseThrow(() -> new EntityNotFoundException("Book was not found"));
        if (book.getArchived() || !book.getShareable()) {
            throw new OperationNotPermittedException("You can not give feedback for archived/non-shareable book!");
        }
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You own this book!");
        }
        var feedback = mapperFeedback.toFeedbackEntity(request);
        Long feedbackId = repositoryFeedback.save(feedback).getId();

        // Notify book owner about new feedback
        serviceNotification.notify(
                book.getOwner(), user,
                NotificationType.FEEDBACK_RECEIVED,
                user.getFullName() + " left feedback on your book",
                "New rating (" + request.note() + "/5) for \"" + book.getTitle() + "\".",
                book.getId(), "BOOK"
        );

        return feedbackId;
    }

    public PageResponse<DtoFeedbackResponse> findFeedbacksForBook(Long bookId, int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size);
        var user = (User) connectedUser.getPrincipal();
        Page<EntityFeedback> feedbacks = repositoryFeedback.findAllByBookId(bookId, pageable);
        List<DtoFeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> mapperFeedback.toDtoFeedbackResponse(f, user.getId()))
                .toList();
        return new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast());
    }
}
