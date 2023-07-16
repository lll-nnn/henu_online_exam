package com.lee.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class QuartzBean {

    private Integer id;

    private String jobName;

    private String jobClass;

    private Integer status;

    private Date date;

}
