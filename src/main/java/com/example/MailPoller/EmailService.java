package com.example.MailPoller;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.search.FlagTerm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Service
public class EmailService {

    @Autowired
    private Session emailSession;

    public void pollEmails(String protocol, String username, String appPassword) throws Exception {
        Store store = emailSession.getStore(protocol);
        store.connect(username, appPassword);

        Folder emailFolder = store.getFolder("INBOX");
        emailFolder.open(Folder.READ_WRITE); // Open in READ_WRITE mode to update flags

        // Fetch only unread messages
        Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        System.out.println("Number of unread messages: " + messages.length);

        for (Message message : messages) {
            System.out.println("Subject: " + message.getSubject());

            try {
                if (message.getContentType().contains("multipart")) {
                    Multipart multipart = (Multipart) message.getContent();
                    processMultipart(multipart, message);
                } else {
                    saveEmailContentAsPdf(message, message.getContent().toString());
                }

                // Mark the message as read
//                message.setFlag(Flags.Flag.SEEN, true);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error processing message: " + e.getMessage());
            }
        }

        emailFolder.close(false);
        store.close();
    }

    private void processMultipart(Multipart multipart, Message message) throws Exception {
        StringBuilder emailContent = new StringBuilder();
        String htmlContent = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                saveAttachment(bodyPart);
            } else {
                String content = getTextFromBodyPart(bodyPart);
                if (bodyPart.isMimeType("text/html")) {
                    htmlContent = content;
                } else {
                    emailContent.append(content).append("\n");
                }
            }
        }
        saveEmailContentAsPdf(message, htmlContent != null ? htmlContent : emailContent.toString());
    }

    private String getTextFromBodyPart(BodyPart bodyPart) throws Exception {
        if (bodyPart.isMimeType("text/*")) {
            return (String) bodyPart.getContent();
        } else if (bodyPart.isMimeType("multipart/alternative")) {
            // Prefer HTML text over plain text
            Multipart multi = (Multipart) bodyPart.getContent();
            String text = null;
            for (int i = 0; i < multi.getCount(); i++) {
                BodyPart part = multi.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    if (text == null) {
                        text = getTextFromBodyPart(part);
                    }
                } else if (part.isMimeType("text/html")) {
                    String html = getTextFromBodyPart(part);
                    return html;
                } else {
                    return getTextFromBodyPart(part);
                }
            }
            return text;
        } else if (bodyPart.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) bodyPart.getContent();
            return getTextFromMultipart(multipart);
        }
        return "";
    }

    private String getTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder emailContent = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            emailContent.append(getTextFromBodyPart(bodyPart)).append("\n");
        }
        return emailContent.toString();
    }

    private void saveAttachment(BodyPart bodyPart) throws Exception {
        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
        InputStream is = mimeBodyPart.getInputStream();

        // Ensure the attachments directory exists
        File attachmentsDir = new File("attachments");
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs();
        }

        File file = new File(attachmentsDir, mimeBodyPart.getFileName());
        System.out.println("Saving attachment to: " + file.getAbsolutePath());

        FileOutputStream fos = new FileOutputStream(file);
        byte[] buf = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buf)) != -1) {
            fos.write(buf, 0, bytesRead);
        }
        fos.close();
        System.out.println("Attachment saved: " + file.getName());
    }

    private void saveEmailContentAsPdf(Message message, String emailContent) throws Exception {
        String pdfFileName = "emails/" + message.getSubject().replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".pdf";

        // Ensure the emails directory exists
        File emailsDir = new File("emails");
        if (!emailsDir.exists()) {
            emailsDir.mkdirs();
        }

        if (message.isMimeType("text/html") || emailContent.trim().startsWith("<html>")) {
            // Convert HTML content to PDF
            HtmlConverter.convertToPdf(emailContent, new PdfWriter(pdfFileName));
        } else {
            // Handle plain text content
            try (PdfWriter writer = new PdfWriter(pdfFileName);
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc)) {
                document.add(new Paragraph("Subject: " + message.getSubject()));
                document.add(new Paragraph("From: " + message.getFrom()[0].toString()));
                document.add(new Paragraph("Date: " + message.getSentDate()));
                document.add(new Paragraph("\n" + emailContent));
            }
        }

        System.out.println("Email content saved as PDF: " + pdfFileName);
    }
}
