package com.lee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "manager.config")
public class GlobalConfig {

    private int interval;

    private int pagesize;

    private int timegap;

    private int minfilesize;

    private int maxfilesize;

    private boolean candelete;

    private String interval_desc;

    private String pagesize_desc;

    private String timegap_desc;

    private String minfilesize_desc;

    private String maxfilesize_desc;

    private String candelete_desc;

}
