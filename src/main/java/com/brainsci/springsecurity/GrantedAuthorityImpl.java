package com.brainsci.springsecurity;

import org.springframework.security.core.GrantedAuthority;

/**
 * 权限封装
 * SecurityContextHolder.getContext()只有两种方法getAuthority, setAuthority均来自这里
 * @author Aknife
 * @date May 1, 2019
 */
public class GrantedAuthorityImpl implements GrantedAuthority {
    private static final long serialVersionUID = 1L;

    private String authority;

    public GrantedAuthorityImpl(String authority) {
        this.authority = authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }

    @Override
    public String toString() {
        return "GrantedAuthorityImpl{" +
                "authority='" + authority + '\'' +
                '}';
    }
}
