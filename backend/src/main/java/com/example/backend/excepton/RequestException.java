package com.example.backend.excepton;

import lombok.Getter;

@Getter
public abstract class RequestException extends RuntimeException {
    private final int statusCode;

  public RequestException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }
}
