package cn.com.citycloud.frame.task.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.citycloud.frame.task.ZKScheduleManager;
import cn.com.citycloud.frame.task.entity.TaskScheduleJob;

/**
 * 任务管理工具类
 * 
 * @author zhaoyi
 */
public class TaskUtils {

    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);

    /**
     * 通过反射调用scheduleJob中定义的方法
     * 
     * @param scheduleJob
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void invokMethod(TaskScheduleJob scheduleJob) {
        Object object = null;
        Class clazz = null;
        if (StringUtils.isNotBlank(scheduleJob.getSpringId())) {
            object = ZKScheduleManager.getApplicationcontext().getBean(scheduleJob.getSpringId());
        } else if (StringUtils.isNotBlank(scheduleJob.getBeanClass())) {
            try {
                clazz = Class.forName(scheduleJob.getBeanClass());
                object = clazz.newInstance();
            } catch (Exception e) {
                logger.error("", e);
            }

        }
        if (object == null) {
            logger.error("任务名称 = [" + scheduleJob.getJobName() + "]---------------未执行成功，请检查是否配置正确！！！");
            return;
        }
        clazz = object.getClass();
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(scheduleJob.getMethodName());
        } catch (NoSuchMethodException e) {
            logger.error("任务名称 = [" + scheduleJob.getJobName() + "]---------------未执行成功，方法名设置错误！！！");
        } catch (SecurityException e) {
            logger.error("", e);
        }
        if (method != null) {
            try {
                method.invoke(object);
                TaskDefine taskDefine=new TaskDefine(scheduleJob);
                ZKScheduleManager zkScheduleManager=ZKScheduleManager.getApplicationcontext().getBean(ZKScheduleManager.class);
                zkScheduleManager.getScheduleDataManager().saveRunningInfo(taskDefine.stringKey(),zkScheduleManager.getCurrenScheduleServerUuid());
            } catch (IllegalAccessException e) {
                logger.error("", e);
            } catch (IllegalArgumentException e) {
                logger.error("", e);
            } catch (InvocationTargetException e) {
                logger.error("", e);
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        logger.debug("任务名称 = [" + scheduleJob.getJobName() + "]----------执行成功");
    }
}
