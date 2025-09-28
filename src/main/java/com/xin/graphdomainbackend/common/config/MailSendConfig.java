package com.xin.graphdomainbackend.common.config;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "spring.mail")
@Data
@Slf4j
public class MailSendConfig {

    /** 发件人邮箱（spring.mail.username 自动绑定到这里） */
    private String username;

    /** 邮件密码（授权码，不是邮箱登录密码） */
    private String password;

    /** SMTP 服务器 */
    private String host;

    /** 端口 */
    private int port;

    /** 管理员邮箱 */
    private String admin;

    @Resource
    private JavaMailSender mailSender;

    /**
     * 发送验证码邮件
     */
    public void sendEmail(String to, String generatedCode) {
        String htmlContent = loadTemplate("html/vericode_email.html")
                .replace("000000", generatedCode)
                .replace(":data=\"verify\"", ":data=\"" + generatedCode + "\"");
        send(to, "图域邮箱验证码", htmlContent);
    }

    /**
     * 发送审核通知邮件
     */
    public void sendReviewEmail(String to, String htmlContent) {
        send(to, "图域内容审核通知", htmlContent);
    }

    /**
     * 通用发送方法
     */
    public void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(username);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true 表示 HTML 格式
            mailSender.send(message);
            log.info("📧 邮件发送成功 -> {}", to);
        } catch (MessagingException e) {
            log.error("❌ 邮件发送失败 -> {}, 错误: {}", to, e.getMessage(), e);
        }
    }

    /**
     * 读取 HTML 模板
     */
    private String loadTemplate(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取邮件模板失败: " + path, e);
        }
    }
}
