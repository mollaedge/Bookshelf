package com.arturmolla.bookshelf.repository.specification;

import com.arturmolla.bookshelf.model.entity.EntityBook;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SpecificationBook {

    public static Specification<EntityBook> withOwnerId(Long ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<EntityBook> notOwnedBy(Long userId) {
        return (root, query, cb) ->
                cb.notEqual(root.get("owner").get("id"), userId);
    }

    public static Specification<EntityBook> notArchived() {
        return (root, query, cb) ->
                cb.isFalse(root.get("archived"));
    }

    public static Specification<EntityBook> isShareable() {
        return (root, query, cb) ->
                cb.isTrue(root.get("shareable"));
    }

    /**
     * Case-insensitive LIKE search across title, authorName, synopsis, isbn, and genre.
     * Returns all books when {@code query} is null or blank.
     */
    public static Specification<EntityBook> matchesQuery(String query) {
        return (root, q, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + query.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            for (String field : List.of("title", "authorName", "synopsis", "isbn", "genre")) {
                predicates.add(cb.like(cb.lower(root.get(field).as(String.class)), pattern));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
