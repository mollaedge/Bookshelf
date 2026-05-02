package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class DtoUserBase {

    private Long id;
    private String firstname;
    private String lastname;
    private String fullName;
    private String email;
    private String bio;
    private String location;
    private String provider;
    private boolean hasProfilePic;
    private boolean hasWallpaper;
}

