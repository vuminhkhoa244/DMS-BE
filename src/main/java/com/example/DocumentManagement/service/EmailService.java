package com.example.DocumentManagement.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("lyvinhthai321@gmail.com");
        message.setTo(to);
        message.setSubject("Welcome to Document Management System");
        message.setText("Hi " + name + ",\n\nWelcome to our Document Management System! "
                + "Your account has been created successfully.\n\nBest regards,\nDM Team");
        mailSender.send(message);
    }
}
