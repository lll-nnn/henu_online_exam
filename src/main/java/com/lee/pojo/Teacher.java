package com.lee.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("teacher")
public class Teacher {

    @TableId
    private Integer id;

    private String name;

    private String password;

    private String fullName;

    private Boolean isAdmin;

}
