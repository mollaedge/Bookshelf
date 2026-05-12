package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.dto.DtoAdminUpdateUserRequest;
import com.arturmolla.bookshelf.model.dto.DtoAdminUserResponse;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.entity.EntityConversation;
import com.arturmolla.bookshelf.model.entity.EntityHomePost;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAdmin {

    private final RepositoryUser repositoryUser;
    private final RepositoryToken repositoryToken;
    private final RepositoryNotification repositoryNotification;
    private final RepositoryUserRelation repositoryUserRelation;
    private final RepositoryPostComment repositoryPostComment;
    private final RepositoryPostLike repositoryPostLike;
    private final RepositoryPostShare repositoryPostShare;
    private final RepositoryHomePost repositoryHomePost;
    private final RepositoryConversation repositoryConversation;
    private final RepositoryMessage repositoryMessage;
    private final RepositoryBookTransactionHistory repositoryBookTransactionHistory;
    private final RepositoryFeedback repositoryFeedback;
    private final RepositoryBook repositoryBook;
    private final RepositoryBookCover repositoryBookCover;
    private final RepositoryBookPdf repositoryBookPdf;
    private final RepositoryUserProfilePic repositoryUserProfilePic;
    private final RepositoryUserWallpaper repositoryUserWallpaper;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    public PageResponse<DtoAdminUserResponse> getAllUsers(Pageable pageable) {
        Page<DtoAdminUserResponse> page = repositoryUser.findAll(pageable).map(this::toDto);
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public DtoAdminUserResponse getUserById(Long id) {
        return repositoryUser.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    @Transactional
    public DtoAdminUserResponse updateUser(Long id, DtoAdminUpdateUserRequest request) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (request.getFirstname() != null) user.setFirstname(request.getFirstname());
        if (request.getLastname() != null) user.setLastname(request.getLastname());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getAccountLocked() != null) user.setAccountLocked(request.getAccountLocked());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());

        return toDto(repositoryUser.save(user));
    }

    // -----------------------------------------------------------------------
    // Activate / Deactivate
    // -----------------------------------------------------------------------

    @Transactional
    public DtoAdminUserResponse activateUser(Long id) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setEnabled(true);
        return toDto(repositoryUser.save(user));
    }

    @Transactional
    public DtoAdminUserResponse deactivateUser(Long id) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setEnabled(false);
        return toDto(repositoryUser.save(user));
    }

    // -----------------------------------------------------------------------
    // Lock / Unlock
    // -----------------------------------------------------------------------

    @Transactional
    public DtoAdminUserResponse lockUser(Long id) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setAccountLocked(true);
        return toDto(repositoryUser.save(user));
    }

    @Transactional
    public DtoAdminUserResponse unlockUser(Long id) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setAccountLocked(false);
        return toDto(repositoryUser.save(user));
    }

    // -----------------------------------------------------------------------
    // Delete  (full cascading wipe)
    // -----------------------------------------------------------------------

    /**
     * Deletes a user and ALL of their associated data in the correct FK order:
     * <ol>
     *   <li>Activation tokens</li>
     *   <li>Notifications (as recipient and as actor)</li>
     *   <li>User relations (friend requests / follows on both sides)</li>
     *   <li>Social activity: comments, likes, shares <em>made by</em> the user</li>
     *   <li>Home posts by the user (incl. social data on those posts from other users
     *       and post attachments via JPA cascade)</li>
     *   <li>Conversations and all their messages</li>
     *   <li>Book borrow history where the user is the borrower</li>
     *   <li>Books owned by the user (feedbacks, transaction history, covers, PDFs)</li>
     *   <li>Profile picture and wallpaper</li>
     *   <li>The user record itself</li>
     * </ol>
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        // 1. Activation / verification tokens
        repositoryToken.deleteAll(repositoryToken.findAllByUser(user));

        // 2. Notifications where this user is the recipient or the actor
        repositoryNotification.deleteAllByRecipientId(id);
        repositoryNotification.deleteAllByActorId(id);

        // 3. Friend / follow relations on both sides
        repositoryUserRelation.deleteAllInvolvingUser(id);

        // 4. Social activity made by this user (across ALL posts, not just their own)
        repositoryPostComment.deleteAllByAuthorId(id);
        repositoryPostLike.deleteAllByUserId(id);
        repositoryPostShare.deleteAllByUserId(id);

        // 5. Posts authored by this user
        //    — first remove other users' social activity on those posts,
        //      then delete the posts (JPA cascade removes post attachments)
        List<EntityHomePost> userPosts = repositoryHomePost.findByAuthorId(id);
        if (!userPosts.isEmpty()) {
            List<Long> postIds = userPosts.stream().map(EntityHomePost::getId).toList();
            repositoryPostComment.deleteAllByPostIdIn(postIds);
            repositoryPostLike.deleteAllByPostIdIn(postIds);
            repositoryPostShare.deleteAllByPostIdIn(postIds);
            repositoryHomePost.deleteAll(userPosts); // cascades → post_attachment
        }

        // 6. Conversations (DMs) and all messages within them
        List<EntityConversation> conversations = repositoryConversation.findAllByUserId(id);
        if (!conversations.isEmpty()) {
            List<Long> convIds = conversations.stream().map(EntityConversation::getId).toList();
            repositoryMessage.deleteAllByConversationIdIn(convIds);
            repositoryConversation.deleteAll(conversations);
        }

        // 7. Book borrow history where this user is the borrower
        repositoryBookTransactionHistory.deleteAllByUserId(id);

        // 8. Books owned by this user
        //    — must clear feedbacks, borrow history for those books, covers and PDFs first
        List<EntityBook> ownedBooks = repositoryBook.findByOwner(user);
        if (!ownedBooks.isEmpty()) {
            List<Long> bookIds = ownedBooks.stream().map(EntityBook::getId).toList();
            repositoryFeedback.deleteAllByBookIdIn(bookIds);
            repositoryBookTransactionHistory.deleteAllByBookIdIn(bookIds);
            bookIds.forEach(bookId -> {
                repositoryBookCover.deleteByBookId(bookId);
                repositoryBookPdf.deleteByBookId(bookId);
            });
            repositoryBook.deleteAll(ownedBooks);
        }

        // 9. Profile picture and wallpaper (stored by raw userId, no JPA FK)
        repositoryUserProfilePic.deleteByUserId(id);
        repositoryUserWallpaper.deleteByUserId(id);

        // 10. The user record (also cleans up the roles join-table automatically)
        repositoryUser.delete(user);
    }

    // -----------------------------------------------------------------------
    // Mapper
    // -----------------------------------------------------------------------

    private DtoAdminUserResponse toDto(User user) {
        return DtoAdminUserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .bio(user.getBio())
                .location(user.getLocation())
                .provider(user.getProvider())
                .dateOfBirth(user.getDateOfBirth())
                .accountLocked(user.isAccountLocked())
                .enabled(user.isEnabled())
                .roles(user.getRoles() == null ? java.util.Collections.emptyList()
                        : user.getRoles().stream()
                          .map(com.arturmolla.bookshelf.model.user.Role::getName)
                          .toList())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }
}
