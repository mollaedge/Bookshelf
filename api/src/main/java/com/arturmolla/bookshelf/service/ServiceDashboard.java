package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.dto.BookStats;
import com.arturmolla.bookshelf.model.dto.GenreDistributionDto;
import com.arturmolla.bookshelf.model.dto.ReadingActivityDto;
import com.arturmolla.bookshelf.model.dto.UserDashboardResponse;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryBook;
import com.arturmolla.bookshelf.repository.RepositoryBookTransactionHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServiceDashboard {

    private final RepositoryBook repositoryBook;
    private final RepositoryBookTransactionHistory repositoryTransaction;

    public UserDashboardResponse getDashboard(User user) {

        // --- Book Stats ---
        long booksOwned = repositoryBook.countByOwner(user);
        long booksRead = repositoryBook.countByOwnerAndReadTrue(user);
        long currentlyBorrowed = repositoryTransaction.countByReturnedFalseAndUserId(user.getId());
        long pendingRequests = repositoryTransaction.countPendingReturnsByOwnerId(user.getId());

        BookStats stats = BookStats.builder()
                .booksOwned(booksOwned)
                .booksRead(booksRead)
                .currentlyBorrowed(currentlyBorrowed)
                .returnedBooks(booksRead)
                .pendingRequests(pendingRequests)
                .build();

        // --- Reading Activity (last 12 calendar months) ---
        LocalDateTime twelveMonthsAgo = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        List<Object[]> rawActivity = repositoryTransaction.findMonthlyReadingActivity(
                user.getId(), twelveMonthsAgo);

        Map<String, Long> activityMap = new HashMap<>();
        for (Object[] row : rawActivity) {
            String yearMonth = (String) row[0];
            long count = ((Number) row[1]).longValue();
            activityMap.put(yearMonth, count);
        }

        // Fill all 12 months, defaulting to 0
        List<ReadingActivityDto> readingActivity = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 11; i >= 0; i--) {
            String label = LocalDate.now().minusMonths(i).format(formatter);
            readingActivity.add(ReadingActivityDto.builder()
                    .month(label)
                    .booksRead(activityMap.getOrDefault(label, 0L))
                    .build());
        }

        // --- Genre Distribution ---
        List<Object[]> rawGenres = repositoryBook.findGenreDistributionByUser(user.getId());
        long totalRead = rawGenres.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<GenreDistributionDto> genreDistribution = new ArrayList<>();
        for (Object[] row : rawGenres) {
            String genre = row[0] != null ? (String) row[0] : "Other";
            long count = ((Number) row[1]).longValue();
            double percentage = totalRead > 0
                    ? Math.round((count * 100.0 / totalRead) * 100.0) / 100.0
                    : 0.0;
            genreDistribution.add(GenreDistributionDto.builder()
                    .genre(genre)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // --- Assemble response ---
        return UserDashboardResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .joinDate(user.getCreatedDate() != null ? user.getCreatedDate().toLocalDate() : null)
                .bio(user.getBio())
                .location(user.getLocation())
                .provider(user.getProvider())
                .stats(stats)
                .readingActivity(readingActivity)
                .genreDistribution(genreDistribution)
                .readingStreak(0) // stub — tracking not yet implemented
                .build();
    }
}

