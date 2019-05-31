package com.brainsci.config;

import com.brainsci.springsecurity.JwtAuthenticationFilter;
import com.brainsci.springsecurity.JwtAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Spring Security Config
 * @author Aknife
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    public static final String JWT_TOKEN_HEADER_PARAM = "Authorization";
    public static final String TOKEN_REFRESH_ENTRY_POINT = "/token";
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 使用自定义身份验证组件
        auth.authenticationProvider(new JwtAuthenticationProvider(userDetailsService));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 禁用 csrf, 由于使用的是JWT，我们这里不需要csrf
        http.cors().and().csrf().disable()
                .authorizeRequests()
                // 所有服务均开放权限(即不需要Role)
                .regexMatchers( "/search.*").permitAll()
                .regexMatchers( "/user.*").permitAll()
                .regexMatchers( "/bar.*").permitAll()
                .regexMatchers( "/bar/\\d+/post.*").permitAll()
                // 跨域“预检”请求
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // web jars
                .antMatchers("/webjars/**").permitAll()
                // swagger
                .antMatchers("/swagger-ui.html").permitAll()
                /**
                 * 访问swagger-ui.html时，html页面会发送四个请求：
                 * 1./swagger-resources/configuration/ui   获取页面UI配置信息
                 * 2./swagger-resources
                 * 3./v2/api-docs        获取接口配置信息
                 * 4./swagger-resources/configuration/security     安全配置
                 */
                .antMatchers("/webjars/springfox-swagger-ui/**").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/v2/api-docs").permitAll()
                // 首页和登录页面
                .antMatchers("/").permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/register").permitAll()
                .antMatchers("/article/**").permitAll()
                .antMatchers("/classify/**").permitAll()
                .antMatchers(TOKEN_REFRESH_ENTRY_POINT).permitAll()
                // 其他所有请求需要身份认证
                .anyRequest().authenticated();
        // Spring Security内置退出登录处理器
        http.logout().logoutUrl("/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
        // token验证过滤器
        http.addFilterBefore(new JwtAuthenticationFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

}
