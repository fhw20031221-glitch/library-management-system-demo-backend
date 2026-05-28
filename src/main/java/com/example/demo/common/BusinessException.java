package com.example.demo.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int status;

    /**
     * 创建默认 400 状态的业务异常。
     * 适合参数不合理、状态不允许等常规业务错误。
     */
    public BusinessException(String message) {
        this(400, message);
    }

    /**
     * 创建指定 HTTP 状态码的业务异常。
     * 例如资源不存在用 404，权限问题用 403，登录失败用 401。
     */
    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }
}
