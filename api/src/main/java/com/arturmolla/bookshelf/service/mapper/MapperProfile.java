package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.DtoProfile;
import com.arturmolla.bookshelf.model.dto.DtoUpdateProfileRequest;
import com.arturmolla.bookshelf.model.user.User;
import org.springframework.stereotype.Component;

@Component
public class MapperProfile {

    public DtoProfile toDto(User user) {
        if (user == null) {
            return null;
        }

        return DtoProfile.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .dateOfBirth(user.getDateOfBirth())
                .bio(user.getBio())
                .location(user.getLocation())
                .provider(user.getProvider())
                .accountLocked(user.isAccountLocked())
                .enabled(user.isEnabled())
                .build();
    }

    public void updateEntityFromRequest(User user, DtoUpdateProfileRequest request) {
        if (request.getFirstname() != null) {
            user.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            user.setLastname(request.getLastname());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
    }

    public User toEntity(DtoProfile dtoProfile) {
        if (dtoProfile == null) {
            return null;
        }

        return User.builder()
                .id(dtoProfile.getId())
                .firstname(dtoProfile.getFirstname())
                .lastname(dtoProfile.getLastname())
                .email(dtoProfile.getEmail())
                .dateOfBirth(dtoProfile.getDateOfBirth())
                .provider(dtoProfile.getProvider())
                .accountLocked(dtoProfile.isAccountLocked())
                .enabled(dtoProfile.isEnabled())
                .build();
    }

}
