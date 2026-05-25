package com.migration.etl.service;

import com.migration.etl.config.MailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatusMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailConfig mailConfig;

    @BeforeEach
    void setUp() {
        mailConfig = new MailConfig();
        mailConfig.setTo("recipient@example.com");
        mailConfig.setFrom("sender@example.com");
        mailConfig.setSubject("Talend Open Studio notification");
        mailConfig.setSmtpHost("localhost");
        mailConfig.setPort(25);
    }

    @Test
    void sendStatusMail_whenDisabled_doesNotSend() {
        StatusMailService service = new StatusMailService(mailSender, mailConfig, false);

        service.sendStatusMail("test");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendStatusMail_whenEnabled_sendsWithCorrectProperties() {
        StatusMailService service = new StatusMailService(mailSender, mailConfig, true);

        service.sendStatusMail("test body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("recipient@example.com", sent.getTo()[0]);
        assertEquals("sender@example.com", sent.getFrom());
        assertEquals("Talend Open Studio notification", sent.getSubject());
        assertEquals("test body", sent.getText());
    }

    @Test
    void sendStatusMail_whenSendFails_doesNotThrow() {
        StatusMailService service = new StatusMailService(mailSender, mailConfig, true);
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.sendStatusMail("test"));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
