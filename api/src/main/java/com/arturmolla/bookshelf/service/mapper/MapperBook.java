package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.DtoBookRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookResponse;
import com.arturmolla.bookshelf.model.dto.DtoBorrowedBooksResponse;
import com.arturmolla.bookshelf.model.dto.DtoRequestedBooksResponse;
import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.entity.EntityBookTransactionHistory;
import com.arturmolla.bookshelf.service.utils.UtilsFile;
import org.springframework.stereotype.Component;

@Component
public class MapperBook {

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
                .cover(UtilsFile.readFileFromLocation(entity.getCover()))
                .coverUrl(entity.getCoverUrl())
                .genre(entity.getGenre())
                .build();
    }

    public DtoBorrowedBooksResponse toDtoBorrowedBookResponse(EntityBookTransactionHistory history) {
        return DtoBorrowedBooksResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .returned(history.getReturned())
                .returnApproved(history.getReturnApproved())
                .build();
    }

    public DtoRequestedBooksResponse toDtoRequestedBookResponse(EntityBookTransactionHistory history) {
        return DtoRequestedBooksResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .requested(history.getRequested())
                .requestApproved(history.getRequestApproved())
                .requesterName(history.getUser().getFullName())
                .cover(UtilsFile.readFileFromLocation(history.getBook().getCover()))
                .coverUrl(history.getBook().getCoverUrl())
                .build();
    }
}
