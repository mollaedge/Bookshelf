package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardResponse {

    private Long id;
    private String firstname;
    private String lastname;
    private String fullName;
    private String email;
    private LocalDate joinDate;
    private String bio;
    private String location;
    private String provider;

    private BookStats stats;
    private List<ReadingActivityDto> readingActivity;
    private List<GenreDistributionDto> genreDistribution;
    @Builder.Default
    private int readingStreak = 0;
}

