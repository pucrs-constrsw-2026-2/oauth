package com.seugrupo.oauth.controller;

import com.seugrupo.oauth.dto.CreateUserRequest;
import com.seugrupo.oauth.dto.UpdatePasswordRequest;
import com.seugrupo.oauth.dto.UpdateUserRequest;
import com.seugrupo.oauth.dto.UserResponse;
import com.seugrupo.oauth.service.KeycloakUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final KeycloakUserService userService;

    public UserController(KeycloakUserService userService) {
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserResponse> create(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(authorization, request));
    }

    @GetMapping
    List<UserResponse> list(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) Boolean enabled
    ) {
        return userService.list(authorization, enabled);
    }

    @GetMapping("/{id}")
    UserResponse getById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String id
    ) {
        return userService.getById(authorization, id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> update(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        userService.update(authorization, id, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> updatePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String id,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        userService.updatePassword(authorization, id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> disable(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String id
    ) {
        userService.disable(authorization, id);
        return ResponseEntity.noContent().build();
    }
}
