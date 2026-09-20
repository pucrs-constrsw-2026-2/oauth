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

    public UserResponse create(CreateUserRequest request) {
        return userClient.create(request);
    }

    public List<UserResponse> findAll() {
        return userClient.findAll();
    }

    public UserResponse findById(String id) {
        return userClient.findById(id);
    }

    public void update(String id, UpdateUserRequest request) {
        userClient.update(id, request);
    }

    public void updatePassword(String id, UpdatePasswordRequest request) {
        userClient.updatePassword(id, request);
    }

    public void disable(String id) {
        userClient.disable(id);
    }
}
