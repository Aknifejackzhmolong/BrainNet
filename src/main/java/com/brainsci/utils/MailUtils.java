package com.brainsci.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class MailUtils {
    public static MailUtils currentMail;
    @Autowired
    private JavaMailSender mailSender;
    //邮件发件人
    @Value("${mail.fromMail.addr}")
    private String from;

    private final String TITLE =  "Brain Sci Tools";

    public MailUtils() {
        MailUtils.currentMail = this;
    }

    /**
     * @author zeng
     * @param to 收件人
     * @param subject 主题
     * @param verifyCode 验证码
     */
    public void sendVerifyMail(String to, String subject, String verifyCode) {
        //创建邮件正文
        String emailContent = "Hello!\n"+
                "Thank you for creating your BrainSci Tools Account. \n\n"+
                "Verification code is "+verifyCode+"\n\n" +
                "The verification code is valid in 30 minutes. \n" +
                "If it is not your operation, please ignore this email. "+
                "\n\nYours truly, \n" +
                "BrainSci Tools Team";
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
    public void sendCompleteMail(String to, String username, String model, String taskname, boolean success) {
        //创建邮件正文
        String emailContent = String.format("Dear %s: \n" +
                "\tThe %s data which task name is '%s' are processed " +
                (success?"successfully. \n" +
                        "you can download data on BrainSci Tools site. \n" :
                        "fail") +
                "\n\nYours truly, \n" +
                "BrainSci Tools Team",username,model,taskname);
        //将模块引擎内容解析成html字符串
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        // 主题
        message.setSubject(TITLE);
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
