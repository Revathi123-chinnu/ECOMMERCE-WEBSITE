package com.example.shoppingcartwebsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Set your SMTP server details
        mailSender.setHost("smtp.gmail.com"); // For Gmail
        mailSender.setPort(587); // Use 465 for SSL or 587 for TLS

        // Replace with your email credentials
        mailSender.setUsername("your-email@gmail.com"); // Replace with your Gmail email
        mailSender.setPassword("your-app-password");    // Replace with your Gmail app password

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Enable TLS
        props.put("mail.debug", "true"); // Enable debug logging

        return mailSender;
    }
}
