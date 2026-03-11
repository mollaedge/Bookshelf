package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
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
public class DtoUpdateProfileRequest {

    @Size(max = 50, message = "Firstname must be at most 50 characters")
    private String firstname;

    @Size(max = 50, message = "Lastname must be at most 50 characters")
    private String lastname;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;
}

