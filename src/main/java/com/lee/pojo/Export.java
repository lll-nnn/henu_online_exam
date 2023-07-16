package com.lee.pojo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class Export {

    @ExcelProperty("考试")
    private String examName;

    @ExcelProperty("学生姓名")
    private String studentName;

    @ExcelProperty("学号")
    private String studentNumber;

    @ExcelProperty("提交时间")
    private Date submitTime;

}
