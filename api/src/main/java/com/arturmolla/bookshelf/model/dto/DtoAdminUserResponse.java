package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoAdminUserResponse {

    private Long id;
    private String firstname;
    private String lastname;
    private String fullName;
    private String email;
    private String bio;
    private String location;
    private String provider;
    private LocalDate dateOfBirth;
    private boolean accountLocked;
    private boolean enabled;
    private List<String> roles;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}

