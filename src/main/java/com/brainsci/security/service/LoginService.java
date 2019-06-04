package com.brainsci.security.service;

import com.brainsci.form.CommonResultForm;
import com.brainsci.security.entity.UserBaseEntity;
import com.brainsci.security.entity.UserEntity;
import com.brainsci.security.exception.LoginException;
import com.brainsci.security.exception.SignUpException;
import com.brainsci.security.form.BaseAuthForm;
import com.brainsci.security.form.LoginRequestForm;
import com.brainsci.security.form.LoginResponForm;
import com.brainsci.security.form.SignUpRequestForm;
import com.brainsci.security.repository.UserBaseRepository;
import com.brainsci.security.util.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service
public class LoginService {
    private final UserService userService;
    private final UserBaseRepository userBaseRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
//    private String activeProfile;
//    private Environment env;

    @Autowired
    public LoginService(UserService userService, UserBaseRepository userBaseRepository, Environment env) {
        this.userService = userService;
        this.userBaseRepository = userBaseRepository;
//        this.env = env;
//        this.activeProfile = env.getProperty("spring.profiles.active", "default");
    }

    public void logout(HttpSession httpSession) {
        logger.info("用户:{}注销", httpSession.getAttribute("username"));
        httpSession.invalidate();
    }

    public void checkVerificationAnswer(LoginRequestForm requestForm, HttpSession httpSession) throws LoginException {
        String verificationAnswer = (String) httpSession.getAttribute("verAnswer");
        httpSession.removeAttribute("verAnswer");
        // 当验证码为空或者验证码不匹配时，抛出异常
        if (verificationAnswer == null || !verificationAnswer.equals(requestForm.getVerification())) {
            throw new LoginException(1, "验证码错误");
        }
    }

    public UserEntity checkPassword(BaseAuthForm requestForm) throws LoginException {
        UserEntity userEntity = userService.findByUsername(requestForm.getUsername());
        if(userEntity == null) userEntity = userService.findByEmail(requestForm.getUsername());
        if (userEntity == null || !userService.checkPlainText(userEntity, requestForm.getPassword())) {
            throw new LoginException(2, "账号或密码错误");
        }
        return userEntity;
    }

    public LoginResponForm loginFromWeb(LoginRequestForm requestForm, HttpSession httpSession) throws LoginException {
        UserEntity userEntity = checkPassword(requestForm);
        return check(userEntity.getUsername(), userEntity, httpSession);
    }

    public LoginResponForm signUpFromWeb(SignUpRequestForm requestForm, HttpSession httpSession) throws SignUpException {
        String username = requestForm.getUsername();
        String password = requestForm.getPassword();
        if (userService.findByUsername(username) != null) throw new SignUpException(2,"This username has been registered!");
        UserEntity userEntity = new UserEntity();
        UserBaseEntity userBaseEntity = new UserBaseEntity();
        userEntity.setUsername(username);
        userBaseEntity.setUsername(username);
        userBaseEntity.setHomeDirectory("./"+username);
        userEntity.setPassword(userService.getCipherText(password));
        userEntity.setName(requestForm.getName());
        userEntity.setAvatarUrl(requestForm.getAvatar());
        userEntity.seteMail(requestForm.getEMail());
        userEntity.setInstitution(requestForm.getInstitution());
        userEntity.setPhone(requestForm.getPhone());
        userEntity.setQq(requestForm.getQq());
        userService.createAccount(userEntity);
        userBaseRepository.save(userBaseEntity);
        return check(username, userEntity, httpSession);
    }

    private LoginResponForm check(String username, UserEntity userEntity, HttpSession httpSession) {
        LoginResponForm loginResponForm = new LoginResponForm(userEntity.getUsername(), userEntity.getAvatarUrl());
        httpSession.setAttribute("username", username);
        String token = TokenManager.createToken(httpSession.getId(), username);
        httpSession.setAttribute("token", token);
        httpSession.setAttribute("nickname", userEntity.getName());
        loginResponForm.setToken(token);
        return loginResponForm;
    }

    public CommonResultForm changePassword(String oldPassword, String newPassword, HttpSession httpSession) throws LoginException {
        UserEntity user = userService.findByUsername((String) httpSession.getAttribute("username"));
        if (userService.changePassword(user, oldPassword, newPassword)) {
            return new CommonResultForm(0, "修改密码成功");
        }
        throw new LoginException(2, "账号密码错误");
    }
}
