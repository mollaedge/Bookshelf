package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.dto.AppFeedbackDto;
import com.arturmolla.bookshelf.model.dto.CommentDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppFeedbackService {

    public AppFeedbackDto save(AppFeedbackDto feedbackDto) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<AppFeedbackDto> getAll() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public AppFeedbackDto getById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public AppFeedbackDto addComment(Long id, CommentDto commentDto) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public AppFeedbackDto changeStatus(Long id, String status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
