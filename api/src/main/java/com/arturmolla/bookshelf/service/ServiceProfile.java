package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.dto.DtoProfile;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryProfile;
import com.arturmolla.bookshelf.service.mapper.MapperProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceProfile {

    private final RepositoryProfile repositoryProfile;
    private final MapperProfile mapperProfile;

    public DtoProfile getUserProfile(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Optional<User> profile = repositoryProfile.findById(user.getId());
        if (profile.isPresent()) {
            return mapperProfile.toDto(profile.get());
        }
        return null;
    }
}
