package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String role;

    private String status;

    private String phone;

    private String email;
}
