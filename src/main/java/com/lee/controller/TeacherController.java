package com.lee.controller;

import cn.hutool.core.util.ZipUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.config.GlobalConfig;
import com.lee.pojo.*;
import com.lee.service.*;
import com.lee.util.AutoStartUtil;
import com.lee.util.FileDownloadUtil;
import com.lee.util.MyWebSocket;
import com.lee.util.StudentListener;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Controller
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private ExamService examService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private GlobalConfig globalConfig;

    @Autowired
    private IpBindService ipBindService;

    @Autowired
    private SubmitService submitService;

    @Resource
    private Scheduler scheduler;

//    public final String basePath = "D:/Users//L--N//Desktop/FILES/all-java/class/exam/target/upload";
    public final String basePath = "/home/exam/upload";

    @PostMapping("/teacher_login")
    @ResponseBody
    public R teacherLogin(@RequestBody Teacher teacher, HttpSession session){
        R r = new R();
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teacher::getName, teacher.getName());
        Teacher res = teacherService.getOne(wrapper);
        if (res == null){
            r.setSuccess(false);
            r.setMsg("用户不存在");
            return r;
        }
        if (!teacher.getPassword().equals(res.getPassword())){
            r.setSuccess(false);
            r.setMsg("密码错误");
            return r;
        }
        r.setSuccess(true);
        session.setAttribute("loginTeacher", res);
        return r;
    }

    @GetMapping("/teacher/logout")
    public String teacherLogout(HttpSession session){
        session.removeAttribute("loginTeacher");
        return "redirect:/";
    }

    @GetMapping("/teacher")
    public String teacherIndex(HttpSession session, Model model){
        Teacher loginTeacher = (Teacher) session.getAttribute("loginTeacher");
        model.addAttribute("loginTeacher", loginTeacher);
        return "teacher";
    }

    @PostMapping("/teacher/changePass")
    @ResponseBody
    public R changePass(@RequestBody Map<String, String> map, HttpSession session){
        R r = new R();
        Teacher loginTeacher = (Teacher) session.getAttribute("loginTeacher");
        String oldPass = "", newPass = "";
        if (map.containsKey("oldPass")){
            oldPass = map.get("oldPass");
        }
        if (map.containsKey("newPass")){
            newPass = map.get("newPass");
        }
        if(!loginTeacher.getPassword().equals(oldPass)){
            r.setSuccess(false);
            r.setMsg("原密码错误");
            return r;
        }
        loginTeacher.setPassword(newPass);
        if(!teacherService.updateById(loginTeacher)){
            r.setSuccess(false);
            r.setMsg("内部错误，修改失败");
            return r;
        }
        r.setSuccess(true);
        r.setMsg("密码修改成功");
        return r;
    }

    @PostMapping("/teacher/addExam")
    @ResponseBody
    public R addExam(@RequestBody Exam exam, HttpSession session) throws SchedulerException {
        R r = new R();
        Teacher loginTeacher = (Teacher) session.getAttribute("loginTeacher");
        System.out.println(exam.getTime());
        exam.setPaper("");
        exam.setType("");
        exam.setTeacherId(loginTeacher.getId());
        exam.setIsActive(false);
        exam.setIsFinish(false);
        exam.setIsCleared(false);
        exam.setIsArchived(false);
        if(!examService.save(exam)) {
            r.setSuccess(false);
            r.setMsg("内部错误，添加失败");
            return r;
        }
        if (exam.getIsAutostart()){
            AutoStartUtil.addAutoExam(scheduler, exam);
        }else if(scheduler.checkExists(JobKey.jobKey(exam.getName()+"-"+exam.getId()))){
            AutoStartUtil.removeAutoExam(scheduler,exam);
        }
        r.setSuccess(true);
        r.setMsg("添加成功");
        return r;
    }

    @GetMapping("/teacher/exam/modify/{id}")
    public String toModifyExam(@PathVariable("id") Integer id, Model model, HttpSession session){
        Teacher loginTeacher = (Teacher) session.getAttribute("loginTeacher");
        Exam exam = examService.getById(id);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getExamId, exam.getId());
        List<Student> studentList = studentService.list(wrapper);
        Date now = new Date();
        long timeGap = (exam.getTime().getTime() - now.getTime()) / (1000 * 60);
        boolean canStart = false;
        if (timeGap < globalConfig.getTimegap()){
            canStart = true;
        }
        model.addAttribute("canStart", canStart);
        model.addAttribute("exam",exam);
        model.addAttribute("loginTeacher",loginTeacher);
        model.addAttribute("studentCount", studentList.size());
        return "teacher_exam_modify";
    }

    @PostMapping("/teacher/updateExam")
    @ResponseBody
    public R updateExam(@RequestBody Exam exam) throws SchedulerException {
        R r = new R();
        Exam examById = examService.getById(exam.getId());
        examById.setName(exam.getName());
        examById.setTime(exam.getTime());
        examById.setIsAutostart(exam.getIsAutostart());
        if (!examService.updateById(examById)){
            r.setSuccess(false);
            r.setMsg("内部错误，修改失败");
            return r;
        }
        String jobName = "Exam-" + examById.getId();
        if (scheduler.checkExists(JobKey.jobKey(jobName))){
            AutoStartUtil.updateAutoExam(scheduler, examById);
        }else if (examById.getIsAutostart()){
            AutoStartUtil.addAutoExam(scheduler, examById);
        }
        r.setSuccess(true);
        r.setMsg("修改成功");
        return r;
    }

    @ResponseBody
    @PostMapping("/teacher/exam/upload")
    public R examUpload(@RequestParam("id")Integer id, @RequestParam("paper") MultipartFile file, HttpSession session){
        R r = new R();
        Exam exam = examService.getById(id);
        String fileName = exam.getName()+"-"+exam.getId()+".doc";
        if (file.getSize() > globalConfig.getMaxfilesize()){
            r.setSuccess(false);
            r.setMsg("文件过大");
            return r;
        }
        if (file.getSize() < globalConfig.getMinfilesize()){
            r.setSuccess(false);
            r.setMsg("文件过小");
            return r;
        }
        String uploadPath = basePath + "/paper";
        File dest = new File(uploadPath);
        if (!dest.exists()){
            boolean mkdirs = dest.mkdirs();
            if(!mkdirs){
                r.setSuccess(false);
                r.setMsg("创建文件夹失败");
                return r;
            }
        }
        String filePath = uploadPath+"/"+fileName;
        try {
            file.transferTo(new File(filePath));
        }catch (Exception e){
            e.printStackTrace();
            r.setSuccess(false);
            r.setMsg("文件上传失败");
            return r;
        }
        exam.setPaper(filePath);
        examService.updateById(exam);
        r.setSuccess(true);
        return r;
    }

    @GetMapping("/teacher/exam/download/{id}")
    public void downloadPaper(@PathVariable("id")int id, HttpServletResponse response) throws IOException {
        Exam exam = examService.getById(id);
        String path = exam.getPaper();
        FileDownloadUtil.downloadFile(path, response);
    }

    @GetMapping("/teacher/exam/before")
    public String toExamBefore(HttpSession session, Model model){
        Teacher loginTeacher = (Teacher)session.getAttribute("loginTeacher");
        List<Exam> examList = examService.list();
        for(Exam exam : examList){
            exam.setTeacherName(teacherService.getById(exam.getTeacherId()).getName());
        }
        model.addAttribute("loginTeacher", loginTeacher);
        model.addAttribute("examList", examList);
        return "teacher_exam_before";
    }

    @GetMapping("/teacher/manage/summary")
    public String toSummary(HttpSession session, Model model){
        Teacher loginTeacher = (Teacher) session.getAttribute("loginTeacher");
        ServletContext context = session.getServletContext();
        if (context.getAttribute("exam") == null){
            model.addAttribute("loginTeacher", loginTeacher);
            return "teacher_manage_summary";
        }
        Exam nowExam = (Exam) context.getAttribute("exam");
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getExamId, nowExam.getId());
        List<Student> studentList = studentService.list(studentWrapper);
        int total = studentList.size(), loginStudents = 0, submitted = 0;
        for (Student student : studentList){
            if (student.getIsLogin() != null && student.getIsLogin()){
                loginStudents++;
            }
            if (student.getIsSubmit() != null && student.getIsSubmit()){
                submitted++;
            }
        }
        model.addAttribute("loginTeacher", loginTeacher);
        model.addAttribute("total", total);
        model.addAttribute("loginStudents", loginStudents);
        model.addAttribute("unloginStudents", total-loginStudents);
        model.addAttribute("submitted", submitted);
        model.addAttribute("unsubmitted", total - submitted);
        return "teacher_manage_summary";
    }

    @GetMapping("/teacher/manage/showbind")
    public String toShowBind(Model model, HttpSession session){
        ServletContext servletContext = session.getServletContext();
        if (servletContext.getAttribute("exam") == null){
            model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
            return "teacher_manage_showbind";
        }
        Exam nowExam = (Exam) servletContext.getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getIsLogin, true).eq(Student::getExamId, nowExam.getId());
        List<Student> ipBinds = studentService.list(wrapper);
        model.addAttribute("bindlist", ipBinds);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        return "teacher_manage_showbind";
    }

    @GetMapping("/teacher/manage/showunbind")
    public String toShowUnbind(HttpSession session, Model model){
        ServletContext context = session.getServletContext();
        if (context.getAttribute("exam") == null){
            model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
            return "teacher_manage_showunbind";
        }
        Exam exam = (Exam) context.getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getIsLogin, false).eq(Student::getExamId, exam.getId());
        List<Student> studentList = studentService.list(wrapper);
        model.addAttribute("stulist", studentList);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        return "teacher_manage_showunbind";
    }

    @GetMapping("/teacher/manage/showsubmit")
    public String toShowSubmit(HttpSession session, Model model){
        ServletContext context = session.getServletContext();
        if (context.getAttribute("exam") == null){
            model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
            return "teacher_manage_showsubmit";
        }
        Exam exam = (Exam) context.getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getIsSubmit, true).eq(Student::getExamId, exam.getId());
        List<Student> studentList = studentService.list(wrapper);
        model.addAttribute("sublist", studentList);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        return "teacher_manage_showsubmit";
    }

    @GetMapping("/teacher/manage/showunsubmit")
    public String toShowUnSubmit(HttpSession session, Model model){
        ServletContext context = session.getServletContext();
        if (context.getAttribute("exam") == null){
            model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
            return "teacher_manage_showunsubmit";
        }
        Exam exam = (Exam) context.getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getIsSubmit, false).eq(Student::getExamId, exam.getId());
        List<Student> studentList = studentService.list(wrapper);
        model.addAttribute("sublist", studentList);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        return "teacher_manage_showunsubmit";
    }

        @PostMapping("/teacher/exam/start")
    public String examActive(@RequestParam("id")int examId, HttpSession session) throws SchedulerException {
        Exam exam = examService.getById(examId);
        ServletContext context = session.getServletContext();
        context.setAttribute("exam", exam);
        exam.setIsActive(true);
        examService.updateById(exam);
        if(scheduler.checkExists(JobKey.jobKey("Exam-"+exam.getId()))){
            AutoStartUtil.removeAutoExam(scheduler, exam);
        }
        return "redirect:/teacher/exam/modify/"+examId;
    }

    @GetMapping("/teacher/student/{eid}/{currentPage}")
    public String toAddStudent(@PathVariable("eid")int examId, HttpSession session,
                               @PathVariable("currentPage")int currentPage, Model model){
        IPage<Student> page = new Page<>(currentPage, globalConfig.getPagesize());
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getExamId, examId);
        studentService.page(page, wrapper);

        model.addAttribute("page", page);
        model.addAttribute("examId", examId);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("config", globalConfig);
        return "teacher_exam_student";
    }

    @PostMapping("/teacher/student/changePageSize")
    public String changePageSize(@RequestParam("pageSize")int pageSize, @RequestParam("examId") int examId){
        globalConfig.setPagesize(pageSize);
        return "redirect:/teacher/student/" + examId + "/1";
    }

    @PostMapping("/teacher/student/changePage")
    public String toPage(@RequestParam("pageNo")int pageNo, @RequestParam("eid") int examId,
                         @RequestParam("pages") int pages) {
        if (pageNo > pages)pageNo = pages;
        return "redirect:/teacher/student/" + examId + "/" + pageNo;
    }

    @PostMapping("/teacher/student/insert")
    @ResponseBody
    public R insertStudent(@RequestParam Map<String, Object> map){
        R r = new R();
        Student student = new Student();
        student.setStudentClass(map.get("studentClass").toString());
        student.setExamId(Integer.parseInt(map.get("examId").toString()));
        student.setName(map.get("name").toString());
        student.setNumber(map.get("number").toString());
        student.setIsLogin(false);
        student.setIsSubmit(false);
        studentService.save(student);
        IpBind ipBind = new IpBind();
        ipBind.setStudentId(student.getId());
        ipBind.setExamId(student.getExamId());
        ipBind.setIp("");
        ipBindService.save(ipBind);
        r.setSuccess(true);
        return r;
    }

    @GetMapping("/teacher/student/delete/{examId}/{stuId}")
    public String deleteStudent(@PathVariable("examId")int examId, @PathVariable("stuId")int stuId){
        studentService.removeById(stuId);
        return "redirect:/teacher/student/" + examId + "/1";
    }

    //todo test
    @PostMapping("/teacher/student/upload")
    @ResponseBody
    public R uploadStudent(@RequestParam("students") MultipartFile file, @RequestParam("eid")int examId) throws IOException {
        R r = new R();
        EasyExcel.read(file.getInputStream(), Student.class, new StudentListener(studentService,examId, ipBindService)).sheet().doRead();
        r.setSuccess(true);
        return r;
    }

    @GetMapping("/teacher/manage/student")
    public String manageStudent(HttpSession session, Model model){
        Exam cur = (Exam) session.getServletContext().getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getExamId, cur.getId());
        List<Student> students = studentService.list(wrapper);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("studentList", students);
        return "teacher_manage_student";
    }

    @PostMapping("/teacher/manage/student/search")
    public String searchStudent(@RequestParam Map<String, String> param, HttpSession session, Model model){
        Exam exam = (Exam) session.getServletContext().getAttribute("exam");
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("studentList", getSearchStudent(param, exam));
        return "teacher_manage_student";
    }

    public List<Student> getSearchStudent(Map<String, String> map, Exam exam){
        Student student = new Student();
        if (map.get("number") != null){
            student.setNumber(map.get("number"));
        }
        if (map.get("name") != null){
            student.setName(map.get("name"));
        }
        if (map.get("studentClass") != null){
            student.setStudentClass(map.get("studentClass"));
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(student.getNumber()!= null && !student.getNumber().isEmpty(), Student::getNumber, student.getNumber())
                .eq(student.getName()!= null &&!student.getName().isEmpty(), Student::getName, student.getName())
                .eq(student.getStudentClass()!=null &&!student.getStudentClass().isEmpty(), Student::getStudentClass, student.getStudentClass())
                .eq(Student::getExamId, exam.getId());
        return studentService.list(wrapper);
    }

    @GetMapping("/teacher/manage/unlock")
    public String unlockStudent(HttpSession session, Model model){
        Exam exam = (Exam) session.getServletContext().getAttribute("exam");
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getIsLogin, true).eq(Student::getExamId, exam.getId());
        List<Student> list = studentService.list(wrapper);
        List<Map<String, String>> students = getRes(list);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("studentList", students);
        return "teacher_manage_unlock";
    }

    @GetMapping("/teacher/manage/binding/delete/{bindId}")
    public String deleteBinding(HttpSession session, Model model, @PathVariable("bindId") Integer bindId){
        IpBind bind = ipBindService.getById(bindId);
        Student student = studentService.getById(bind.getStudentId());
        student.setIsLogin(false);
        bind.setIp("");
        studentService.updateById(student);
        ipBindService.updateById(bind);
        return "redirect:/teacher/manage/unlock";
    }

    @PostMapping("/teacher/manage/binding/search")
    public String searchBinding(HttpSession session, Model model, @RequestParam Map<String, String> param){
        Exam exam = (Exam) session.getServletContext().getAttribute("exam");
        List<Student> students = getSearchStudent(param, exam);
        List<Map<String, String>> res = getRes(students);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("studentList", res);
        return "teacher_manage_unlock";
    }

    @PostMapping("/teacher/manage/binding/searchip")
    public String searchIPBinding(HttpSession session, Model model, @RequestParam("ip")String ip){
        LambdaQueryWrapper<IpBind> ipWrapper = new LambdaQueryWrapper<>();
        Exam exam = (Exam) session.getServletContext().getAttribute("exam");
        ipWrapper.eq(IpBind::getIp, ip);
        List<IpBind> list = ipBindService.list(ipWrapper);
        List<Map<String,String>> res = new ArrayList<>();
        for (IpBind ipBind : list){
            Map<String,String> map = new HashMap<>();
            map.put("ip", ipBind.getIp());
            map.put("bindId", ipBind.getId()+"");
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getExamId, exam.getId()).eq(Student::getId, ipBind.getStudentId());
            Student student = studentService.getById(wrapper);
            map.put("name", student.getName());
            map.put("studentClass", student.getStudentClass());
            map.put("number", student.getNumber());
            res.add(map);
        }
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("studentList", res);
        return "teacher_manage_unlock";
    }

    @GetMapping("/teacher/manage/notify")
    public String toNotify(HttpSession session, Model model){
        ServletContext context = session.getServletContext();
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        if (context.getAttribute("noticeList") != null){
            model.addAttribute("noticeList", context.getAttribute("noticeList"));
        }
        return "teacher_manage_notify";
    }

    @PostMapping("/teacher/manage/notify/add")
    public  String addNotice(HttpSession session, Model model, @RequestParam("notice")String notice){
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        ServletContext context = session.getServletContext();
        List<String> noticeList;
        if (context.getAttribute("noticeList")== null){
            noticeList = new ArrayList<String>();
        }else{
            noticeList = (List<String>) context.getAttribute("noticeList");
        }
        noticeList.add(notice);
        context.setAttribute("noticeList", noticeList);
        model.addAttribute("noticeList", noticeList);

        MyWebSocket.broadcast();

        return "teacher_manage_notify";
    }

    @GetMapping("/teacher/manage/notify/del/{index}")
    public String delNotice(HttpSession session, Model model, @PathVariable("index")int index){
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        ServletContext context = session.getServletContext();
        List<String> noticeList = (List<String>) context.getAttribute("noticeList");
        noticeList.remove(index);
        context.setAttribute("noticeList", noticeList);
        model.addAttribute("noticeList", noticeList);

        MyWebSocket.broadcast();

        return "teacher_manage_notify";
    }

    @GetMapping("/teacher/exam/after")
    public String toAfterExam(HttpSession session, Model model){
        List<Exam> examList = examService.list();
        for (Exam exam : examList){
            exam.setTeacherName(teacherService.getById(exam.getTeacherId()).getName());
        }
        model.addAttribute("config", globalConfig);
        model.addAttribute("loginTeacher", session.getAttribute("loginTeacher"));
        model.addAttribute("examList", examList);
        return "teacher_exam_after";
    }

    @GetMapping("/teacher/exam/finish/{id}")
    public String finishExam(HttpSession session, @PathVariable("id")int id){
        Exam exam = examService.getById(id);
        exam.setIsActive(false);
        exam.setIsFinish(true);
        ServletContext context = session.getServletContext();
        if (context.getAttribute("exam") != null){
            context.removeAttribute("exam");
        }
        if (context.getAttribute("noticeList") != null){
            context.removeAttribute("noticeList");
        }
        examService.updateById(exam);
        return "redirect:/teacher/exam/after";
    }

    @GetMapping("/teacher/exam/clear/{id}")
    public String clearExam(HttpSession session, @PathVariable("id")int id) throws IOException {
        Exam exam = examService.getById(id);
        exam.setIsCleared(true);
        examService.updateById(exam);
        LambdaQueryWrapper<Student> sWrapper = new LambdaQueryWrapper<>();
        sWrapper.eq(Student::getExamId, exam.getId());
        studentService.remove(sWrapper);
        LambdaQueryWrapper<IpBind> ipWrapper = new LambdaQueryWrapper<>();
        ipWrapper.eq(IpBind::getExamId, exam.getId());
        ipBindService.remove(ipWrapper);
        LambdaQueryWrapper<Submit> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submit::getExamId, exam.getId());
        submitService.remove(subWrapper);

        String  dir = basePath + "/student/exam-"  + exam.getName() + "-" + exam.getId() + "/";
        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        return "redirect:/teacher/exam/after";
    }

    @GetMapping("/teacher/exam/delete/{id}")
    public String examDelete(@PathVariable("id") int id) {
        examService.removeById(id);
        return "redirect:/teacher/exam/after";
    }

    @GetMapping("/teacher/exam/export/{id}")
    public void exportExam(@PathVariable("id") int id, HttpServletResponse response) throws IOException {
        Exam exam = examService.getById(id);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = "exam-" + exam.getName();
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), Export.class).autoCloseStream(false).sheet("sheet1")
                .doWrite(getData(id));
    }

    @GetMapping("/teacher/exam/zip/{id}")
    public void zipExam(@PathVariable("id") int id, HttpServletResponse response) throws IOException {
        Exam exam = examService.getById(id);
        String  dir = basePath + "/student/exam-"  + exam.getName() + "-" + exam.getId() + "/";
        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=exam-" + exam.getName() + ".zip");
        String des = basePath + "/zip/exam-" + exam.getName() + ".zip";
        File dirFile = new File(dir);
        if (!dirFile.exists()){
            boolean mkdirs = dirFile.mkdirs();
            if (!mkdirs){
                throw new RuntimeException("文件夹创建失败");
            }
        }
        File zip = ZipUtil.zip(dir, des, true);
        try(InputStream inputStream = Files.newInputStream(zip.toPath())){
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buff))!= -1){
                outputStream.write(buff, 0, len);
                outputStream.flush();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        exam.setIsArchived(true);
        examService.updateById(exam);
    }

    private List<Export> getData(int id){
        List<Export> res = new ArrayList<>();
        LambdaQueryWrapper<Submit> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submit::getExamId, id);
        List<Submit> list = submitService.list(subWrapper);
        for (Submit submit : list){
            Export export = new Export();
            export.setSubmitTime(submit.getSubmitTime());
            export.setExamName(examService.getById(submit.getExamId()).getName());
            Student student = studentService.getById(submit.getStudentId());
            export.setStudentName(student.getName());
            export.setStudentNumber(student.getNumber());
            res.add(export);
        }
        return res;
    }

    private List<Map<String, String>> getRes( List<Student> list){
        List<Map<String, String>> students = new ArrayList<>();
        if (list == null || list.size() == 0){
            return students;
        }
        for (Student student : list) {
            Map<String, String> map = new HashMap<>();
            map.put("number", student.getNumber());
            map.put("name", student.getName());
            map.put("studentClass", student.getStudentClass());
            LambdaQueryWrapper<IpBind> ipWrapper = new LambdaQueryWrapper<>();
            ipWrapper.eq(IpBind::getStudentId, student.getId());
            IpBind one = ipBindService.getOne(ipWrapper);
            map.put("ip", one.getIp());
            map.put("bindId", one.getId()+"");
            students.add(map);
        }
        return students;
    }
}
