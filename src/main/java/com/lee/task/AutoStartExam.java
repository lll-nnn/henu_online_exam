package com.lee.task;

import com.lee.pojo.Exam;
import com.lee.service.ExamService;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

public class AutoStartExam extends QuartzJobBean {

    @Resource
    private HttpServlet httpServlet;

    @Resource
    private ExamService examService;

    @Override
    protected void executeInternal(@NotNull JobExecutionContext context) throws JobExecutionException {
        ServletContext servletContext = httpServlet.getServletContext();
        if (servletContext.getAttribute("exam") != null){
            return;
        }
        Integer examId = (Integer) context.getJobDetail().getJobDataMap().get("id");
        Exam exam = examService.getById(examId);
        servletContext.setAttribute("exam",exam);
        exam.setIsActive(true);
        examService.updateById(exam);
        System.out.println("exam:" + exam.getName() + "is running...");
    }
}
