package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.client.UserClient;
import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UpdateUserRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserClient userClient;

    public UserService(UserClient userClient) {
        this.userClient = userClient;
    }

    public UserResponse create(String authorization, CreateUserRequest request) {
        return userClient.create(authorization, request);
    }

    public List<UserResponse> findAll(String authorization) {
        return userClient.findAll(authorization);
    }

    public UserResponse findById(String authorization, String id) {
        return userClient.findById(authorization, id);
    }

    public void update(String authorization, String id, UpdateUserRequest request) {
        userClient.update(authorization, id, request);
    }

    public void updatePassword(String authorization, String id, UpdatePasswordRequest request) {
        userClient.updatePassword(authorization, id, request);
    }

    public void disable(String authorization, String id) {
        userClient.disable(authorization, id);
    }
}
