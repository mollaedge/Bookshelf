package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.dto.DtoProfile;
import com.arturmolla.bookshelf.model.dto.DtoUpdateProfileRequest;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryProfile;
import com.arturmolla.bookshelf.service.mapper.MapperProfile;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ServiceProfile {

    private final RepositoryProfile repositoryProfile;
    private final MapperProfile mapperProfile;
    private final ServiceFileStorage fileStorage;

    public DtoProfile getUserProfile(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        return repositoryProfile.findById(user.getId())
                .map(mapperProfile::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));
    }

    @Transactional
    public DtoProfile updateUserProfile(Authentication connectedUser, DtoUpdateProfileRequest request) {
        var principal = (User) connectedUser.getPrincipal();
        User user = repositoryProfile.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));
        mapperProfile.updateEntityFromRequest(user, request);
        return mapperProfile.toDto(repositoryProfile.save(user));
    }

    @Transactional
    public void deleteUserProfile(Authentication connectedUser) {
        var principal = (User) connectedUser.getPrincipal();
        User user = repositoryProfile.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));
        repositoryProfile.delete(user);
    }

    // -------------------------------------------------------------------------
    // Profile picture
    // -------------------------------------------------------------------------

    @Transactional
    public void uploadProfilePic(MultipartFile file, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        fileStorage.saveProfilePic(file, user.getId());
    }

    public byte[] getProfilePic(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        return fileStorage.loadProfilePic(user.getId());
    }

    public String getProfilePicContentType(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        return fileStorage.getProfilePicContentType(user.getId());
    }

    // -------------------------------------------------------------------------
    // Wallpaper
    // -------------------------------------------------------------------------

    @Transactional
    public void uploadWallpaper(MultipartFile file, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        fileStorage.saveWallpaper(file, user.getId());
    }

    public byte[] getWallpaper(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        return fileStorage.loadWallpaper(user.getId());
    }

    public String getWallpaperContentType(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        return fileStorage.getWallpaperContentType(user.getId());
    }
}
