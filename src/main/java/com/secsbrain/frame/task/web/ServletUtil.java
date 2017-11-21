package com.secsbrain.frame.task.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public enum ServletUtil {

    INSTANCE;

    private Map<String, String> resourceMap=new ConcurrentHashMap<>();
    
    public void renderHtml(HttpServletRequest request, HttpServletResponse response,String view) throws IOException  {
        if (request != null && response != null) {
            InputStream inputStream=this.getClass().getResourceAsStream("/view/"+view+".html");

            response.setContentType("text/html;charset=UTF-8");
            byte[] bytes = IOUtils.toByteArray(inputStream);
            if (bytes != null) {
                response.getOutputStream().write(bytes);
            }
        }

    }
    
    public void returnResourceFile(String fileName, HttpServletResponse response) throws IOException {
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
