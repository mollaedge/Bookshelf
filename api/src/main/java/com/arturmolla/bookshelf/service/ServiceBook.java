package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoBookRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookResponse;
import com.arturmolla.bookshelf.model.dto.DtoBookUpdateRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookTransactionResponse;
import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.entity.EntityBookTransactionHistory;
import com.arturmolla.bookshelf.model.enums.NotificationType;
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
    private final ServiceNotification serviceNotification;

    public Long saveBook(DtoBookRequest request, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityBook book = mapperBook.toEntityBook(request);
        book.setOwner(user);
        return repositoryBook.save(book).getId();
    }

    public DtoBookResponse updateBook(Long bookId, DtoBookUpdateRequest request, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityBook book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        mapperBook.updateEntityFromRequest(book, request);
        return mapperBook.toDtoBookResponse(repositoryBook.save(book));
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

    public PageResponse<DtoBookResponse> getAllBooksPaged(int page, int size, String query, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        var spec = SpecificationBook.notOwnedBy(user.getId())
                .and(SpecificationBook.notArchived())
                .and(SpecificationBook.isShareable())
                .and(SpecificationBook.matchesQuery(query));
        Page<EntityBook> books = repositoryBook.findAll(spec, pageable);
        return mapPageToCustomWrapper(books);
    }

    public PageResponse<DtoBookResponse> getAllBooksByOwner(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBook> books = repositoryBook.findAll(SpecificationBook.withOwnerId(user.getId()), pageable);
        return mapPageToCustomWrapper(books);
    }

    public PageResponse<DtoBookTransactionResponse> getAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> allBorrowedBooks = repositoryBookTransactionHistory.findAllBorrowedBooks(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistories(allBorrowedBooks);
    }

    public PageResponse<DtoBookTransactionResponse> getAllReturnedBooks(int page, int size, Authentication connectedUser) {
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
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
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
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
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
                .requested(true)
                .requestApproved(false)
                .returned(false)
                .returnApproved(false)
                .build();
        Long historyId = repositoryBookTransactionHistory.save(bookTransactionHistory).getId();

        // Notify book owner about the borrow request
        serviceNotification.notify(
                book.getOwner(), user,
                NotificationType.BOOK_BORROW_REQUESTED,
                user.getFullName() + " wants to borrow your book",
                "Borrow request for \"" + book.getTitle() + "\".",
                bookId, "BOOK"
        );

        return historyId;
    }

    public Long returnBorrowedBook(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        if (book.getArchived() || !book.getShareable()) {
            throw new OperationNotPermittedException("The requested book can not be borrowed (archived/non borrowable)");
        }
        var user = (User) connectedUser.getPrincipal();
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot return your own book!");
        }
        EntityBookTransactionHistory bookTransactionHistory = repositoryBookTransactionHistory.findByBookIdAndUserId(
                bookId,
                user.getId()
        ).orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book!"));
        bookTransactionHistory.setReturned(true);
        Long historyId = repositoryBookTransactionHistory.save(bookTransactionHistory).getId();

        // Notify book owner that the book has been returned
        serviceNotification.notify(
                book.getOwner(), user,
                NotificationType.BOOK_RETURNED,
                user.getFullName() + " returned your book",
                "\"" + book.getTitle() + "\" has been returned. Please approve.",
                bookId, "BOOK"
        );

        return historyId;
    }

    public Long approveBorrowRequest(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You do not own this book!");
        }
        EntityBookTransactionHistory bookTransactionHistory = repositoryBookTransactionHistory
                .findPendingRequestByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("No pending borrow request found for this book!"));
        bookTransactionHistory.setRequestApproved(true);
        Long historyId = repositoryBookTransactionHistory.save(bookTransactionHistory).getId();

        // Notify borrower that their request was approved
        serviceNotification.notify(
                bookTransactionHistory.getUser(), user,
                NotificationType.BOOK_BORROW_APPROVED,
                "Your borrow request was approved",
                "You can now read \"" + book.getTitle() + "\".",
                bookId, "BOOK"
        );

        return historyId;
    }

    public Long rejectBorrowRequest(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You do not own this book!");
        }
        EntityBookTransactionHistory bookTransactionHistory = repositoryBookTransactionHistory
                .findPendingRequestByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("No pending borrow request found for this book!"));
        Long historyId = bookTransactionHistory.getId();
        repositoryBookTransactionHistory.delete(bookTransactionHistory);

        // Notify the requester that their borrow request was rejected
        serviceNotification.notify(
                bookTransactionHistory.getUser(), user,
                NotificationType.BOOK_BORROW_REJECTED,
                "Your borrow request was rejected",
                "Your request to borrow \"" + book.getTitle() + "\" was declined by the owner.",
                bookId, "BOOK"
        );

        return historyId;
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
        Long historyId = repositoryBookTransactionHistory.save(bookTransactionHistory).getId();

        // Notify borrower that return was approved
        serviceNotification.notify(
                bookTransactionHistory.getUser(), user,
                NotificationType.BOOK_RETURN_APPROVED,
                "Your book return was confirmed",
                "The return of \"" + book.getTitle() + "\" has been confirmed.",
                bookId, "BOOK"
        );

        return historyId;
    }

    public PageResponse<DtoBookTransactionResponse> getAllRequestedBooks(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> allRequestedBooks = repositoryBookTransactionHistory.findAllPendingRequests(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistories(allRequestedBooks);
    }

    public PageResponse<DtoBookTransactionResponse> getAllRequestsByMe(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> requests = repositoryBookTransactionHistory.findAllRequestsByMe(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistories(requests);
    }

    public PageResponse<DtoBookTransactionResponse> getAllRequestsFromMe(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityBookTransactionHistory> requests = repositoryBookTransactionHistory.findAllRequestsFromMe(
                pageable, user.getId()
        );
        return mapPageToCustomWrapperHistories(requests);
    }

    public void uploadBookCoverImage(MultipartFile file, Authentication connectedUser, Long bookId) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        serviceFileStorage.saveFile(file, bookId);
        // Cover is now stored in book_cover table; clear any stale local-path reference
        book.setCover(null);
        repositoryBook.save(book);
    }

    public byte[] getBookCoverImage(Long bookId) {
        repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        return serviceFileStorage.loadFile(bookId);
    }

    public void uploadBookPdf(MultipartFile file, Authentication connectedUser, Long bookId) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        serviceFileStorage.savePdf(file, bookId);
    }

    public byte[] getBookPdf(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can not access this PDF!");
        }
        return serviceFileStorage.loadPdf(bookId);
    }

    public DtoBookResponse updatePdfPagePointer(Long bookId, Integer page, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You can not perform this action!");
        }
        book.setPdfPagePointer(page);
        return mapperBook.toDtoBookResponse(repositoryBook.save(book));
    }

    public void deleteBookById(Long bookId, Authentication connectedUser) {
        var book = repositoryBook.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(BOOK_NOT_FOUND + bookId));
        var user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getCreatedBy(), user.getId())) {
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

    private PageResponse<DtoBookTransactionResponse> mapPageToCustomWrapperHistories(Page<EntityBookTransactionHistory> histories) {
        List<DtoBookTransactionResponse> responses = histories.stream()
                .map(mapperBook::toBookTransactionResponse)
                .toList();
        return new PageResponse<>(
                responses,
                histories.getNumber(),
                histories.getSize(),
                histories.getTotalElements(),
                histories.getTotalPages(),
                histories.isFirst(),
                histories.isLast()
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
}
