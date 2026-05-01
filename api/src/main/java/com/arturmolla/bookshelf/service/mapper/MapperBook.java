package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.DtoBookRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookResponse;
import com.arturmolla.bookshelf.model.dto.DtoBookUpdateRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookTransactionResponse;
import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.entity.EntityBookTransactionHistory;
import com.arturmolla.bookshelf.service.ServiceFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MapperBook {

    private final ServiceFileStorage serviceFileStorage;

    public EntityBook toEntityBook(DtoBookRequest request) {
        return EntityBook.builder()
                .title(request.title())
                .authorName(request.authorName())
                .isbn(request.isbn())
                .synopsis(request.synopsis())
                .genre(request.genre())
                .cover(request.cover())
                .coverUrl(request.coverUrl())
                .pageBookmark(request.pageBookmark())
                .favourite(request.favourite())
                .archived(request.archived())
                .shareable(request.shareable())
                .read(request.read())
                .build();
    }

    public void updateEntityFromRequest(EntityBook book, DtoBookUpdateRequest request) {
        if (request.title() != null) book.setTitle(request.title());
        if (request.authorName() != null) book.setAuthorName(request.authorName());
        if (request.isbn() != null) book.setIsbn(request.isbn());
        if (request.synopsis() != null) book.setSynopsis(request.synopsis());
        if (request.genre() != null) book.setGenre(request.genre());
        if (request.coverUrl() != null) book.setCoverUrl(request.coverUrl());
        if (request.favourite() != null) book.setFavourite(request.favourite());
        if (request.archived() != null) book.setArchived(request.archived());
        if (request.shareable() != null) book.setShareable(request.shareable());
        if (request.read() != null) book.setRead(request.read());
    }

    public DtoBookResponse toDtoBookResponse(EntityBook entity) {
        return DtoBookResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .authorName(entity.getAuthorName())
                .isbn(entity.getIsbn())
                .synopsis(entity.getSynopsis())
                .rate(entity.getRate())
                .favourite(entity.getFavourite())
                .archived(entity.getArchived())
                .shareable(entity.getShareable())
                .read(entity.getRead())
                .owner(entity.getOwner().getFullName())
                .cover(serviceFileStorage.loadFile(entity.getId()))
                .coverUrl(entity.getCoverUrl())
                .genre(entity.getGenre())
                .build();
    }

    public DtoBookTransactionResponse toBookTransactionResponse(EntityBookTransactionHistory history) {
        return DtoBookTransactionResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .returned(history.getReturned())
                .returnApproved(history.getReturnApproved())
                .requested(history.getRequested())
                .requestApproved(history.getRequestApproved())
                .requesterName(history.getUser().getFullName())
                .ownerName(history.getBook().getOwner().getFullName())
                .cover(serviceFileStorage.loadFile(history.getBook().getId()))
                .coverUrl(history.getBook().getCoverUrl())
                .ownerId(history.getBook().getOwner().getId())
                .requesterId(history.getUser().getId())
                .build();
    }
}
