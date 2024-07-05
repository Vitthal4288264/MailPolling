package com.example.MailPoller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.mail.Session;
import java.util.Properties;

@Configuration
public class MailConfig {


    @Value("${email.imap.host}")
    String imapHost;

    @Value("${email.imap.port}")
    String imapPort;

    @Value("${email.imap.ssl}")
    boolean imapSsl;

    @Value("${email.pop3s.host}")
    String popHost;

    @Value("${email.pop3s.port}")
    String popPort;

    @Value("${email.pop3s.ssl}")
    boolean popSsl;

    @Bean
    public Session emailSession() {
        Properties properties = new Properties();
        // Common settings
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.pop3s.host", popHost);
        properties.put("mail.pop3s.port", popPort);
        properties.put("mail.pop3s.ssl.enable", popSsl);

        properties.put("mail.imaps.host", imapHost);
        properties.put("mail.imaps.port", imapPort);
        properties.put("mail.imaps.ssl.enable", imapSsl);

        properties.put("mail.debug", "true"); // Enable debugging

        return Session.getInstance(properties);
    }
}