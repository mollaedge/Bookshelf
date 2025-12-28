package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.dto.AppFeedbackDto;
import com.arturmolla.bookshelf.model.dto.CommentDto;
import com.arturmolla.bookshelf.service.AppFeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("app-feedbacks")
@RequiredArgsConstructor
@Tag(name = "Application Feedback")
public class ControllerAppFeedback {

    private final AppFeedbackService appFeedbackService;

    @PostMapping
    public ResponseEntity<AppFeedbackDto> save(@RequestBody AppFeedbackDto feedbackDto) {
        return ResponseEntity.ok(appFeedbackService.save(feedbackDto));
    }

    @GetMapping
    public ResponseEntity<List<AppFeedbackDto>> getAll() {
        return ResponseEntity.ok(appFeedbackService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppFeedbackDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appFeedbackService.getById(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<AppFeedbackDto> addComment(@PathVariable Long id, @RequestBody CommentDto commentDto) {
        return ResponseEntity.ok(appFeedbackService.addComment(id, commentDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        appFeedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppFeedbackDto> changeStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(appFeedbackService.changeStatus(id, status));
    }
}

