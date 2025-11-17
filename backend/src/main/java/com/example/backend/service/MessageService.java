package com.example.backend.service;


import com.example.backend.model.User;
import com.example.backend.model.order.Order;

public interface MessageService {
    void sendMessage(String toEmail, String content, String subject);

    void sendOrderConfirmationAsync(Order order);
}
