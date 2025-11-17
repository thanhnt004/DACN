package com.example.backend.service;

import com.example.backend.model.order.Order;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class EmailService implements MessageService {
    private final JavaMailSender mailSender;
    @Override
    public void sendMessage(String toEmail, String content,String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setText(content, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom("nhthanh2k4@gmail.com");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    @Async
    public void sendOrderConfirmationAsync(Order order) {
        String toEmail = order.getUser().getEmail();
        String subject = "Order Confirmation - " + order.getOrderNumber();
        String content = "<h1>Thank you for your order!</h1>"
                + "<p>Your order number is: " + order.getOrderNumber() + "</p>"
                + "<p>We will notify you when your order is shipped.</p>";
        sendMessage(toEmail, content, subject);
    }
}
