package com.arturmolla.bookshelf.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_feedback")
public class EntityAppFeedback {

    @Id
    private Long id;
    private String title;
    private String description;
    private Integer likeCount;
    private List<String> labels;
    private String createdBy;
    private LocalDateTime createdAt;

    public String getAge() {
        if (createdAt == null) {
            return "unknown";
        }

        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(createdAt, now).getSeconds();

        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "") + " ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        }

        long days = hours / 24;
        if (days < 7) {
            return days + " day" + (days != 1 ? "s" : "") + " ago";
        }

        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + " week" + (weeks != 1 ? "s" : "") + " ago";
        }

        long months = days / 30;
        if (months < 12) {
            return months + " month" + (months != 1 ? "s" : "") + " ago";
        }

        long years = days / 365;
        return years + " year" + (years != 1 ? "s" : "") + " ago";
    }
}
