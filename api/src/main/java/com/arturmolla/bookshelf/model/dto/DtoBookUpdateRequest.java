package com.arturmolla.bookshelf.model.dto;

public record DtoBookUpdateRequest(
        String title,
        String authorName,
        String isbn,
        String synopsis,
        String genre,
        String coverUrl,
        Boolean favourite,
        Boolean archived,
        Boolean shareable,
        Boolean read,
        Integer pdfPagePointer
) {
}
