package com.secsbrain.frame.task.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.secsbrain.frame.task.ConsoleManager;
import com.secsbrain.frame.task.common.ErrorCodeEnum;
import com.secsbrain.frame.task.core.TaskDefine;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/taskSchedule"}, loadOnStartup = 1)
public class HomeServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(HomeServlet.class);

    private void write(HttpServletResponse response,Object result) throws IOException{
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(200);

        PrintWriter out = response.getWriter();
        out.println(JSON.toJSONString(result));
        out.flush();
        out.close();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Home Get Action!");
        try {
            String contextPath = request.getContextPath();
            String servletPath = request.getServletPath();
            String requestURI = request.getRequestURI();
            
            String path = requestURI.substring(contextPath.length() + servletPath.length());
            
            if ("/serverList".equals(path)) {
                List<String> servers = ConsoleManager.getScheduleManager().getScheduleDataManager().loadScheduleServerNames();
                List<Map<String, String>> serverList=new ArrayList<>();
                for(String server:servers){
                    Map<String, String> serverMap=new HashMap<>();
                    serverMap.put("serverName", server);
                    if( ConsoleManager.getScheduleManager().getScheduleDataManager().isLeader(server, servers)){
                        serverMap.put("leader", "1");
                    }else{
                        serverMap.put("leader", "0");
                    }
                    serverList.add(serverMap);
                }
                this.write(response, serverList);
                return;
            }else if("/taskList".equals(path)){
                List<TaskBean> taskList = ConsoleManager.queryScheduleTask();
                for(TaskBean taskBean:taskList){
                    if(!"1".equals(taskBean.getType())){
                        taskBean.setType("2");
                    }
                }
                this.write(response, taskList);
                return;
            }
            
            if ("".equals(path)||"/".equals(path)||"/index".equals(path)) {
                ServletUtil.INSTANCE.renderHtml(request, response, "index");
                return;
            }
            
            ServletUtil.INSTANCE.returnResourceFile(path, response);
        } catch (Exception ex) {
            logger.error("",ex);
            this.write(response, ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo());
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Home Post Action!");
        try {
            String action = request.getParameter("action");

            switch (action) {
                case "add":
                    TaskDefine taskDefine=new TaskDefine();
                    ConsoleManager.getScheduleManager().getScheduleDataManager().settingTask(taskDefine);
                    response.sendRedirect("/index");
                    break;
                case "del":
                    response.sendRedirect("/index");
                    break;
                default:
                    response.sendRedirect("/index");
            }

        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            this.write(response, ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo());
        }
    }
}
