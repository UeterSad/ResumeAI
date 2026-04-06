package com.example.springai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springai.model.UserAccount;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}