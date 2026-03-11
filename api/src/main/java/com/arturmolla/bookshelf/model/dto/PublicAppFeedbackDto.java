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
public class PublicAppFeedbackDto {

    private Long id;
    private String title;
    private String description;
    private AppFeedbackStatus status;
    private int upvoteCount;
    private String age;
    private LocalDateTime createdDate;
    private String authorName;
    private List<PublicCommentDto> comments;
}

