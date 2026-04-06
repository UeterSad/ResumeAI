package com.example.springai.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;


import com.example.springai.model.UserAccount;
import com.example.springai.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public String addUser(@RequestBody UserAccount userPayload) {
        boolean created = userService.createUser(userPayload);
        return created ? "新增用户成功！" : "用户已存在";
    }

    // 用户登录：账号支持用户名或邮箱。
    @PostMapping("/login")
    public ResponseEntity<UserAccount> login(@RequestBody UserAccount loginPayload) {
        UserAccount authenticatedUser = userService.authenticateByAccount(loginPayload);
        if (authenticatedUser != null) {
            return ResponseEntity.ok(authenticatedUser);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

}