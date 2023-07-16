package com.lee.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("submit")
public class Submit {

    @TableId
    private Integer id;

    private Integer examId;

    private Integer studentId;

    private Date submitTime;

}
