package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoBookRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookResponse;
import com.arturmolla.bookshelf.model.dto.DtoBorrowedBooksResponse;
import com.arturmolla.bookshelf.model.dto.DtoRequestedBooksResponse;
import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.entity.EntityBookTransactionHistory;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryBook;
import com.arturmolla.bookshelf.repository.RepositoryBookTransactionHistory;
import com.arturmolla.bookshelf.repository.specification.SpecificationBook;
import com.arturmolla.bookshelf.service.mapper.MapperBook;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceBook {

    public static final String BOOK_NOT_FOUND = "Book was not found with id: ";
    private final RepositoryBook repositoryBook;
    private final RepositoryBookTransactionHistory repositoryBookTransactionHistory;
    private final ServiceFileStorage serviceFileStorage;
    private final MapperBook mapperBook;

    public Long saveBook(DtoBookRequest request, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityBook book = mapperBook.toEntityBook(request);
        book.setOwner(user);
        return repositoryBook.save(book).getId();
    }

    public DtoBookResponse findBookById(Long bookId) {
        return repositoryBook.findById(bookId)
                .map(mapperBook::toDtoBookResponse)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
    }

    public PageResponse<DtoBookResponse> getAllMyBooksPaged(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBook> books = repositoryBook.findAllUsersBooks(pageable, user.getId());
        return mapPageToCustomWrapper(books);
    }

    public PageResponse<DtoBookResponse> getAllBooksPaged(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBook> books = repositoryBook.findAllBooks(pageable, user.getId());
        return mapPageToCustomWrapper(books);
    }

    public PageResponse<DtoBookResponse> getAllBooksByOwner(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBook> books = repositoryBook.findAll(SpecificationBook.withOwnerId(user.getId()), pageable);
        return mapPageToCustomWrapper(books);
    }

    public PageResponse<DtoBorrowedBooksResponse> getAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> allBorrowedBooks = repositoryBookTransactionHistory.findAllBorrowedBooks(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistories(allBorrowedBooks);
    }

    public PageResponse<DtoBorrowedBooksResponse> getAllReturnedBooks(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> allBorrowedBooks = repositoryBookTransactionHistory.findAllReturnedBooks(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistories(allBorrowedBooks);
    }

    public Long updateShareableStatus(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        book.setShareable(!book.getShareable());
        repositoryBook.save(book);
        return book.getId();
    }

    public Long updateArchiveStatus(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        book.setArchived(!book.getArchived());
        repositoryBook.save(book);
        return book.getId();
    }

    public Long borrowBook(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        if (book.getArchived() || !book.getShareable()) {
            throw new OperationNotPermittedException("The requested book can not be borrowed (archived/non borrowable)");
        }
        var user = (User) connectedUser.getPrincipal();
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You own this book!");
        }
        final boolean isAlreadyBorrowed = repositoryBookTransactionHistory.isAlreadyBorrowedByUser(bookId, user.getId());
        if (isAlreadyBorrowed) {
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }
        EntityBookTransactionHistory bookTransactionHistory = EntityBookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        return repositoryBookTransactionHistory.save(bookTransactionHistory).getId();
    }

    public Long returnBorrowedBook(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        if (book.getArchived() || !book.getShareable()) {
            throw new OperationNotPermittedException("The requested book can not be borrowed (archived/non borrowable)");
        }
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You own this book!");
        }
        EntityBookTransactionHistory bookTransactionHistory = repositoryBookTransactionHistory.findByBookIdAndUserId(
                bookId,
                user.getId()
        ).orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book!"));
        bookTransactionHistory.setReturned(true);
        return repositoryBookTransactionHistory.save(bookTransactionHistory).getId();
    }


    public Long approveReturnBorrowedBook(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        if (book.getArchived() || !book.getShareable()) {
            throw new OperationNotPermittedException("The requested book can not be borrowed (archived/nonchargeable)");
        }
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You own this book!");
        }
        EntityBookTransactionHistory bookTransactionHistory = repositoryBookTransactionHistory.findByBookIdAndOwnerId(
                bookId,
                user.getId()
        ).orElseThrow(() -> new OperationNotPermittedException("Book is not returned yet!"));
        bookTransactionHistory.setReturnApproved(true);
        return repositoryBookTransactionHistory.save(bookTransactionHistory).getId();
    }

    public PageResponse<DtoRequestedBooksResponse> getAllRequestedBooks(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> allRequestedBooks = repositoryBookTransactionHistory.findAllReturnedBooks(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistoriesRequested(allRequestedBooks);
    }

    public void uploadBookCoverImage(MultipartFile file, Authentication connectedUser, Long bookId) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        var cover = serviceFileStorage.saveFile(file, user.getId());
        book.setCover(cover);
        repositoryBook.save(book);
    }

    public void deleteBookById(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        repositoryBook.deleteById(bookId);
    }

    public PageResponse<DtoBookResponse> getRecentBooks(int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(0, Math.min(size, 10));
        return mapPageToCustomWrapper(repositoryBook.findByOwnerOrderByCreatedDateDesc(user, pageable));
    }

    // HELPER METHODS

    private PageResponse<DtoBorrowedBooksResponse> mapPageToCustomWrapperHistories(Page<EntityBookTransactionHistory> allBorrowedBooks) {
        List<DtoBorrowedBooksResponse> borrowedBooksResponses = allBorrowedBooks.stream()
                .map(mapperBook::toDtoBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                borrowedBooksResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    private PageResponse<DtoBookResponse> mapPageToCustomWrapper(Page<EntityBook> books) {
        List<DtoBookResponse> bookResponses = books.stream()
                .map(mapperBook::toDtoBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    private PageResponse<DtoRequestedBooksResponse> mapPageToCustomWrapperHistoriesRequested(Page<EntityBookTransactionHistory> allRequestedBooks) {
        List<DtoRequestedBooksResponse> requestedBooksResponses = allRequestedBooks.stream()
                .map(mapperBook::toDtoRequestedBookResponse)
                .toList();
        return new PageResponse<>(
                requestedBooksResponses,
                allRequestedBooks.getNumber(),
                allRequestedBooks.getSize(),
                allRequestedBooks.getTotalElements(),
                allRequestedBooks.getTotalPages(),
                allRequestedBooks.isFirst(),
                allRequestedBooks.isLast()
        );
    }
}
