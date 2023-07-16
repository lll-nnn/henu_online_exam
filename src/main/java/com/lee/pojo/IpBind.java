package com.lee.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ip_bind")
public class IpBind {

    @TableId
    private Integer id;

    private Integer studentId;

    private Integer examId;

    private String ip;

}
