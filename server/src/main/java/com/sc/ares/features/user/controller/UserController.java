package com.sc.ares.features.user.controller;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.user.dto.ChangePasswordRequest;
import com.sc.ares.features.user.dto.CreateUserRequest;
import com.sc.ares.features.user.dto.OwnProfileResponse;
import com.sc.ares.features.user.dto.UpdateOwnProfileRequest;
import com.sc.ares.features.user.dto.UpdateUserRequest;
import com.sc.ares.features.user.dto.UserLogoUploadResponse;
import com.sc.ares.features.user.dto.UserResponse;
import com.sc.ares.features.user.service.UserLogoStorageService;
import com.sc.ares.features.user.service.UserManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserManagementService userManagementService;
    private final UserLogoStorageService userLogoStorageService;

    public UserController(
        UserManagementService userManagementService,
        UserLogoStorageService userLogoStorageService
    ) {
        this.userManagementService = userManagementService;
        this.userLogoStorageService = userLogoStorageService;
    }

    @GetMapping
    public PagedResponse<UserResponse> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        return userManagementService.listUsers(page, size, sort, status, search);
    }

    @GetMapping("/{userId:[0-9a-fA-F\\-]{36}}")
    public UserResponse getUser(@PathVariable UUID userId) {
        return userManagementService.getUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userManagementService.createUser(request);
    }

    @PutMapping("/{userId:[0-9a-fA-F\\-]{36}}")
    public UserResponse updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        return userManagementService.updateUser(userId, request);
    }

    @PutMapping("/me")
    public OwnProfileResponse updateOwnProfile(@Valid @RequestBody UpdateOwnProfileRequest request) {
        return userManagementService.updateOwnProfile(request);
    }

    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserLogoUploadResponse uploadOwnLogo(@RequestParam("file") MultipartFile file) {
        String fileName = userManagementService.storeOwnLogo(file);
        String logoUrl = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath("/api/users/logo/" + fileName)
            .toUriString();

        return new UserLogoUploadResponse(logoUrl);
    }

    @GetMapping("/logo/{fileName}")
    public ResponseEntity<Resource> getUserLogo(@PathVariable String fileName) {
        Resource logoResource = userLogoStorageService.getLogoResource(fileName);

        return ResponseEntity.ok()
            .contentType(userLogoStorageService.getMediaType(fileName))
            .cacheControl(CacheControl.noCache())
            .body(logoResource);
    }

    @DeleteMapping("/{userId:[0-9a-fA-F\\-]{36}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userManagementService.deleteUser(userId);
    }

    @PostMapping("/me/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userManagementService.changeOwnPassword(request);
    }
}
