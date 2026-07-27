package com.example.imanicommunityapp;

public class GenericResponse {

    private boolean success;
    private String message;
    private Object data; // Optional – only present if backend sends data

    public boolean isSuccess() {
        return success;
    }

    public String getStatus
            () {
        return message;
    }

    public Object getData() {
        return data;
    }
}

