package com.brainsci.springsecurity;


import com.aknife.blog.entity.User;
import com.aknife.blog.entity.UserRole;
import com.aknife.blog.repo.UserRepository;
import com.aknife.blog.repo.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户登录认证信息查询
 * @author Aknife
 * @date May 1, 2019
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("该用户不存在");
        }

        System.out.println(user);

        // 用户权限列表，根据用户拥有的权限标识与如 @PreAuthorize("hasAuthority('sys:menu:view')") 标注的接口对比，决定是否可以调用接口
        List<UserRole> adminRoleList = userRoleRepository.findUserRoleListByUserEmail(user.getEmail());
        Set<String> permissions = new HashSet<>();
        for (UserRole adminRole:adminRoleList){
            permissions.add(adminRole.getRole().getName());
        }

        System.out.println(permissions);

        List<GrantedAuthority> grantedAuthorities = permissions.stream().map(GrantedAuthorityImpl::new).collect(Collectors.toList());

        System.out.println(String.format("grantedAuthorities=%s",grantedAuthorities));

        return new JwtUserDetails(user.getEmail(), user.getPassword(), user.getSalt(), grantedAuthorities);
    }

}
