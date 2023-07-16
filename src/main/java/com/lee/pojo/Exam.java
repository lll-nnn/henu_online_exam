package com.lee.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("exam")
public class Exam {

    @TableId
    private Integer id;

    private Integer teacherId;

    @TableField(exist = false)
    private String teacherName;

    private String name;

    private String paper;

    private String type;

    private Date time;

    private Boolean isAutostart;

    private Boolean isActive;

    private Boolean isFinish;

    private Boolean isArchived;

    private Boolean isCleared;

}
