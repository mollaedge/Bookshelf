package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppFeedbackDto {

    private Long id;
    private String title;
    private String description;
    private AppFeedbackStatus status;
    private int upvoteCount;
    private boolean upvotedByCurrentUser;
    private boolean ownFeedback;
    private String age;
    private LocalDateTime createdDate;
    private Long createdBy;
    private List<CommentDto> comments;
}
