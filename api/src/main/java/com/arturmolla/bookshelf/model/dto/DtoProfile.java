package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DtoProfile extends DtoUserBase {

    private LocalDate dateOfBirth;
    private boolean accountLocked;
    private boolean enabled;
}
