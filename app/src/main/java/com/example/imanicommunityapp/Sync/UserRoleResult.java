package com.example.imanicommunityapp.Sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserRoleResult {
    private final boolean success;
    @Nullable
    private final String userRole;
    @Nullable
    private final String message;

    private UserRoleResult(boolean success, @Nullable String userRole, @Nullable String message) {
        this.success = success;
        this.userRole = userRole;
        this.message = message;
    }

    public static UserRoleResult success(@NonNull String userRole) {
        return new UserRoleResult(true, userRole, null);
    }

    public static UserRoleResult failure(@NonNull String message) {
        return new UserRoleResult(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getUserRole() {
        return userRole;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}

