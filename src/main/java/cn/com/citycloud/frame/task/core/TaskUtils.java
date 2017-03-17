package cn.com.citycloud.frame.task.core;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.citycloud.frame.task.ZKScheduleManager;
import cn.com.citycloud.frame.task.util.VerificationUtil;

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
    @SuppressWarnings({ "rawtypes" })
    public static void invokMethod(final TaskDefine scheduleJob) {
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
        final Method method = VerificationUtil.getMethod(clazz, scheduleJob.getMethodName());

        if (method == null) {
            logger.error("任务名称 = [" + scheduleJob.getJobName() + "]---------------未执行成功，方法名设置错误！！！");
            return;
        }

        final Object obj = object;

        final ZKScheduleManager zkScheduleManager = ZKScheduleManager.getApplicationcontext().getBean(ZKScheduleManager.class);
        
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new Runnable() {

            @Override
            public void run() {
                long startTime=System.currentTimeMillis();
                try {
                    // 执行方法
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes != null && parameterTypes.length > 0) {
                        // TODO 只支持一个参数传入，需要做验证
                        method.invoke(obj, scheduleJob.getParams());
                    } else {
                        method.invoke(obj);
                    }
                } catch (Exception e) {
                    logger.error("", e);
                } finally {
                    long endTime=System.currentTimeMillis();
                    logger.debug("方法执行时间："+(endTime-startTime)+" ms");
                    zkScheduleManager.getScheduleDataManager().saveRunningInfo2(scheduleJob.stringKey(), (endTime-startTime),zkScheduleManager.getCurrenScheduleServerUuid());
                }
            }

        });
        // 保存运行信息
        zkScheduleManager.getScheduleDataManager().saveRunningInfo(scheduleJob.stringKey(), zkScheduleManager.getCurrenScheduleServerUuid());
        logger.debug("任务名称 = [" + scheduleJob.getJobName() + "]----------执行成功");
    }
}
