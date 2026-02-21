package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.AppFeedbackDto;
import com.arturmolla.bookshelf.model.dto.AppFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.CommentDto;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.model.user.Role;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryAppFeedback;
import com.arturmolla.bookshelf.service.mapper.MapperAppFeedback;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppFeedbackServiceTest {

    @Mock
    private RepositoryAppFeedback repositoryAppFeedback;

    @Mock
    private MapperAppFeedback mapperAppFeedback;

    @InjectMocks
    private AppFeedbackService appFeedbackService;

    @Mock
    private Authentication userAuth;

    @Mock
    private Authentication adminAuth;

    private User regularUser;
    private User adminUser;
    private EntityAppFeedback feedback;
    private AppFeedbackDto feedbackDto;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().name("ROLE_USER").build();
        Role adminRole = Role.builder().name("ROLE_ADMIN").build();

        regularUser = User.builder()
                .id(1L)
                .firstname("John")
                .lastname("Doe")
                .email("john@example.com")
                .roles(List.of(userRole))
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(2L)
                .firstname("Admin")
                .lastname("User")
                .email("admin@example.com")
                .roles(List.of(userRole, adminRole))
                .enabled(true)
                .build();

        feedback = EntityAppFeedback.builder()
                .title("Test Bug")
                .description("Something is broken")
                .status(AppFeedbackStatus.NEW)
                .build();
        // Simulate persistence: set id
        setField(feedback, "id", 10L);
        setField(feedback, "createdBy", 1L);

        feedbackDto = AppFeedbackDto.builder()
                .id(10L)
                .title("Test Bug")
                .description("Something is broken")
                .status(AppFeedbackStatus.NEW)
                .upvoteCount(0)
                .ownFeedback(true)
                .build();

        // Configure userAuth
        when(userAuth.getPrincipal()).thenReturn(regularUser);
        when(userAuth.getAuthorities()).thenAnswer(inv ->
                regularUser.getAuthorities().stream().toList());

        // Configure adminAuth
        when(adminAuth.getPrincipal()).thenReturn(adminUser);
        when(adminAuth.getAuthorities()).thenAnswer(inv ->
                adminUser.getAuthorities().stream().toList());
    }

    /**
     * Reflective helper to set private/protected fields (simulates JPA-set ID)
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> cls = target.getClass();
            while (cls != null) {
                try {
                    var field = cls.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SAVE
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("saves feedback and returns DTO for the current user")
        void save_success() {
            AppFeedbackRequest request = new AppFeedbackRequest("Test Bug", "Something is broken");
            when(mapperAppFeedback.toEntity(request)).thenReturn(feedback);
            when(repositoryAppFeedback.save(feedback)).thenReturn(feedback);
            when(mapperAppFeedback.toDto(feedback, 1L)).thenReturn(feedbackDto);

            AppFeedbackDto result = appFeedbackService.save(request, userAuth);

            assertThat(result).isEqualTo(feedbackDto);
            verify(repositoryAppFeedback).save(feedback);
            verify(mapperAppFeedback).toDto(feedback, 1L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("returns paginated feedbacks for the authenticated user")
        void getAll_returnsPaginatedFeedbacks() {
            var page = new PageImpl<>(List.of(feedback));
            when(repositoryAppFeedback.findAll(any(Pageable.class))).thenReturn(page);
            when(mapperAppFeedback.toDto(eq(feedback), eq(1L))).thenReturn(feedbackDto);

            PageResponse<AppFeedbackDto> result = appFeedbackService.getAll(0, 15, userAuth);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(feedbackDto);
            assertThat(result.getTotalElement()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns empty page when there are no feedbacks")
        void getAll_returnsEmptyPage_whenNoFeedbacks() {
            when(repositoryAppFeedback.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            PageResponse<AppFeedbackDto> result = appFeedbackService.getAll(0, 15, userAuth);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElement()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("returns feedback DTO when found")
        void getById_returnsFeedback_whenFound() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(mapperAppFeedback.toDto(feedback, 1L)).thenReturn(feedbackDto);

            AppFeedbackDto result = appFeedbackService.getById(10L, userAuth);

            assertThat(result).isEqualTo(feedbackDto);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when feedback not found")
        void getById_throws_whenNotFound() {
            when(repositoryAppFeedback.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appFeedbackService.getById(999L, userAuth))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPVOTE (TOGGLE)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("upvote()")
    class UpvoteTests {

        @Test
        @DisplayName("adds upvote when user has not upvoted yet")
        void upvote_addsUpvote_whenNotYetUpvoted() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(1L))).thenReturn(feedbackDto);

            appFeedbackService.upvote(10L, userAuth);

            assertThat(feedback.getUpvotedBy()).contains(1L);
        }

        @Test
        @DisplayName("removes upvote when user already upvoted (toggle)")
        void upvote_removesUpvote_whenAlreadyUpvoted() {
            feedback.getUpvotedBy().add(1L);
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(1L))).thenReturn(feedbackDto);

            appFeedbackService.upvote(10L, userAuth);

            assertThat(feedback.getUpvotedBy()).doesNotContain(1L);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when feedback not found")
        void upvote_throws_whenNotFound() {
            when(repositoryAppFeedback.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appFeedbackService.upvote(999L, userAuth))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("multiple users can upvote the same feedback")
        void upvote_multipleUsers() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), any())).thenReturn(feedbackDto);

            appFeedbackService.upvote(10L, userAuth);  // user 1 upvotes

            // Simulate user 2
            Authentication user2Auth = mock(Authentication.class);
            User user2 = User.builder().id(2L).firstname("Jane").lastname("Doe")
                    .email("jane@example.com").roles(List.of()).build();
            when(user2Auth.getPrincipal()).thenReturn(user2);
            appFeedbackService.upvote(10L, user2Auth);  // user 2 upvotes

            assertThat(feedback.getUpvotedBy()).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // EDIT
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("edit()")
    class EditTests {

        @Test
        @DisplayName("owner can edit their own feedback")
        void edit_success_forOwner() {
            AppFeedbackRequest request = new AppFeedbackRequest("Updated Title", "Updated Description");
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(1L))).thenReturn(feedbackDto);

            appFeedbackService.edit(10L, request, userAuth);

            assertThat(feedback.getTitle()).isEqualTo("Updated Title");
            assertThat(feedback.getDescription()).isEqualTo("Updated Description");
            verify(repositoryAppFeedback).save(feedback);
        }

        @Test
        @DisplayName("admin can edit any feedback")
        void edit_success_forAdmin() {
            AppFeedbackRequest request = new AppFeedbackRequest("Admin Edit", "Admin changed this");
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(2L))).thenReturn(feedbackDto);

            appFeedbackService.edit(10L, request, adminAuth);

            assertThat(feedback.getTitle()).isEqualTo("Admin Edit");
            verify(repositoryAppFeedback).save(feedback);
        }

        @Test
        @DisplayName("non-owner regular user cannot edit others' feedback")
        void edit_throws_forNonOwner() {
            User otherUser = User.builder().id(99L).firstname("Other").lastname("User")
                    .email("other@example.com").roles(List.of()).build();
            Authentication otherAuth = mock(Authentication.class);
            when(otherAuth.getPrincipal()).thenReturn(otherUser);
            Collection<GrantedAuthority> userAuthorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            doReturn(userAuthorities).when(otherAuth).getAuthorities();
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));

            AppFeedbackRequest request = new AppFeedbackRequest("Hack", "Should fail");

            assertThatThrownBy(() -> appFeedbackService.edit(10L, request, otherAuth))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("own feedback");
        }

        @Test
        @DisplayName("throws EntityNotFoundException when feedback not found")
        void edit_throws_whenNotFound() {
            when(repositoryAppFeedback.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appFeedbackService.edit(999L,
                    new AppFeedbackRequest("T", "D"), userAuth))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ADD COMMENT
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addComment()")
    class AddCommentTests {

        @Test
        @DisplayName("adds comment to feedback")
        void addComment_success() {
            CommentDto commentDto = CommentDto.builder().message("Great feedback!").build();
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(1L))).thenReturn(feedbackDto);

            appFeedbackService.addComment(10L, commentDto, userAuth);

            assertThat(feedback.getComments()).hasSize(1);
            assertThat(feedback.getComments().get(0).getMessage()).isEqualTo("Great feedback!");
            assertThat(feedback.getComments().get(0).getAuthorId()).isEqualTo(1L);
            assertThat(feedback.getComments().get(0).getAuthorName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("multiple comments can be added")
        void addComment_multipleComments() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), any())).thenReturn(feedbackDto);

            appFeedbackService.addComment(10L, CommentDto.builder().message("First").build(), userAuth);
            appFeedbackService.addComment(10L, CommentDto.builder().message("Second").build(), userAuth);

            assertThat(feedback.getComments()).hasSize(2);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when feedback not found")
        void addComment_throws_whenNotFound() {
            when(repositoryAppFeedback.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appFeedbackService.addComment(999L,
                    CommentDto.builder().message("test").build(), userAuth))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("owner can delete their own feedback")
        void delete_success_forOwner() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));

            appFeedbackService.delete(10L, userAuth);

            verify(repositoryAppFeedback).delete(feedback);
        }

        @Test
        @DisplayName("admin can delete any feedback")
        void delete_success_forAdmin() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));

            appFeedbackService.delete(10L, adminAuth);

            verify(repositoryAppFeedback).delete(feedback);
        }

        @Test
        @DisplayName("non-owner regular user cannot delete others' feedback")
        void delete_throws_forNonOwner() {
            User otherUser = User.builder().id(99L).firstname("Other").lastname("User")
                    .email("other@example.com").roles(List.of()).build();
            Authentication otherAuth = mock(Authentication.class);
            when(otherAuth.getPrincipal()).thenReturn(otherUser);
            Collection<GrantedAuthority> userAuthorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            doReturn(userAuthorities).when(otherAuth).getAuthorities();
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));

            assertThatThrownBy(() -> appFeedbackService.delete(10L, otherAuth))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("own feedback");

            verify(repositoryAppFeedback, never()).delete(any());
        }

        @Test
        @DisplayName("throws EntityNotFoundException when feedback not found")
        void delete_throws_whenNotFound() {
            when(repositoryAppFeedback.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appFeedbackService.delete(999L, userAuth))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CHANGE STATUS (Admin only)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatusTests {

        @Test
        @DisplayName("admin can change status to IN_PROGRESS")
        void changeStatus_success_toInProgress() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(2L))).thenReturn(feedbackDto);

            appFeedbackService.changeStatus(10L, AppFeedbackStatus.IN_PROGRESS, adminAuth);

            assertThat(feedback.getStatus()).isEqualTo(AppFeedbackStatus.IN_PROGRESS);
            verify(repositoryAppFeedback).save(feedback);
        }

        @Test
        @DisplayName("admin can change status to RESOLVED")
        void changeStatus_success_toResolved() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(2L))).thenReturn(feedbackDto);

            appFeedbackService.changeStatus(10L, AppFeedbackStatus.RESOLVED, adminAuth);

            assertThat(feedback.getStatus()).isEqualTo(AppFeedbackStatus.RESOLVED);
        }

        @Test
        @DisplayName("admin can change status to CLOSED")
        void changeStatus_success_toClosed() {
            when(repositoryAppFeedback.findById(10L)).thenReturn(Optional.of(feedback));
            when(repositoryAppFeedback.save(any())).thenReturn(feedback);
            when(mapperAppFeedback.toDto(any(), eq(2L))).thenReturn(feedbackDto);

            appFeedbackService.changeStatus(10L, AppFeedbackStatus.CLOSED, adminAuth);

            assertThat(feedback.getStatus()).isEqualTo(AppFeedbackStatus.CLOSED);
        }

        @Test
        @DisplayName("regular user cannot change status")
        void changeStatus_throws_forRegularUser() {
            assertThatThrownBy(() -> appFeedbackService.changeStatus(10L, AppFeedbackStatus.RESOLVED, userAuth))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("admins");

            verify(repositoryAppFeedback, never()).findById(any());
            verify(repositoryAppFeedback, never()).save(any());
        }

        @Test
        @DisplayName("throws EntityNotFoundException when feedback not found (admin)")
        void changeStatus_throws_whenNotFound() {
            when(repositoryAppFeedback.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appFeedbackService.changeStatus(999L, AppFeedbackStatus.RESOLVED, adminAuth))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET MY FEEDBACKS
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMyFeedbacks()")
    class GetMyFeedbacksTests {

        @Test
        @DisplayName("returns feedbacks belonging to the current user")
        void getMyFeedbacks_returnsOwnFeedbacks() {
            var page = new PageImpl<>(List.of(feedback));
            when(repositoryAppFeedback.findAllByCreatedBy(eq(1L), any(Pageable.class))).thenReturn(page);
            when(mapperAppFeedback.toDto(eq(feedback), eq(1L))).thenReturn(feedbackDto);

            PageResponse<AppFeedbackDto> result = appFeedbackService.getMyFeedbacks(0, 15, userAuth);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(feedbackDto);
        }

        @Test
        @DisplayName("returns empty page when user has no feedbacks")
        void getMyFeedbacks_returnsEmptyPage() {
            when(repositoryAppFeedback.findAllByCreatedBy(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            PageResponse<AppFeedbackDto> result = appFeedbackService.getMyFeedbacks(0, 15, userAuth);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET ALL BY STATUS
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllByStatus()")
    class GetAllByStatusTests {

        @Test
        @DisplayName("admin gets feedbacks filtered by NEW status")
        void getAllByStatus_returnsFilteredFeedbacks() {
            var page = new PageImpl<>(List.of(feedback));
            when(repositoryAppFeedback.findAllByStatus(eq(AppFeedbackStatus.NEW), any(Pageable.class))).thenReturn(page);
            when(mapperAppFeedback.toDto(eq(feedback), eq(2L))).thenReturn(feedbackDto);

            PageResponse<AppFeedbackDto> result = appFeedbackService.getAllByStatus(AppFeedbackStatus.NEW, 0, 15, adminAuth);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("returns empty when no feedbacks match the status")
        void getAllByStatus_returnsEmpty_whenNoMatch() {
            when(repositoryAppFeedback.findAllByStatus(eq(AppFeedbackStatus.CLOSED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            PageResponse<AppFeedbackDto> result = appFeedbackService.getAllByStatus(AppFeedbackStatus.CLOSED, 0, 15, adminAuth);

            assertThat(result.getContent()).isEmpty();
        }
    }
}

