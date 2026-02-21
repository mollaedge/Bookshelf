package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.AbstractIntegrationTest;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.model.user.Role;
import com.arturmolla.bookshelf.model.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// No @Transactional — keeping the same context key as ControllerAppFeedbackIT
// so Spring caches and reuses the single ApplicationContext across both classes.
class RepositoryAppFeedbackTest extends AbstractIntegrationTest {

    private static final Long USER_1_ID = 1L;
    private static final Long USER_2_ID = 2L;

    @Autowired
    private RepositoryAppFeedback repositoryAppFeedback;
    @Autowired
    private RepositoryUser repositoryUser;
    @Autowired
    private RepositoryRole repositoryRole;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(status -> {
            repositoryAppFeedback.deleteAll();
            repositoryUser.deleteAll();
            repositoryRole.deleteAll();
            return null;
        });

        transactionTemplate.execute(status -> {
            Role role = repositoryRole.save(Role.builder().name("ROLE_USER").build());
            testUser = repositoryUser.save(User.builder()
                    .firstname("Test").lastname("User")
                    .email("repo-test@test.com")
                    .password("$2a$10$dummyhash")
                    .roles(List.of(role))
                    .enabled(true)
                    .build());
            return null;
        });

        var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transactionTemplate.execute(status -> {
            repositoryAppFeedback.deleteAll();
            repositoryUser.deleteAll();
            repositoryRole.deleteAll();
            return null;
        });
    }

    private EntityAppFeedback save(String title, String description, AppFeedbackStatus status) {
        return transactionTemplate.execute(s -> {
            var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return repositoryAppFeedback.save(EntityAppFeedback.builder()
                    .title(title).description(description).status(status).build());
        });
    }

    @Test
    @DisplayName("Save feedback persists entity correctly")
    void save_persistsFeedback() {
        EntityAppFeedback saved = save("Bug Report", "App crashes on login", AppFeedbackStatus.NEW);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Bug Report");
        assertThat(saved.getStatus()).isEqualTo(AppFeedbackStatus.NEW);
    }

    @Test
    @DisplayName("findById returns feedback when it exists")
    void findById_returnsFeedback_whenExists() {
        EntityAppFeedback saved = save("Feature Request", "Add dark mode", AppFeedbackStatus.NEW);

        Optional<EntityAppFeedback> found = repositoryAppFeedback.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Feature Request");
    }

    @Test
    @DisplayName("findById returns empty when feedback does not exist")
    void findById_returnsEmpty_whenNotExists() {
        assertThat(repositoryAppFeedback.findById(999L)).isEmpty();
    }

    @Test
    @DisplayName("findAllByStatus returns only feedbacks with matching status")
    void findAllByStatus_returnsMatchingFeedbacks() {
        save("F1", "D1", AppFeedbackStatus.NEW);
        save("F2", "D2", AppFeedbackStatus.NEW);
        save("F3", "D3", AppFeedbackStatus.RESOLVED);

        Page<EntityAppFeedback> newOnes = repositoryAppFeedback.findAllByStatus(AppFeedbackStatus.NEW, PageRequest.of(0, 10));
        Page<EntityAppFeedback> resolved = repositoryAppFeedback.findAllByStatus(AppFeedbackStatus.RESOLVED, PageRequest.of(0, 10));

        assertThat(newOnes.getTotalElements()).isEqualTo(2);
        assertThat(resolved.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAllByStatus returns empty page when no feedbacks match")
    void findAllByStatus_returnsEmpty_whenNoMatch() {
        save("F1", "D1", AppFeedbackStatus.NEW);

        Page<EntityAppFeedback> result = repositoryAppFeedback.findAllByStatus(AppFeedbackStatus.CLOSED, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByCreatedBy query executes without error")
    void findAllByCreatedBy_queryExecutes() {
        Page<EntityAppFeedback> result = repositoryAppFeedback.findAllByCreatedBy(USER_1_ID, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("delete removes the feedback")
    void delete_removesFeedback() {
        EntityAppFeedback saved = save("To Delete", "Will be deleted", AppFeedbackStatus.NEW);

        transactionTemplate.execute(status -> {
            repositoryAppFeedback.deleteById(saved.getId());
            return null;
        });

        assertThat(repositoryAppFeedback.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Updating status persists correctly")
    void updateStatus_persistsCorrectly() {
        EntityAppFeedback saved = save("Status Test", "Check status update", AppFeedbackStatus.NEW);

        transactionTemplate.execute(status -> {
            EntityAppFeedback toUpdate = repositoryAppFeedback.findById(saved.getId()).orElseThrow();
            toUpdate.setStatus(AppFeedbackStatus.IN_PROGRESS);
            repositoryAppFeedback.saveAndFlush(toUpdate);
            return null;
        });

        EntityAppFeedback updated = repositoryAppFeedback.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppFeedbackStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Pagination works correctly for findAll")
    void findAll_paginatesCorrectly() {
        for (int i = 1; i <= 5; i++) {
            save("Feedback " + i, "Description " + i, AppFeedbackStatus.NEW);
        }

        Page<EntityAppFeedback> page0 = repositoryAppFeedback.findAll(PageRequest.of(0, 2));
        Page<EntityAppFeedback> page1 = repositoryAppFeedback.findAll(PageRequest.of(1, 2));
        Page<EntityAppFeedback> page2 = repositoryAppFeedback.findAll(PageRequest.of(2, 2));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Upvotes set is persisted correctly")
    void upvotedBy_persistedCorrectly() {
        EntityAppFeedback saved = transactionTemplate.execute(status -> {
            var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            EntityAppFeedback f = EntityAppFeedback.builder()
                    .title("Upvote Test").description("Check upvotes").status(AppFeedbackStatus.NEW).build();
            f.getUpvotedBy().add(USER_1_ID);
            f.getUpvotedBy().add(USER_2_ID);
            return repositoryAppFeedback.saveAndFlush(f);
        });

        // Access lazy collection inside a transaction to avoid LazyInitializationException
        transactionTemplate.execute(status -> {
            EntityAppFeedback loaded = repositoryAppFeedback.findById(saved.getId()).orElseThrow();
            assertThat(loaded.getUpvotedBy()).containsExactlyInAnyOrder(USER_1_ID, USER_2_ID);
            assertThat(loaded.getUpvoteCount()).isEqualTo(2);
            return null;
        });
    }
}

