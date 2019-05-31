package com.brainsci.springsecurity.execption;

import com.aknife.blog.common.ResultEnum;

/**
 * 权限异常
 */
public class ForbiddenException extends RuntimeException{
    private int code = ResultEnum.FORBIDDEN_EXCEPTION.getCode();
    private static String msg = ResultEnum.FORBIDDEN_EXCEPTION.getMsg();

    public ForbiddenException() {
        super(msg);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
