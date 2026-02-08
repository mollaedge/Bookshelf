package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.DtoFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.DtoFeedbackResponse;
import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.entity.EntityFeedback;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MapperFeedback {

    public EntityFeedback toFeedbackEntity(DtoFeedbackRequest request) {
        return EntityFeedback.builder()
                .note(request.note())
                .comment(request.comment())
                .book(EntityBook.builder().id(request.bookId()).build())
                .build();
    }

    public DtoFeedbackResponse toDtoFeedbackResponse(EntityFeedback feedback, Long userId) {
        return DtoFeedbackResponse.builder()
                .note(feedback.getNote())
                .comment(feedback.getComment())
                .ownFeedback(Objects.equals(feedback.getCreatedBy(), userId))
                .build();
    }
}
