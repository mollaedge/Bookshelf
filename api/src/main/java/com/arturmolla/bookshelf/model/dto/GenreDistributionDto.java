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
public class GenreDistributionDto {

    private String genre;
    @Builder.Default
    private long count = 0L;
    @Builder.Default
    private double percentage = 0.0;
}

