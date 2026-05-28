package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 返回带业务数据的成功响应。
     * Controller 通常用它把 Service 返回的数据包装成统一 JSON 结构。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 返回不带业务数据的成功响应。
     * 常用于删除、重置密码等只需要表达“操作成功”的接口。
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "success", null);
    }

    /**
     * 返回失败响应。
     * 全局异常处理器会调用它，把异常转换成前端容易处理的 JSON。
     */
    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
