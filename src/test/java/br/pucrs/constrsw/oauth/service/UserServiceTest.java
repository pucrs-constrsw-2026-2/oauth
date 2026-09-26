package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.client.UserClient;
import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UpdateUserRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String AUTHORIZATION = "Bearer user-token";

    private final UserClient userClient = mock(UserClient.class);
    private final UserService userService = new UserService(userClient);

    @Test
    void delegatesUserCreation() {
        CreateUserRequest request = new CreateUserRequest(
                "user@example.com", "secret", "First", "Last");
        UserResponse expected = new UserResponse(
                "user-id", "user@example.com", "First", "Last", true);
        when(userClient.create(AUTHORIZATION, request)).thenReturn(expected);

        UserResponse response = userService.create(AUTHORIZATION, request);

        assertThat(response).isEqualTo(expected);
        verify(userClient).create(AUTHORIZATION, request);
    }

    @Test
    void delegatesUserListing() {
        List<UserResponse> expected = List.of(
                new UserResponse("user-id", "user@example.com", "First", "Last", true));
        when(userClient.findAll(AUTHORIZATION)).thenReturn(expected);

        List<UserResponse> response = userService.findAll(AUTHORIZATION);

        assertThat(response).isEqualTo(expected);
        verify(userClient).findAll(AUTHORIZATION);
    }

    @Test
    void delegatesFindingUserById() {
        UserResponse expected = new UserResponse(
                "user-id", "user@example.com", "First", "Last", true);
        when(userClient.findById(AUTHORIZATION, "user-id")).thenReturn(expected);

        UserResponse response = userService.findById(AUTHORIZATION, "user-id");

        assertThat(response).isEqualTo(expected);
        verify(userClient).findById(AUTHORIZATION, "user-id");
    }

    @Test
    void delegatesUserUpdate() {
        UpdateUserRequest request = new UpdateUserRequest("Updated", "User", true);

        userService.update(AUTHORIZATION, "user-id", request);

        verify(userClient).update(AUTHORIZATION, "user-id", request);
    }

    @Test
    void delegatesPasswordUpdate() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("new-secret");

        userService.updatePassword(AUTHORIZATION, "user-id", request);

        verify(userClient).updatePassword(AUTHORIZATION, "user-id", request);
    }

    @Test
    void delegatesUserDisablement() {
        userService.disable(AUTHORIZATION, "user-id");

        verify(userClient).disable(AUTHORIZATION, "user-id");
    }
}
