package com.dehold.contentmanager.user.web.dto;

public class CreateUserRequest {

    private String alias;

    private String email;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
