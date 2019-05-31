package com.brainsci.springsecurity.controller;

import com.aknife.blog.common.ResultBean;
import com.aknife.blog.common.ResultEnum;
import com.aknife.blog.common.ResultHandler;
import com.aknife.blog.entity.User;
import com.aknife.blog.repo.UserRepository;
import com.aknife.blog.security.JwtAuthenticationToken;
import com.aknife.blog.security.JwtUserDetails;
import com.aknife.blog.security.execption.BaseException;
import com.aknife.blog.security.util.PasswordEncoder;
import com.aknife.blog.security.util.SecurityUtils;
import com.aknife.blog.security.vo.LoginBean;
import com.aknife.blog.security.vo.TokenVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/")
public class UserController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;


    @PostMapping(value = "/login")
    ResultBean login(HttpSession session, @RequestBody LoginBean loginBean, HttpServletRequest request){
        String email = loginBean.getEmail();
        String passwd = loginBean.getPasswd();

        User user = userRepository.findUserByEmail(email);

        System.out.println(loginBean);
        System.out.println(user);

        if(user==null)
            throw new BaseException(ResultEnum.LOGIN_ADMIN_NOT_FOUND);

        if (user.getValid()==0) {
            throw new BaseException(ResultEnum.LOGIN_ACCOUNT_FREEZED);
        }

        //session.setAttribute();
        // 系统登录认证

        System.out.println(String.format("rawPass=%s encodePass=%s",passwd,new PasswordEncoder(user.getSalt()).encode(passwd) ));

        if(!new PasswordEncoder(user.getSalt()).encode(passwd).toString().equals(user.getPassword()))
            throw new BaseException(ResultEnum.LOGIN_WRONG_PASSWD);

        JwtAuthenticationToken token = SecurityUtils.login(request,email , passwd, authenticationManager);

        return ResultHandler.ok(new TokenVo(token,user.getEmail()));
    }

    @PutMapping(value = "/register")
    ResultBean register(HttpSession session, @RequestBody User user) {
        assert user.getId()!=null;  //断言，当user.getId()不为null继续执行

        System.out.println("/register："+user);

        String email = user.getEmail();

        User _userRes = userRepository.findUserByEmail(email);

        if(_userRes!=null)
            throw new BaseException(ResultEnum.REGISTER_ACCOUNT_EXISTED);

        String passwd = user.getPassword();
        String salt = user.getName();
        String passwdEncoded = new PasswordEncoder(salt).encode(passwd);
        user.setSalt(salt);
        user.setPassword(passwdEncoded);
        User userRes = userRepository.save(user);
        if(userRes == null)
            throw new BaseException(ResultEnum.FAIL);
        return ResultHandler.ok(user.getEmail());
    }

    @GetMapping(value = "/user/info")
    ResultBean adminInfo(HttpSession session, HttpServletRequest request){
        if(request.getHeader("Authorization")==null)
            throw new BaseException(ResultEnum.AUTHORIZATION_REQUIRED);
        System.out.println("Header:" + request.getHeader("Authorization"));
        System.out.println("JWT:" + SecurityContextHolder.getContext().getAuthentication());

        JwtUserDetails jwtUserDetails =
                (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email = jwtUserDetails.getUsername();

        User user = userRepository.findUserByEmail(email);

        return ResultHandler.ok(user);
    }

}
