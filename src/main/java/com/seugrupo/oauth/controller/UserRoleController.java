package com.seugrupo.oauth.controller;

import com.seugrupo.oauth.service.KeycloakRoleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserRoleController {

    private final KeycloakRoleService roleService;

    public UserRoleController(KeycloakRoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/{userId}/roles/{roleId}")
    ResponseEntity<Void> assign(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String userId,
            @PathVariable String roleId
    ) {
        roleService.assignToUser(authorization, userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    ResponseEntity<Void> unassign(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String userId,
            @PathVariable String roleId
    ) {
        roleService.unassignFromUser(authorization, userId, roleId);
        return ResponseEntity.noContent().build();
    }
}
