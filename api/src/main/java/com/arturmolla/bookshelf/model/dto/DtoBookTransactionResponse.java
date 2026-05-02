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
public class DtoBookTransactionResponse {

    private Long id;
    private String title;
    private String authorName;
    private String isbn;
    private Double rate;
    private byte[] cover;
    private String coverUrl;
    private Long ownerId;
    private Long requesterId;
    private String ownerName;
    private String requesterName;
    // Borrow-return flow
    private Boolean returned;
    private Boolean returnApproved;
    // Borrow-request flow
    private Boolean requested;
    private Boolean requestApproved;
}

