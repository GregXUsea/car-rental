package com.carrental.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${mail.host}")
    private String host;

    @Value("${mail.port}")
    private int port;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    @Value("${mail.from}")
    private String from;

    /**
     * 发送验证码邮件
     * @return 发送结果，开发模式下返回验证码内容
     */
    public String sendVerificationCode(String toEmail, String code) {
        if (username == null || username.contains("your-email")) {
            log.warn("[邮件] SMTP未配置，验证码为: {} (目标邮箱: {})", code, toEmail);
            return code; // 开发模式，返回验证码
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.ssl.trust", host);
            if (port == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "御途租车", "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("【御途租车】密码重置验证码");
            message.setContent(buildEmailContent(code), "text/html; charset=UTF-8");

            Transport.send(message);
            log.info("[邮件] 验证码已发送至 {}", toEmail);
            return "sent";
        } catch (Exception e) {
            log.error("[邮件] 发送失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String buildEmailContent(String code) {
        return """
            <div style="font-family: 'Microsoft YaHei', Arial; max-width: 500px; margin: 0 auto; padding: 30px; background: #f8f9fa; border-radius: 12px;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <h2 style="color: #1a1a2e; margin: 0;">御途租车</h2>
                    <p style="color: #999; font-size: 13px; margin-top: 4px;">YUTU CAR RENTAL</p>
                </div>
                <div style="background: #fff; padding: 30px; border-radius: 10px; text-align: center;">
                    <p style="font-size: 15px; color: #333; margin-bottom: 20px;">您正在重置密码，验证码如下：</p>
                    <div style="font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px; padding: 16px; background: #f0f2ff; border-radius: 8px; display: inline-block;">%s</div>
                    <p style="font-size: 13px; color: #999; margin-top: 20px;">验证码 <b>5分钟</b> 内有效，请勿泄露给他人</p>
                </div>
                <p style="font-size: 12px; color: #bbb; text-align: center; margin-top: 20px;">如非本人操作，请忽略此邮件</p>
            </div>
            """.formatted(code);
    }
}
