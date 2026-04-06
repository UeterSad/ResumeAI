package com.example.springai.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springai.config.UuidConfig;
import com.example.springai.mapper.UserAccountMapper;
import com.example.springai.model.UserAccount;
import com.example.springai.service.UserService;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserAccountMapper userMapper;

    @Autowired
    private UuidConfig.UuidGenerator uuidGenerator;


    @Override
    public boolean createUser(UserAccount user) {
        // 用户名与邮箱任一重复都不允许创建。
        QueryWrapper<UserAccount> duplicateCheckQuery = new QueryWrapper<>();
        duplicateCheckQuery.eq("username", user.getUsername()).or().eq("email", user.getEmail());
        if (userMapper.selectCount(duplicateCheckQuery) > 0) {
            return false;
        }
        user.setId(uuidGenerator.generateUuid32());
        return userMapper.insert(user) > 0;
    }

    @Override
    public UserAccount authenticateByAccount(UserAccount user) {
        // 前端将登录账号放在 email 字段中，可能是邮箱也可能是用户名。
        String account = user.getEmail();
        QueryWrapper<UserAccount> accountQuery = new QueryWrapper<>();
        accountQuery.eq("email", account).or().eq("username", account);
        UserAccount storedUser = userMapper.selectOne(accountQuery);
        return storedUser != null && storedUser.getPassword().equals(user.getPassword()) ? storedUser : null;
    }

    @Override
    public UserAccount findUserById(String id) {
        return userMapper.selectById(id);
    }
}
