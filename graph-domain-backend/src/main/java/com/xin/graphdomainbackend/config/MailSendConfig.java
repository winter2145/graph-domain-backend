package com.xin.graphdomainbackend.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

@Configuration
@ConfigurationProperties(prefix = "spring.mail")
@Data
@Slf4j
public class MailSendConfig {

    // 发件人邮箱
    private String from;

    // 邮件密匙
    private String password;

    // SMTP服务器地址
    private String host;

    // SMTP服务器端口
    private int port;

    public void sendEmail(String toEmail, String generatedCode) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            // 编码邮件主题
            String encodedSubject = MimeUtility.encodeText("图域邮箱验证码", "UTF-8", "B");
            message.setSubject(encodedSubject, "UTF-8");

            String htmlContent = readHTMLFromFile();
            htmlContent = htmlContent.replace(":data=\"verify\"", ":data=\"" + generatedCode + "\"").replace("000000", generatedCode);

            // 设置邮件内容编码
            message.setContent(htmlContent, "text/html;charset=UTF-8");

            Transport.send(message);
            log.info("Sent message successfully to {}", toEmail);
        } catch (MessagingException | IOException e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String readHTMLFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("html/vericode_email.html");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine())!= null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    public void sendReviewEmail(String toEmail, String htmlContent) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            // 编码邮件主题
            String encodedSubject = MimeUtility.encodeText("图域内容审核通知", "UTF-8", "B");
            message.setSubject(encodedSubject, "UTF-8");

            // 设置邮件内容
            message.setContent(htmlContent, "text/html;charset=UTF-8");

            Transport.send(message);
            log.info("审核通知邮件发送成功");
        } catch (MessagingException | IOException e) {
            log.error("审核通知邮件发送失败: {}", e.getMessage(), e);
        }
    }
}

