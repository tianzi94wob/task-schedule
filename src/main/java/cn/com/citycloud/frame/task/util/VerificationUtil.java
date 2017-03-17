/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: VerificationUtil.java
 * Author:   zhaoyi
 * Date:     2017-3-7 上午9:51:12
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package cn.com.citycloud.frame.task.util;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.springframework.context.ApplicationContext;

import cn.com.citycloud.frame.task.ZKScheduleManager;
import cn.com.citycloud.frame.task.core.TaskDefine;
import cn.com.citycloud.frame.task.exception.TaskException;

/**
 * 校验工具
 *
 * @author zhaoyi
 */
public class VerificationUtil {

    @SuppressWarnings({ "rawtypes"})
    public static void checkTaskParam(TaskDefine taskDefine,ApplicationContext applicationContext) throws TaskException{
        if (taskDefine == null){
            throw new TaskException("参数对象为空！");
        }
        
        if(StringUtils.isEmpty(taskDefine.getPrjName())){
            throw new TaskException("工程名不能为空！");
        }
        
        if(StringUtils.isEmpty(taskDefine.getJobName())){
            throw new TaskException("任务名不能为空！");
        }
        
        if(StringUtils.isEmpty(taskDefine.getJobGroup())){
            throw new TaskException("任务组不能为空！");
        }
        
        try {
            CronScheduleBuilder.cronSchedule(taskDefine.getCronExpression());
        } catch (Exception e) {
            throw new TaskException("cron表达式有误！");
        }
        
        if(taskDefine.getPrjName().equals(ZKScheduleManager.getAppName())){
            Object obj = null;
            try {
                if (StringUtils.isNotBlank(taskDefine.getSpringId())) {
                    obj = applicationContext.getBean(taskDefine.getSpringId());
                } else {
                    Class clazz = Class.forName(taskDefine.getBeanClass());
                    obj = clazz.newInstance();
                }
            } catch (Exception e) {
            }
    
            if (obj == null) {
                throw new TaskException("未找到目标类，请检查类路径或springId是否正确");
            }
    
            Class clazz = obj.getClass();
            if (getMethod(clazz, taskDefine.getMethodName())==null) {
                throw new TaskException("未找到目标方法，请检查方法名是否正确");
            }
        }
        
    }
    
    /**
     * 功能描述: <br>
     * 获取public方法
     *
     * @param clazz
     * @param methodName
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Method getMethod(Class clazz,String methodName){
        try {
            Method[] methods=clazz.getMethods();
            for(Method method : methods){
                if(method.getName().equals(methodName)){
                    return method;
                }
            }
            
            return null;
        } catch (Exception e) {
        }
        return null;
    }
    
}
