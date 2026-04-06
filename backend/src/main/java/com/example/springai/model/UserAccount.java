package com.example.springai.model;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_account")
public class UserAccount {

    private String id;
    private String username;
    private String password;
    private String email;
    private String phone;
    /**
     * 用户类型: 0-普通用户, 1-管理员
     */
    private Integer userType;
}
