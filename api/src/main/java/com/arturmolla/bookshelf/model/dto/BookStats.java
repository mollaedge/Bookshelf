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
public class BookStats {

    @Builder.Default
    private long booksOwned = 0L;
    @Builder.Default
    private long booksRead = 0L;
    @Builder.Default
    private long currentlyBorrowed = 0L;
    @Builder.Default
    private long returnedBooks = 0L;
    @Builder.Default
    private long pendingRequests = 0L;
}

