package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardResponse extends DtoUserBase {

    private LocalDate joinDate;
    private BookStats stats;
    private List<ReadingActivityDto> readingActivity;
    private List<GenreDistributionDto> genreDistribution;
    private int readingStreak;
}
