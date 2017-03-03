package cn.com.citycloud.frame.task.web;

import java.io.IOException;
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

import cn.com.citycloud.frame.task.ConsoleManager;
import cn.com.citycloud.frame.task.core.TaskDefine;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/taskSchedule"}, loadOnStartup = 1)
public class HomeServlet extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(HomeServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Home Get Action!");
        try {
            String contextPath = request.getContextPath();
            String servletPath = request.getServletPath();
            String requestURI = request.getRequestURI();
            
            String uri = contextPath + servletPath;
            String path = requestURI.substring(contextPath.length() + servletPath.length());
            
            Map<String, Object> templateParam = new HashMap<>();
            
            if ("".equals(path)||"/".equals(path)||"/index".equals(path)) {
                List<String> servers = ConsoleManager.getScheduleManager().getScheduleDataManager().loadScheduleServerNames();
                List<Map<String, String>> serverList=new ArrayList<Map<String, String>>();
                for(String server:servers){
                    Map<String, String> serverMap=new HashMap<String, String>();
                    serverMap.put("serverName", server);
                    if( ConsoleManager.getScheduleManager().getScheduleDataManager().isLeader(server, servers)){
                        serverMap.put("leader", "是");
                    }else{
                        serverMap.put("leader", "否");
                    }
                    serverList.add(serverMap);
                }
                
                List<TaskDefine> taskList = ConsoleManager.queryScheduleTask();
                
                templateParam.put("serverList", serverList);
                templateParam.put("taskList", taskList);
                ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "home.ftl.html");
                return;
            }
            
            ServletUtil.INSTANCE.returnResourceFile(path, uri, response);
        } catch (Exception ex) {
            logger.error("",ex);
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Home Post Action!");
        try {

            Map<String, Object> templateParam = new HashMap<>();
            String action = request.getParameter("action");

            switch (action) {
                case "add":
                    TaskDefine taskDefine=new TaskDefine();
                    //TODO 临时任务失效如何清理？
                    //TODO 加载持久化任务的时候不需要同步，否则临时任务会被清理
                    ConsoleManager.getScheduleManager().getScheduleDataManager().settingTask(taskDefine);
                    response.sendRedirect("/index");
                    break;
                case "del":
                    //TODO 删除
                    response.sendRedirect("/index");
                    break;
                default:
                    response.sendRedirect("/index");
            }

        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
