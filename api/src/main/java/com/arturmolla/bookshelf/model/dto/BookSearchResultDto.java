package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.BookSearchSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookSearchResultDto {

    private String title;
    private List<String> authors;
    private String isbn;
    private String synopsis;
    private String genre;
    private String coverUrl;
    private String publisher;
    private String publishedDate;
    private Integer pageCount;
    private String externalId;
    private BookSearchSource source;
    private String previewLink;
}

