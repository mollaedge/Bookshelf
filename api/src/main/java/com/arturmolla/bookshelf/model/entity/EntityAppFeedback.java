package com.arturmolla.bookshelf.model.entity;

import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_feedback")
public class EntityAppFeedback extends EntityBase {

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AppFeedbackStatus status = AppFeedbackStatus.NEW;

    @ElementCollection
    @CollectionTable(name = "app_feedback_upvotes", joinColumns = @JoinColumn(name = "feedback_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> upvotedBy = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "app_feedback_comments", joinColumns = @JoinColumn(name = "feedback_id"))
    @Builder.Default
    private List<EntityAppFeedbackComment> comments = new ArrayList<>();

    public int getUpvoteCount() {
        return upvotedBy == null ? 0 : upvotedBy.size();
    }

    public String getAge() {
        LocalDateTime createdAt = getCreatedDate();
        if (createdAt == null) {
            return "unknown";
        }

        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(createdAt, now).getSeconds();

        if (seconds < 60) return seconds + " second" + (seconds != 1 ? "s" : "") + " ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        long days = hours / 24;
        if (days < 7) return days + " day" + (days != 1 ? "s" : "") + " ago";
        long weeks = days / 7;
        if (weeks < 4) return weeks + " week" + (weeks != 1 ? "s" : "") + " ago";
        long months = days / 30;
        if (months < 12) return months + " month" + (months != 1 ? "s" : "") + " ago";
        long years = days / 365;
        return years + " year" + (years != 1 ? "s" : "") + " ago";
    }
}
