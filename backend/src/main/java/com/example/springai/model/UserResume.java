package com.example.springai.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_resume")

public class UserResume {

    private Long id;

    private String userId;
    private String name;
    private String phone;
    private String email;
    private String avatar;
    private String jobStatus;
    private String jobTitle;
    private String salaryExpectation;
    private String education;
    private String profession;
    private String work;
    private String project;
    private String award;
    private Date updateTime;

}
