package com.lee.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        if (session.getAttribute("loginManager") == null &&
                session.getAttribute("loginTeacher") == null &&
                session.getAttribute("loginStudent") == null){
            response.sendRedirect("/");
        }else{
            return true;
        }
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        HttpSession session = request.getSession();
//        if (session.getAttribute("loginManager") != null){
//            modelAndView.addObject("loginManager", session.getAttribute("loginManager"));
//        }else if(session.getAttribute("loginTeacher") != null){
//            modelAndView.addObject("loginTeacher", session.getAttribute("loginTeacher"));
//        }else if(session.getAttribute("loginStudent") != null){
//            modelAndView.addObject("loginStudent", session.getAttribute("loginStudent"));
//        }
    }
}
