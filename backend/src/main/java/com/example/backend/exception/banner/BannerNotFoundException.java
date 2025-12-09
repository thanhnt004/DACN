package com.example.backend.exception.banner;

import com.example.backend.exception.NotFoundException;

public class BannerNotFoundException extends NotFoundException {
    public BannerNotFoundException(String message) {
        super(message);
    }
}

