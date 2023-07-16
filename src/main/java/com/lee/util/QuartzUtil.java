package com.lee.util;

import com.lee.pojo.QuartzBean;
import org.quartz.*;

import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzUtil {
    /**
     * 创建定时任务 定时任务创建之后默认启动状态
     * @param scheduler   调度器
     * @param quartzBean  定时任务信息类
     * @throws Exception
     */
    public static void createScheduleJob(Scheduler scheduler, QuartzBean quartzBean){
        try {
            //获取到定时任务的执行类  必须是类的绝对路径名称
            //定时任务类需要是job类的具体实现 QuartzJobBean是job的抽象类。
            Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(quartzBean.getJobClass());
            // 构建定时任务信息
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .usingJobData("id", quartzBean.getId())
                    .withIdentity(quartzBean.getJobName()).build();
            // 构建触发器trigger
            SimpleTrigger simpleTrigger = (SimpleTrigger) newTrigger()
                            .withIdentity(quartzBean.getJobName())
                                    .startAt(quartzBean.getDate()).build();
            scheduler.scheduleJob(jobDetail, simpleTrigger);
        } catch (ClassNotFoundException e) {
            System.out.println("定时任务类路径出错：请输入类的绝对路径");
        } catch (SchedulerException e) {
            System.out.println("创建定时任务出错："+e.getMessage());
        }
    }

    /**
     * 更新定时任务
     * @param scheduler   调度器
     * @param quartzBean  定时任务信息类
     * @throws SchedulerException
     */
    public static void updateScheduleJob(Scheduler scheduler, QuartzBean quartzBean)  {
        try {
            //获取到对应任务的触发器
            TriggerKey triggerKey = TriggerKey.triggerKey(quartzBean.getJobName());
            //重新构建任务的触发器
            SimpleTrigger trigger1 = (SimpleTrigger) scheduler.getTrigger(triggerKey);
            trigger1 = trigger1.getTriggerBuilder()
                    .withIdentity(triggerKey).startAt(quartzBean.getDate()).build();
            //重置对应的job
            scheduler.rescheduleJob(triggerKey, trigger1);
        } catch (SchedulerException e) {
            System.out.println("更新定时任务出错："+e.getMessage());
        }
    }

    /**
     * 根据定时任务名称从调度器当中删除定时任务
     * @param scheduler 调度器
     * @param jobName   定时任务名称
     * @throws SchedulerException
     */
    public static void deleteScheduleJob(Scheduler scheduler, String jobName) {
        JobKey jobKey = JobKey.jobKey(jobName);
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            System.out.println("删除定时任务出错："+e.getMessage());
        }
    }
}
