package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.AbstractIntegrationTest;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.model.user.Role;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryAppFeedback;
import com.arturmolla.bookshelf.repository.RepositoryRole;
import com.arturmolla.bookshelf.repository.RepositoryUser;
import com.arturmolla.bookshelf.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllerAppFeedbackIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/app-feedbacks";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RepositoryAppFeedback repositoryAppFeedback;
    @Autowired
    private RepositoryUser repositoryUser;
    @Autowired
    private RepositoryRole repositoryRole;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    private User regularUser;
    private User adminUser;
    private User otherUser;
    private String userToken;
    private String adminToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(status -> {
            repositoryAppFeedback.deleteAll();
            repositoryUser.deleteAll();
            repositoryRole.deleteAll();
            return null;
        });

        transactionTemplate.execute(status -> {
            Role userRole = repositoryRole.save(Role.builder().name("ROLE_USER").build());
            Role adminRole = repositoryRole.save(Role.builder().name("ROLE_ADMIN").build());

            regularUser = repositoryUser.save(User.builder()
                    .firstname("John").lastname("Doe")
                    .email("john@test.com")
                    .password("$2a$10$dummyhash")
                    .roles(List.of(userRole))
                    .enabled(true)
                    .build());

            adminUser = repositoryUser.save(User.builder()
                    .firstname("Admin").lastname("User")
                    .email("admin@test.com")
                    .password("$2a$10$dummyhash")
                    .roles(List.of(userRole, adminRole))
                    .enabled(true)
                    .build());

            otherUser = repositoryUser.save(User.builder()
                    .firstname("Other").lastname("Person")
                    .email("other@test.com")
                    .password("$2a$10$dummyhash")
                    .roles(List.of(userRole))
                    .enabled(true)
                    .build());
            return null;
        });

        userToken = "Bearer " + jwtService.generateToken(regularUser);
        adminToken = "Bearer " + jwtService.generateToken(adminUser);
        otherToken = "Bearer " + jwtService.generateToken(otherUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private EntityAppFeedback createFeedbackAs(User owner, String title, String description) {
        return transactionTemplate.execute(status -> {
            var auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                EntityAppFeedback feedback = EntityAppFeedback.builder()
                        .title(title)
                        .description(description)
                        .status(AppFeedbackStatus.NEW)
                        .build();
                return repositoryAppFeedback.save(feedback);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // POST /app-feedbacks
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /app-feedbacks")
    class SaveTests {

        @Test
        @DisplayName("authenticated user can create a feedback")
        void save_returns200_withValidRequest() throws Exception {
            var body = Map.of("title", "Login Bug", "description", "Cannot log in with Google");

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Login Bug"))
                    .andExpect(jsonPath("$.description").value("Cannot log in with Google"))
                    .andExpect(jsonPath("$.status").value("NEW"))
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @Test
        @DisplayName("returns 400 when title is blank")
        void save_returns400_whenTitleBlank() throws Exception {
            var body = Map.of("title", "", "description", "Some description");

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when description is blank")
        void save_returns400_whenDescriptionBlank() throws Exception {
            var body = Map.of("title", "Valid Title", "description", "");

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 403 when no token provided")
        void save_returns403_whenUnauthenticated() throws Exception {
            var body = Map.of("title", "Some Bug", "description", "Some description");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /app-feedbacks
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /app-feedbacks")
    class GetAllTests {

        @Test
        @DisplayName("returns paginated feedbacks for authenticated user")
        void getAll_returns200_withFeedbacks() throws Exception {
            createFeedbackAs(regularUser, "Bug A", "Desc A");
            createFeedbackAs(adminUser, "Bug B", "Desc B");

            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElement").value(2));
        }

        @Test
        @DisplayName("returns empty page when no feedbacks exist")
        void getAll_returns200_withEmptyPage() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("respects page and size parameters")
        void getAll_respectsPagination() throws Exception {
            for (int i = 1; i <= 5; i++) {
                createFeedbackAs(regularUser, "Feedback " + i, "Desc " + i);
            }

            mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "2")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElement").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("returns 403 when unauthenticated")
        void getAll_returns403_whenUnauthenticated() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /app-feedbacks/me
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /app-feedbacks/me")
    class GetMyFeedbacksTests {

        @Test
        @DisplayName("returns only the current user's feedbacks")
        void getMyFeedbacks_returnsOwnFeedbacks() throws Exception {
            createFeedbackAs(regularUser, "My Feedback", "Mine");
            createFeedbackAs(adminUser, "Admin Feedback", "Not mine");

            mockMvc.perform(get(BASE_URL + "/me")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title").value("My Feedback"))
                    .andExpect(jsonPath("$.content[0].ownFeedback").value(true));
        }

        @Test
        @DisplayName("returns empty when user has no feedbacks")
        void getMyFeedbacks_returnsEmpty_whenNoOwn() throws Exception {
            createFeedbackAs(adminUser, "Admin Feedback", "Not mine");

            mockMvc.perform(get(BASE_URL + "/me")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /app-feedbacks/{id}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /app-feedbacks/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("returns feedback by ID")
        void getById_returns200() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Specific Bug", "Details here");

            mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.title").value("Specific Bug"));
        }

        @Test
        @DisplayName("returns 500 when feedback not found")
        void getById_returns500_whenNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/99999")
                            .header("Authorization", userToken))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /app-feedbacks/{id}/upvote
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /app-feedbacks/{id}/upvote")
    class UpvoteTests {

        @Test
        @DisplayName("user can upvote a feedback")
        void upvote_addsUpvote() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(adminUser, "Upvote Me", "Please upvote");

            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/upvote")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.upvoteCount").value(1))
                    .andExpect(jsonPath("$.upvotedByCurrentUser").value(true));
        }

        @Test
        @DisplayName("second upvote removes the upvote (toggle)")
        void upvote_togglesUpvote() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(adminUser, "Toggle Upvote", "Desc");

            // First upvote
            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/upvote")
                            .header("Authorization", userToken))
                    .andExpect(jsonPath("$.upvoteCount").value(1));

            // Second upvote removes it
            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/upvote")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.upvoteCount").value(0))
                    .andExpect(jsonPath("$.upvotedByCurrentUser").value(false));
        }

        @Test
        @DisplayName("returns 500 when feedback not found")
        void upvote_returns500_whenNotFound() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/99999/upvote")
                            .header("Authorization", userToken))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /app-feedbacks/{id}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /app-feedbacks/{id}")
    class EditTests {

        @Test
        @DisplayName("owner can edit their own feedback")
        void edit_success_forOwner() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Original Title", "Original Desc");
            var body = Map.of("title", "Updated Title", "description", "Updated Description");

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"))
                    .andExpect(jsonPath("$.description").value("Updated Description"));
        }

        @Test
        @DisplayName("admin can edit any feedback")
        void edit_success_forAdmin() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "User's Feedback", "Desc");
            var body = Map.of("title", "Admin Edited", "description", "Admin changed this");

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Admin Edited"));
        }

        @Test
        @DisplayName("non-owner cannot edit others' feedback (400 - OperationNotPermittedException)")
        void edit_returns400_forNonOwner() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Owned by user", "Desc");
            var body = Map.of("title", "Hijacked Title", "description", "Hijacked");

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body is invalid")
        void edit_returns400_whenInvalid() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Valid Feedback", "Valid Desc");
            var body = Map.of("title", "", "description", "");

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 500 when feedback not found")
        void edit_returns500_whenNotFound() throws Exception {
            var body = Map.of("title", "T", "description", "D");

            mockMvc.perform(put(BASE_URL + "/99999")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST /app-feedbacks/{id}/comments
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /app-feedbacks/{id}/comments")
    class AddCommentTests {

        @Test
        @DisplayName("any authenticated user can add a comment")
        void addComment_success() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Commented Feedback", "Desc");
            var body = Map.of("message", "This is a great idea!");

            mockMvc.perform(post(BASE_URL + "/" + saved.getId() + "/comments")
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments", hasSize(1)))
                    .andExpect(jsonPath("$.comments[0].message").value("This is a great idea!"));
        }

        @Test
        @DisplayName("multiple users can comment")
        void addComment_multipleUsers() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Multi Comment", "Desc");

            mockMvc.perform(post(BASE_URL + "/" + saved.getId() + "/comments")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("message", "First comment"))));

            mockMvc.perform(post(BASE_URL + "/" + saved.getId() + "/comments")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "Admin comment"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments", hasSize(2)));
        }

        @Test
        @DisplayName("returns 500 when feedback not found")
        void addComment_returns500_whenFeedbackNotFound() throws Exception {
            mockMvc.perform(post(BASE_URL + "/99999/comments")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "test"))))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /app-feedbacks/{id}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /app-feedbacks/{id}")
    class DeleteTests {

        @Test
        @DisplayName("owner can delete their own feedback")
        void delete_success_forOwner() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "To Delete", "Will be gone");

            mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isNoContent());

            assertThat(repositoryAppFeedback.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("admin can delete any feedback")
        void delete_success_forAdmin() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Admin Deletes", "Desc");

            mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isNoContent());

            assertThat(repositoryAppFeedback.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("non-owner cannot delete others' feedback (400)")
        void delete_returns400_forNonOwner() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Protected", "Desc");

            mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                            .header("Authorization", otherToken))
                    .andExpect(status().isBadRequest());

            assertThat(repositoryAppFeedback.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("returns 500 when feedback not found")
        void delete_returns500_whenNotFound() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/99999")
                            .header("Authorization", userToken))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /app-feedbacks/{id}/status (Admin only)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /app-feedbacks/{id}/status")
    class ChangeStatusTests {

        @Test
        @DisplayName("admin can change status to IN_PROGRESS")
        void changeStatus_toInProgress_forAdmin() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Status Feedback", "Desc");

            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/status")
                            .header("Authorization", adminToken)
                            .param("status", "IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("admin can change status to RESOLVED")
        void changeStatus_toResolved_forAdmin() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Resolve This", "Desc");

            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/status")
                            .header("Authorization", adminToken)
                            .param("status", "RESOLVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        @Test
        @DisplayName("admin can change status to CLOSED")
        void changeStatus_toClosed_forAdmin() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Close This", "Desc");

            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/status")
                            .header("Authorization", adminToken)
                            .param("status", "CLOSED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("regular user cannot change status (500 - @Secured blocks access)")
        void changeStatus_returns403_forRegularUser() throws Exception {
            EntityAppFeedback saved = createFeedbackAs(regularUser, "Status Block", "Desc");

            mockMvc.perform(patch(BASE_URL + "/" + saved.getId() + "/status")
                            .header("Authorization", userToken)
                            .param("status", "RESOLVED"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("returns 500 when feedback not found (admin)")
        void changeStatus_returns500_whenNotFound() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/99999/status")
                            .header("Authorization", adminToken)
                            .param("status", "RESOLVED"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /app-feedbacks/by-status (Admin only)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /app-feedbacks/by-status")
    class GetAllByStatusTests {

        @Test
        @DisplayName("admin can filter feedbacks by NEW status")
        void getAllByStatus_returnsFiltered_forAdmin() throws Exception {
            createFeedbackAs(regularUser, "New Feedback", "Desc");
            EntityAppFeedback resolved = createFeedbackAs(regularUser, "Resolved Feedback", "Desc");
            transactionTemplate.execute(status -> {
                resolved.setStatus(AppFeedbackStatus.RESOLVED);
                repositoryAppFeedback.save(resolved);
                return null;
            });

            mockMvc.perform(get(BASE_URL + "/by-status")
                            .header("Authorization", adminToken)
                            .param("status", "NEW"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title").value("New Feedback"));
        }

        @Test
        @DisplayName("regular user cannot access by-status endpoint (500 - @Secured blocks access)")
        void getAllByStatus_returns403_forRegularUser() throws Exception {
            mockMvc.perform(get(BASE_URL + "/by-status")
                            .header("Authorization", userToken)
                            .param("status", "NEW"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("returns empty when no feedbacks match the status")
        void getAllByStatus_returnsEmpty_whenNoMatch() throws Exception {
            createFeedbackAs(regularUser, "New One", "Desc");

            mockMvc.perform(get(BASE_URL + "/by-status")
                            .header("Authorization", adminToken)
                            .param("status", "CLOSED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }
}


