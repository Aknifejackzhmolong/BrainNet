package com.brainsci.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class MailUtils {
    @Autowired
    private JavaMailSender mailSender;
    //邮件发件人
    @Value("${mail.fromMail.addr}")
    private String from;

    /**
     * @author zeng
     * @param to 收件人
     * @param subject 主题
     * @param verifyCode 验证码
     */
    public void sendVerifyMail(String to, String subject, String verifyCode) {
        //创建邮件正文
        String emailContent = "您的验证码为"+verifyCode+"\n" +
                "验证码有效时间为30分钟，感谢您对本项目的支持\n" +
                "若不是本人的操作，请忽视此邮件";
        //将模块引擎内容解析成html字符串
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        // 主题
        message.setSubject(subject);
        message.setText(emailContent);
        try {
            mailSender.send(message);
            System.out.println("简单邮件已经发送。");
        } catch (Exception e) {
            System.out.println("发送简单邮件时发生异常！");
            e.printStackTrace();
        }
    }
    public void sendCompleteMail(String to, String username, String model, boolean success) {
        //创建邮件正文
        String emailContent = String.format("尊敬的%s，您在本站(Brain Sci Tools)上运行的%s脚本"+(success?"已经处理完成，请查看\n\n":"处理失败") +
                "感谢您对本项目的支持",username,model);
        //将模块引擎内容解析成html字符串
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        // 主题
        message.setSubject("脚本执行完成");
        message.setText(emailContent);
        try {
            mailSender.send(message);
            System.out.println("简单邮件已经发送。");
        } catch (Exception e) {
            System.out.println("发送简单邮件时发生异常！");
            e.printStackTrace();
        }
    }
}
