package br.pucrs.constrsw.oauth.client;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UpdateUserRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import br.pucrs.constrsw.oauth.error.UserManagementNotImplementedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserClient {

    public UserResponse create(CreateUserRequest request) {
        throw notImplemented();
    }

    public List<UserResponse> findAll() {
        throw notImplemented();
    }

    public UserResponse findById(String id) {
        throw notImplemented();
    }

    public void update(String id, UpdateUserRequest request) {
        throw notImplemented();
    }

    public void updatePassword(String id, UpdatePasswordRequest request) {
        throw notImplemented();
    }

    public void disable(String id) {
        throw notImplemented();
    }

    private UserManagementNotImplementedException notImplemented() {
        return new UserManagementNotImplementedException();
    }
}
