package com.migration.etl.service;

import com.migration.etl.config.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class StatusMailService {

    private static final Logger log = LoggerFactory.getLogger(StatusMailService.class);

    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;
    private final boolean emailEnabled;

    public StatusMailService(JavaMailSender mailSender,
                             MailConfig mailConfig,
                             @Value("${app.email.enabled:false}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.mailConfig = mailConfig;
        this.emailEnabled = emailEnabled;
    }

    public void sendStatusMail(String body) {
        if (!emailEnabled) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(mailConfig.getTo());
            message.setFrom(mailConfig.getFrom());
            message.setSubject(mailConfig.getSubject());
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("There was an error sending status mail. Code: 43", e);
        }
    }
}
