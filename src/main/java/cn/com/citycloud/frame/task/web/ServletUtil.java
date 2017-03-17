package cn.com.citycloud.frame.task.web;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ServletUtil {

    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(ServletUtil.class);

    public void renderHtml(HttpServletRequest request, HttpServletResponse response, Map<String, Object> templateParam,
            String view) throws IOException, TemplateException {

        if (request != null && response != null && templateParam != null) {
            response.setContentType("text/html;charset=UTF-8");

            Configuration config = new Configuration(Configuration.VERSION_2_3_0);
            config.setClassForTemplateLoading(this.getClass(), "/view");
            
            Template template = config.getTemplate("/template/" + view, "UTF-8");
            template.process(templateParam, response.getWriter());
        }

    }

    public void renderError(HttpServletRequest request, HttpServletResponse response, String error) {
        try {
            logger.error("Error :" + error);
            Map<String, Object> templateParam = new HashMap<>();
            response.setContentType("text/html;charset=UTF-8");
            Template template = null;
            Configuration config = new Configuration(Configuration.VERSION_2_3_0);
            config.setClassForTemplateLoading(this.getClass(), "/view");

            template = config.getTemplate("/template/error.ftl.html");
            templateParam.put("error", error);
            template.process(templateParam, response.getWriter());
        } catch (TemplateException | IOException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        }

    }

    private Map<String, String> resourceMap=new ConcurrentHashMap<String, String>();
    
    public void returnResourceFile(String fileName, String uri, HttpServletResponse response)
            throws ServletException, IOException {
        if(!(fileName.endsWith(".jpg")||fileName.endsWith(".css")||fileName.endsWith(".js"))){
            return;
        }
        
        InputStream inputStream=this.getClass().getResourceAsStream("/view"+fileName);
        if (fileName.endsWith(".jpg")) {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            if (bytes != null) {
                response.getOutputStream().write(bytes);
            }
            return;
        }

        if (fileName.endsWith(".css")) {
            response.setContentType("text/css;charset=utf-8");
        } else if (fileName.endsWith(".js")) {
            response.setContentType("text/javascript;charset=utf-8");
        }
        
        String text = resourceMap.get(fileName);
        if(!resourceMap.containsKey(fileName)){
            text = IOUtils.toString(inputStream);
            resourceMap.put(fileName, text);
        }
        response.getWriter().write(text);
    }
    
}
