package br.pucrs.constrsw.oauth.error;

public class UserManagementNotImplementedException extends RuntimeException {

    public UserManagementNotImplementedException() {
        super("User management integration is not implemented yet");
    }
}
