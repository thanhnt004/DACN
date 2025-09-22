package com.example.backend.service;


import com.example.backend.model.User;

public interface MessageService {
    void sendMessage(String toEmail, String content, String subject);
}
