package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoBookRequest;
import com.arturmolla.bookshelf.model.dto.DtoBookResponse;
import com.arturmolla.bookshelf.model.dto.DtoBorrowedBooksResponse;
import com.arturmolla.bookshelf.model.dto.DtoRequestedBooksResponse;
import com.arturmolla.bookshelf.service.ServiceBook;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("books")
@Tag(name = "Book")
public class ControllerBook {

    private final ServiceBook serviceBook;

    @PostMapping
    public ResponseEntity<Long> saveBook(@Valid @RequestBody DtoBookRequest request,
                                         Authentication connectedUser) {
        return ResponseEntity.ok(serviceBook.saveBook(request, connectedUser));
    }

    @GetMapping("/{book-id}")
    public ResponseEntity<DtoBookResponse> findBookById(@PathVariable("book-id") Long bookId) {
        return ResponseEntity.ok(serviceBook.findBookById(bookId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DtoBookResponse>> getAllBooksPaged(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.getAllBooksPaged(page, size, connectedUser));
    }

    @GetMapping("/owner")
    public ResponseEntity<PageResponse<DtoBookResponse>> getAllBooksByOwner(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.getAllBooksByOwner(page, size, connectedUser));
    }

    @GetMapping("/requested")
    public ResponseEntity<PageResponse<DtoRequestedBooksResponse>> getAllRequestedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.getAllRequestedBooks(page, size, connectedUser));
    }

    @GetMapping("/borrowed")
    public ResponseEntity<PageResponse<DtoBorrowedBooksResponse>> getAllBorrowedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.getAllBorrowedBooks(page, size, connectedUser));
    }

    @GetMapping("/returned")
    public ResponseEntity<PageResponse<DtoBorrowedBooksResponse>> getAllReturnedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.getAllReturnedBooks(page, size, connectedUser));
    }

    @PatchMapping("/shareable/{book-id}")
    public ResponseEntity<Long> updateShareableStatus(
            @PathVariable("book-id") Long bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.updateShareableStatus(bookId, connectedUser));
    }

    @PatchMapping("/archive/{book-id}")
    public ResponseEntity<Long> updateArchiveStatus(
            @PathVariable("book-id") Long bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.updateArchiveStatus(bookId, connectedUser));
    }

    @PostMapping("/borrow/{book-id}")
    public ResponseEntity<Long> borrowBook(
            @PathVariable("book-id") Long bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.borrowBook(bookId, connectedUser));
    }

    @PatchMapping("/borrow/return/{book-id}")
    public ResponseEntity<Long> returnBorrowedBook(
            @PathVariable("book-id") Long bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.returnBorrowedBook(bookId, connectedUser));
    }

    @PatchMapping("/borrow/return/approve/{book-id}")
    public ResponseEntity<Long> approveReturnBorrowedBook(
            @PathVariable("book-id") Long bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceBook.approveReturnBorrowedBook(bookId, connectedUser));
    }

    @PostMapping(value = "/cover/{book-id}", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadBookCoverImage(
            @PathVariable("book-id") Long bookId,
            @Parameter()
            @RequestPart("file") MultipartFile file,
            Authentication connectedUser
    ) {
        serviceBook.uploadBookCoverImage(file, connectedUser, bookId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{book-id}")
    public ResponseEntity<Void> deleteBookById(@PathVariable("book-id") Long bookId, Authentication connectedUser) {
        serviceBook.deleteBookById(bookId, connectedUser);
        return ResponseEntity.noContent().build();
    }
}
