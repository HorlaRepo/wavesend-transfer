package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.enums.EmailTemplateName;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.mail.MailSendException;

class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;

    @Captor
    private ArgumentCaptor<String> templateCaptor;

    @InjectMocks
    private EmailServiceImpl emailService;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_SUBJECT = "Test Subject";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendEmail_WithValidParameters_SendsEmail() throws MessagingException {
        // Arrange
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "Test User");

        EmailTemplateName templateName = mock(EmailTemplateName.class);
        when(templateName.getName()).thenReturn("test-template");

        String processedTemplate = "<html><body>Test Template</body></html>";
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(processedTemplate);

        // Act
        emailService.sendEmail(TEST_EMAIL, properties, templateName, TEST_SUBJECT);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(templateCaptor.capture(), contextCaptor.capture());

        assertEquals("test-template", templateCaptor.getValue());

        // Verify each property individually
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            assertEquals(entry.getValue(), contextCaptor.getValue().getVariable(entry.getKey()));
        }

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_WithNullTemplateName_UsesDefaultTemplate() throws MessagingException {
        // Arrange
        Map<String, Object> properties = new HashMap<>();
        String processedTemplate = "<html><body>Default Template</body></html>";

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(processedTemplate);

        // Act
        emailService.sendEmail(TEST_EMAIL, properties, null, TEST_SUBJECT);

        // Assert
        verify(templateEngine).process(templateCaptor.capture(), contextCaptor.capture());

        assertEquals("confirm-email", templateCaptor.getValue());

        // Verify each property individually
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            assertEquals(entry.getValue(), contextCaptor.getValue().getVariable(entry.getKey()));
        }

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_WhenExceptionOccurs_PropagatesException() {
        // Arrange
        Map<String, Object> properties = new HashMap<>();
        EmailTemplateName templateName = mock(EmailTemplateName.class);
        when(templateName.getName()).thenReturn("test-template");

        // Use MailSendException instead of MessagingException
        doThrow(new MailSendException("Test error")).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail(TEST_EMAIL, properties, templateName, TEST_SUBJECT));

        verify(templateEngine).process(anyString(), any(Context.class));
    }
}