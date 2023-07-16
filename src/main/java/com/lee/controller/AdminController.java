package com.lee.controller;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.config.GlobalConfig;
import com.lee.pojo.Exam;
import com.lee.pojo.R;
import com.lee.pojo.Teacher;
import com.lee.service.ExamService;
import com.lee.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private GlobalConfig globalConfig;

    @Autowired
    private ExamService examService;

    @PostMapping("/admin_login")
    @ResponseBody
    public R adminLogin(@RequestBody Teacher teacher, HttpSession session){
        R r = new R();
//        System.out.println(teacher);
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
        if (!res.getIsAdmin()){
            r.setSuccess(false);
            r.setMsg("无权限");
            return r;
        }
        r.setSuccess(true);
        session.setAttribute("loginManager", res);
        return r;
    }

    @GetMapping("/admin")
    public String adminPage(HttpServletRequest request, Model model){
        HttpSession session = request.getSession();
        ServletContext context = request.getServletContext();
        Teacher loginManager = (Teacher) session.getAttribute("loginManager");

        model.addAttribute("loginManager", loginManager);
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teacher::getIsAdmin, true);
        List<Teacher> teachers = teacherService.list(wrapper);
        if (teachers.size() == 1 && teachers.get(0).getName().equals("admin")){
            context.setAttribute("init", false);
        }else{
            context.setAttribute("init", true);
        }
        return "manager";
    }

    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session){
        if (session.getAttribute("loginManager") != null){
            session.removeAttribute("loginManager");
        }
        return "redirect:/";
    }

    @GetMapping("/admin/teacher")
    public String adminTeacher(Model model, HttpSession session){
        List<Teacher> teachers = teacherService.list();
        Teacher loginManager = (Teacher) session.getAttribute("loginManager");

        model.addAttribute("teacherList", teachers);
        model.addAttribute("loginManager", loginManager);
        return "manager_teacher";
    }

    @ResponseBody
    @PostMapping("/admin/teacher/changePass")
    public R changePassword(@RequestBody Map<String, String> map, HttpSession session){
        R r = new R();
        String oldPass = "", newPass = "";
        if (map.containsKey("oldPass")){
            oldPass = map.get("oldPass");
        }
        if (map.containsKey("newPass1")){
            newPass = map.get("newPass1");
        }
        Teacher manager = (Teacher) session.getAttribute("loginManager");
        if (!manager.getPassword().equals(oldPass)){
            r.setSuccess(false);
            r.setMsg("旧密码错误");
            return r;
        }
        manager.setPassword(newPass);
        if ( !teacherService.updateById(manager)){
            r.setSuccess(false);
            r.setMsg("内部错误，修改失败");
        };
        r.setSuccess(true);
        r.setMsg("修改成功");
        return r;
    }

    @ResponseBody
    @PostMapping("/admin/teacher/add")
    public R addTeacher(@RequestBody Teacher newTeacher){
        R r = new R();
        if(!teacherService.save(newTeacher)){
            r.setSuccess(false);
            r.setMsg("添加失败");
            return r;
        }
        r.setSuccess(true);
        r.setMsg("添加成功");
        return r;
    }

    @GetMapping("/admin/config")
    public String toConfig(HttpSession session, Model model){
        Teacher loginManager = (Teacher) session.getAttribute("loginManager");

        model.addAttribute("loginManager", loginManager);
        model.addAttribute("config", globalConfig);
        return "manager_config";
    }

    @GetMapping("/admin/exam")
    public String toExam(HttpSession session, Model model){
        Teacher loginManager = (Teacher) session.getAttribute("loginManager");
        List<Exam> examList = examService.list();
        for(Exam exam : examList){
            exam.setTeacherName(teacherService.getById(exam.getTeacherId()).getName());
        }

        model.addAttribute("loginManager", loginManager);
        model.addAttribute("examList", examList);
        return "manager_exam";
    }

    @GetMapping("/admin/teacher/edit/{id}")
    public String toTeacherModify(Model model, @PathVariable("id")Integer id, HttpSession session){
        Teacher teacher = teacherService.getById(id);
        Teacher manager = (Teacher) session.getAttribute("loginManager");
        model.addAttribute("loginManager", manager);
        model.addAttribute("name", teacher.getName());
        model.addAttribute("fullname", teacher.getFullName());
        model.addAttribute("admin", teacher.getIsAdmin());
        return "manager_teacher_modify";
    }

    @PostMapping("/admin/teacher/update")
    @ResponseBody
    public R TeacherUpdate(@RequestBody Teacher teacher){
        R r = new R();
        Integer id = teacher.getId();
        Teacher teacher1 = teacherService.getById(id);
        if (teacher.getPassword().equals(SecureUtil.md5(""))){
            teacher.setPassword(teacher1.getPassword());
        }
        if(!teacherService.updateById(teacher)){
            r.setSuccess(false);
            r.setMsg("内部错误，修改失败");
            return r;
        }
        r.setSuccess(true);
        r.setMsg("修改成功");
        return r;
    }

    @GetMapping("/admin/teacher/delete/{id}")
    public String deleteTeacher(@PathVariable("id")Integer id){
        teacherService.removeById(id);
        return "redirect:/admin/teacher";
    }

    @PostMapping("/admin/config/update")
    @ResponseBody
    public R updateConfig(@RequestBody GlobalConfig config){
        R r = new R();
        globalConfig.setInterval(config.getInterval());
        globalConfig.setCandelete(config.isCandelete());
        globalConfig.setMaxfilesize(config.getMaxfilesize());
        globalConfig.setPagesize(config.getPagesize());
        globalConfig.setMinfilesize(config.getMinfilesize());
        globalConfig.setTimegap(config.getTimegap());
        r.setSuccess(true);
        r.setMsg("修改成功");
        return r;
    }
}
