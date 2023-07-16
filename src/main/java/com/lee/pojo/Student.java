package com.lee.pojo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student")
public class Student {

    @TableId
    private Integer id;

    @ExcelProperty(value = "学号", index = 0)
    private String number;

    @ExcelProperty(value = "姓名", index = 1)
    private String name;

    @ExcelProperty(value = "班级", index = 2)
    private String studentClass;

    private Integer examId;

    private Boolean isLogin;

    private Boolean isSubmit;
}
