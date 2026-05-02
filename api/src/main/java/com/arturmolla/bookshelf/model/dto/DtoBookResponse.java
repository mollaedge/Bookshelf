package com.arturmolla.bookshelf.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DtoBookResponse {

    private Long id;
    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private String owner;
    private byte[] cover;
    private String coverUrl;
    private String genre;
    private Double rate;
    private Boolean favourite;
    private Boolean archived;
    private Boolean shareable;
    private Boolean read;
    private Integer pdfPagePointer;
    private Boolean hasPdf;
}
