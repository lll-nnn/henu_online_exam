package com.lee.util;

import com.lee.pojo.Exam;
import com.lee.pojo.QuartzBean;
import org.quartz.JobKey;
import org.quartz.Scheduler;

public class AutoStartUtil {

    public static void addAutoExam(Scheduler scheduler, Exam exam){
        QuartzBean quartzBean = new QuartzBean();
        quartzBean.setId(exam.getId());
        quartzBean.setJobName("Exam-" + exam.getId());
        quartzBean.setJobClass("com.lee.task.AutoStartExam");
        quartzBean.setDate(exam.getTime());
        try{
            if (!scheduler.checkExists(JobKey.jobKey(quartzBean.getJobName()))){
                QuartzUtil.createScheduleJob(scheduler, quartzBean);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void updateAutoExam(Scheduler scheduler, Exam exam){
        try{
            QuartzBean quartzBean = new QuartzBean();
            quartzBean.setDate(exam.getTime());
            quartzBean.setId(exam.getId());
            quartzBean.setJobName("Exam-" +  exam.getId());
            if (scheduler.checkExists(JobKey.jobKey(quartzBean.getJobName()))){
                QuartzUtil.updateScheduleJob(scheduler, quartzBean);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void removeAutoExam(Scheduler scheduler,Exam exam){
        try{
            String jobName = "Exam-" +  exam.getId();
            if (scheduler.checkExists(JobKey.jobKey(jobName))){
                QuartzUtil.deleteScheduleJob(scheduler, jobName);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
