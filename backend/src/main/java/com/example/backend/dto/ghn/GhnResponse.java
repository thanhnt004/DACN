package com.example.backend.dto.ghn;

import lombok.Data;

@Data
public class GhnResponse<T> {
    private int code;
    private String message;
    private T data;

    public boolean isSuccess() {
        return code == 200;
    }
}