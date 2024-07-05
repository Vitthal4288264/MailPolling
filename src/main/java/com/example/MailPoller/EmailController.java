package com.example.MailPoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/pollEmails")
    public String pollEmails(@RequestParam String protocol, @RequestParam String username, @RequestParam String password) {
        try {
            emailService.pollEmails(protocol, username, password);
            return "Emails polled and attachments downloaded successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to poll emails: " + e.getMessage();
        }
    }
}