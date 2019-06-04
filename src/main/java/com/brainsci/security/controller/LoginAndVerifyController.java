package com.brainsci.security.controller;

import com.brainsci.form.CommonResultForm;
import com.brainsci.security.exception.LoginException;
import com.brainsci.security.exception.SignUpException;
import com.brainsci.security.form.ChangePasswordForm;
import com.brainsci.security.form.LoginRequestForm;
import com.brainsci.security.form.SignUpRequestForm;
import com.brainsci.security.form.VerficationDataForm;
import com.brainsci.security.service.LoginService;
import com.brainsci.utils.MailUtils;
import com.brainsci.security.util.Image2Base64;
import com.brainsci.security.util.LoginVerificationCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.awt.*;
import java.io.IOException;
import java.net.URLDecoder;

@RestController
public class LoginAndVerifyController {

    private final LoginService loginService;
    private final MailUtils mailUtils;

    @Autowired
    public LoginAndVerifyController(LoginService loginService, MailUtils mailUtils) {
        this.loginService = loginService;
        this.mailUtils = mailUtils;
    }

    @PostMapping(value = "/live")
    public CommonResultForm live(HttpSession httpSession) {
        if (httpSession.getAttribute("username") == null) {
            return new CommonResultForm(666, "未登录");
        }
        return CommonResultForm.of204("/live");
    }

    /**
     * 获取验证码，答案放入session内。
     */
    @PostMapping(value = "/verify")
    public CommonResultForm sendVer(HttpSession httpSession) throws IOException, FontFormatException {
        LoginVerificationCode createVer = new LoginVerificationCode();
        byte[] imageByteArray = createVer.getVerificationCode();
        String imageCode;
        imageCode = Image2Base64.getImageString(imageByteArray);
        httpSession.setAttribute("verAnswer", createVer.getResult());
        VerficationDataForm verficationDataForm = new VerficationDataForm(imageCode);
        return new CommonResultForm(0, "已生成验证码", verficationDataForm);
    }
    /**
     * 获取验证码，答案放入session内。
     */
    @GetMapping(value = "/verifyMail")
    public CommonResultForm sendVerMail(@RequestParam String email, HttpSession httpSession) throws IOException, FontFormatException {
        String random = ((int)((Math.random()*9+1)*100000))+"";
        httpSession.setAttribute("verifyCode", random+URLDecoder.decode(email, "UTF-8"));
        mailUtils.sendVerifyMail(URLDecoder.decode(email, "UTF-8"), "Brain Sci Tools", random);
        return CommonResultForm.of204("Verify Code has been generated!");
    }

    /**
     * 登录，进行验证用户名，密码，验证码
     */
    @PostMapping(value = "/login")
    public CommonResultForm login(@RequestBody LoginRequestForm requestForm, HttpSession httpSession) {
        if (null != httpSession.getAttribute("username")) {
            return CommonResultForm.of400("不允许同一浏览器登录多次，请注销后登录");
        }
        if (null == httpSession.getAttribute("verAnswer")) {
            return CommonResultForm.of400("未获取验证码");
        }
        try {
            loginService.checkVerificationAnswer(requestForm, httpSession);
            return CommonResultForm.of200("login successfully", loginService.loginFromWeb(requestForm, httpSession));
        } catch (LoginException e) {
            return CommonResultForm.of400(e.getMessage());
        }
    }
    /**
     * 注册
     */
    @PostMapping(value = "/signup")
    public CommonResultForm signOn(@RequestBody SignUpRequestForm requestForm, HttpSession httpSession) {
        if (null != httpSession.getAttribute("username")) {
            return CommonResultForm.of400("Please log out and try to register");
        }
        try {
            String verifyCode = (String) httpSession.getAttribute("verifyCode");
            if (verifyCode != null&&verifyCode.equals(requestForm.getVerifyCode()+requestForm.getEMail())) return CommonResultForm.of200("registered successfully", loginService.signUpFromWeb(requestForm, httpSession));
            else return CommonResultForm.of400("Registration failed! Verification code error");
        } catch (SignUpException e) {
            return CommonResultForm.of400(e.getMessage());
        }
    }

    @PostMapping(value = "/login-test")
    public CommonResultForm loginTest(@RequestBody LoginRequestForm loginRequestForm, HttpSession httpSession) {
        if (null != httpSession.getAttribute("username")) {
            httpSession.invalidate();
            return CommonResultForm.of400("不允许同一浏览器登录多次，已经注销，请重新登录");
        }
        try {
            return CommonResultForm.of200("login success", loginService.loginFromWeb(loginRequestForm, httpSession));
        } catch (LoginException e) {
            return CommonResultForm.of400(e.getMessage());
        }
    }

    /**
     * 注销，使该session无效。
     */
    @PostMapping(value = "/signout")
    public CommonResultForm logout(HttpSession httpSession) {
        loginService.logout(httpSession);
        return new CommonResultForm(0, "log out", null);
    }

    /**
     * 修改密码
     */
    @PostMapping("/modifyPassword")
    public CommonResultForm changePassword(@RequestBody ChangePasswordForm changePasswordForm, HttpSession httpSession) {
        if (changePasswordForm.getNewPassword() == null || changePasswordForm.getOldPassword() == null) {
            return CommonResultForm.of400("密码格式错误！");
        }
        try {
            return loginService.changePassword(changePasswordForm.getOldPassword(), changePasswordForm.getNewPassword(), httpSession);
        } catch (LoginException e) {
            return CommonResultForm.of400(e.getMessage());
        }
    }
}
