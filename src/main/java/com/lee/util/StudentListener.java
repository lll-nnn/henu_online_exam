package com.lee.util;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.lee.pojo.IpBind;
import com.lee.pojo.Student;
import com.lee.service.IpBindService;
import com.lee.service.StudentService;

import java.util.ArrayList;
import java.util.List;

public class StudentListener implements ReadListener<Student> {

    private static final int BATCH_COUNT = 5;

    private List<Student> studentList = new ArrayList<>(BATCH_COUNT);
    private List<IpBind> ipList = new ArrayList<>(BATCH_COUNT);


    private int examId;

    private final StudentService studentService;

    private final IpBindService ipBindService;

    public StudentListener(StudentService studentService, int examId, IpBindService ipBindService) {
        this.studentService = studentService;
        this.examId = examId;
        this.ipBindService = ipBindService;
    }

    @Override
    public void invoke(Student student, AnalysisContext analysisContext) {
        student.setExamId(examId);
        student.setIsSubmit(false);
        student.setIsLogin(false);
        studentList.add(student);
        if (studentList.size() >= BATCH_COUNT){
            saveData();
            studentList.clear();
            ipList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
    }

    private void saveData(){
        studentService.saveBatch(studentList);
        for (Student student : studentList) {
            IpBind ipBind = new IpBind();
            ipBind.setIp("");
            ipBind.setStudentId(student.getId());
            ipBind.setExamId(examId);
            ipList.add(ipBind);
        }
        ipBindService.saveBatch(ipList);
    }
}
