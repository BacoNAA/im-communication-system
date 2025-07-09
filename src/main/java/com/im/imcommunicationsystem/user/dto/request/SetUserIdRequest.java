package com.im.imcommunicationsystem.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 设置用户ID请求DTO
 */
public class SetUserIdRequest {

    @NotBlank(message = "用户ID不能为空")
    @Size(min = 3, max = 20, message = "用户ID长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户ID只能包含字母、数字和下划线")
    private String userIdString;

    // 构造函数
    public SetUserIdRequest() {}

    public SetUserIdRequest(String userIdString) {
        this.userIdString = userIdString;
    }

    // Getter和Setter方法
    public String getUserIdString() {
        return userIdString;
    }

    public void setUserIdString(String userIdString) {
        this.userIdString = userIdString;
    }

    // 方法别名，保持向后兼容
    public String getUserIdStr() {
        return userIdString;
    }

    @Override
    public String toString() {
        return "SetUserIdRequest{" +
                "userIdString='" + userIdString + '\'' +
                '}';
    }
}