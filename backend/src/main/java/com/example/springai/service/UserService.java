package com.example.springai.service;

import com.example.springai.model.UserAccount;

public interface UserService {

    /**
     * 创建用户，若用户名或邮箱重复则返回 false。
     */
    boolean createUser(UserAccount user);

    /**
     * 账号登录校验，账号字段支持用户名或邮箱。
     */
    UserAccount authenticateByAccount(UserAccount user);

    /**
     * 根据用户 ID 查询用户信息。
     */
    UserAccount findUserById(String id);
}
