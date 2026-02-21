package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReadingActivityDto {

    /**
     * Format: "YYYY-MM"
     */
    private String month;
    @Builder.Default
    private long booksRead = 0L;
}

