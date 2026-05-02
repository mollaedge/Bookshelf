package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoAppFeedback {

    private Long id;
    private String title;
    private String description;
    private AppFeedbackStatus status;
    private int upvoteCount;
    // Populated for authenticated views; false for public views
    private boolean upvotedByCurrentUser;
    private boolean ownFeedback;
    private String age;
    private LocalDateTime createdDate;
    // Populated for authenticated views; null for public views
    private Long createdBy;
    // Populated for public views; null for authenticated views
    private String authorName;
    private List<DtoComment> comments;
}

