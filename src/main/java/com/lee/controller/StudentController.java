package com.lee.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.pojo.*;
import com.lee.service.IpBindService;
import com.lee.service.StudentService;
import com.lee.service.SubmitService;
import com.lee.util.FileDownloadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private IpBindService ipBindService;

    @Autowired
    private SubmitService submitService;

//    private final String basePath = "D:/Users//L--N//Desktop/FILES/all-java/class/exam/target/upload";
    private final String basePath = "/home/exam/upload";

    @ResponseBody
    @PostMapping("/student_login")
    public R studentLogin(@RequestBody Student student, HttpServletRequest request){
        R r = new R();
        ServletContext context = request.getServletContext();
        Exam exam = (Exam) context.getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getExamId, exam.getId());
        List<Student> list = studentService.list(wrapper);
        Student res = null;
        for (Student s : list){
            if (s.getNumber().equals(student.getNumber()) && s.getName().equals(student.getName())){
                res = s;
            }
        }
        if (res == null){
            r.setSuccess(false);
            r.setMsg("学生不在该考试");
            return r;
        }
        LambdaQueryWrapper<IpBind> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(IpBind::getStudentId, res.getId());
        IpBind ipBind = ipBindService.getOne(wrapper1);
        if (!ipBind.getIp().equals("")){
            if (!ipBind.getIp().equals(request.getRemoteAddr())){
                r.setSuccess(false);
                r.setMsg("不能重复登录");
                return r;
            }
        }
        res.setIsLogin(true);
        studentService.updateById(res);
        ipBind.setIp(request.getRemoteAddr());
        ipBindService.updateById(ipBind);
        HttpSession session = request.getSession();
        session.setAttribute("loginStudent", res);
        r.setSuccess(true);
        return r;
    }

    @GetMapping("/student")
    public String student(Model model, HttpSession session){
        ServletContext context = session.getServletContext();
        Exam exam = (Exam) context.getAttribute("exam");
        model.addAttribute("loginStudent", session.getAttribute("loginStudent"));
        model.addAttribute("exam", exam);
        return "student";
    }

    @PostMapping("/student/exam/upload")
    @ResponseBody
    public R uploadPaper(HttpSession session, @RequestParam("answer")MultipartFile file) throws IOException {
        R r = new R();
        Student student = (Student) session.getAttribute("loginStudent");
        String filename = file.getOriginalFilename();
        if (filename == null){
            filename = student.getName()+ "-" + student.getNumber() +".doc";
        }
        if(!filename.substring(filename.lastIndexOf(".")).equals(".doc")){
            r.setSuccess(false);
            r.setMsg("文件类型不正确");
            return r;
        }

        ServletContext context = session.getServletContext();
        Exam exam = (Exam) context.getAttribute("exam");
        Date time = new Date();
        Submit submit = new Submit();
        submit.setStudentId(student.getId());
        submit.setSubmitTime(time);
        submit.setExamId(exam.getId());
        submitService.save(submit);

        String  uploadPath = basePath + "/student/exam-"  + exam.getName() + "-" + exam.getId() + "/"  +
                        "sClass-" + student.getStudentClass() + "/sName-" + student.getName() + "/";

        File f = new File(uploadPath);
        if(!f.exists()){
            boolean mkdirs = f.mkdirs();
            if (!mkdirs){
                throw new IOException("创建文件夹失败");
            }
        }
        try{
            file.transferTo(new File(uploadPath + filename));
        }catch (Exception e){
            e.printStackTrace();
            r.setSuccess(false);
            r.setMsg("上传失败");
            return r;
        }
        student.setIsSubmit(true);
        studentService.updateById(student);
        r.setSuccess(true);
        r.setMsg("上传成功");
        return r;
    }

    @GetMapping("/student/exam/listdir")
    public String toListDir(HttpSession session, Model model){
        List<Map<String, String>> res = new ArrayList<>();
        Student student = (Student) session.getAttribute("loginStudent");
        ServletContext context = session.getServletContext();
        Exam exam = (Exam) context.getAttribute("exam");
        String  uploadPath = basePath + "/student/exam-"  + exam.getName() + "-" + exam.getId() + "/"  +
                "sClass-" + student.getStudentClass() + "/sName-" + student.getName() + "/";
        File dir = new File(uploadPath);
        if (dir.exists()){
            File[] files = dir.listFiles();
            if (files != null){
                for (File file : files){
                    Map<String, String> map = new HashMap<>();
                    map.put("name", file.getName());
                    Date date = new Date(file.lastModified());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    map.put("time", sdf.format(date));
                    map.put("size", file.length() + "");
                    res.add(map);
                }
            }

        }
        model.addAttribute("exam", exam);
        model.addAttribute("loginStudent", session.getAttribute("loginStudent"));
        model.addAttribute("fileList", res);
        return "student_listdir";
    }

    @GetMapping("/student/exam/download/")
    public void downloadPaper(HttpSession session, HttpServletResponse response){
        Exam exam = (Exam) session.getServletContext().getAttribute("exam");
        String path = exam.getPaper();
        FileDownloadUtil.downloadFile(path, response);
    }

}
