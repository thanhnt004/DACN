package com.example.backend.excepton;

import lombok.Getter;

@Getter
public abstract class RequestException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

  public RequestException(int statusCode, String errorCode, String message) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}
