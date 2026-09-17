package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void sendPasswordResetEmail_buildsAndSendsCorrectMessage() {
        String to = "user@example.com";
        String token = "sample-reset-token-12345";

        emailService.sendPasswordResetEmail(to, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("user@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Reset your Finance Tracker password");
        assertThat(sentMessage.getText()).contains("http://localhost:3000/reset-password?token=sample-reset-token-12345");
        assertThat(sentMessage.getText()).contains("valid for 30 minutes");
    }

    @Test
    void sendPasswordResetEmail_whenMailSenderThrowsException_throwsMailException() {
        String to = "user@example.com";
        String token = "sample-reset-token-12345";

        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendPasswordResetEmail(to, token))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP connection refused");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
