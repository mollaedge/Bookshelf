package com.arturmolla.bookshelf.model.dto;

import lombok.*;

@Data
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {

    private String fullName;
    private String message;
}
