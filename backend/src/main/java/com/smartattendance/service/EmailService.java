package com.smartattendance.service;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import com.smartattendance.controller.CourseController;
import com.smartattendance.exception.InvalidRequestException;
import com.smartattendance.util.constants.EmailConstants;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    public void sendmail(String recipientEmail) {    
        try {
            Properties props = new Properties();
            
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", EmailConstants.EMAILER_HOST);
            props.put("mail.smtp.port", EmailConstants.EMAILER_PORT);
            
            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EmailConstants.EMAILER_ADDRESS, EmailConstants.EMAILER_PASSWORD);
                }
            });
            logger.info(EmailConstants.EMAILER_ADDRESS);
    
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(EmailConstants.EMAILER_ADDRESS, false));
    
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            msg.setSubject("Report");
            msg.setContent("<b>Test</b>", "text/html");
    
            Transport.send(msg);
        } catch (AddressException e) {
            throw new InvalidRequestException("AddressException: " + e.getMessage());
        } catch (MessagingException e) {
            throw new InvalidRequestException("MessagingException: " + e.getMessage());
        } 
    }
}
