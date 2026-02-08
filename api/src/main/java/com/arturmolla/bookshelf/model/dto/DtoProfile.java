package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoProfile {

    private Long id;
    private String firstname;
    private String lastname;
    private String fullName;
    private String email;
    private LocalDate dateOfBirth;
    private String provider;
    private boolean accountLocked;
    private boolean enabled;
}
