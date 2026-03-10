package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.BookSearchResultDto;
import com.arturmolla.bookshelf.model.enums.BookSearchSource;
import com.arturmolla.bookshelf.service.ServiceBookSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("books/search")
@Tag(name = "Book Search", description = "Search books from external sources (Google Books, Open Library)")
public class ControllerBookSearch {

    private final ServiceBookSearch serviceBookSearch;

    @GetMapping
    @Operation(
            summary = "Search books from an external source",
            description = "Searches for books using the provided query string against the selected source. " +
                    "Available sources: GOOGLE_BOOKS, OPEN_LIBRARY. No authentication required."
    )
    public ResponseEntity<PageResponse<BookSearchResultDto>> search(
            @Parameter(description = "Search query (title, author, ISBN, keywords)", required = true)
            @RequestParam String query,

            @Parameter(description = "Source to search from: GOOGLE_BOOKS or OPEN_LIBRARY", required = true)
            @RequestParam BookSearchSource source,

            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (default 15, max 40 for Google Books / 100 for Open Library)")
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(serviceBookSearch.search(query, source, page, size));
    }
}
