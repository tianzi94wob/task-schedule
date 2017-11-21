package com.secsbrain.frame.task;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.secsbrain.frame.task.core.TaskDefine;
import com.secsbrain.frame.task.exception.TaskException;
import com.secsbrain.frame.task.web.TaskBean;

public class ConsoleManager {
	
    private static transient Logger log = LoggerFactory.getLogger(ConsoleManager.class);
    
    private static ZKScheduleManager scheduleManager;
    
    public static ZKScheduleManager getScheduleManager() throws Exception {
    	if(null == ConsoleManager.scheduleManager){
			synchronized(ConsoleManager.class) {
				ConsoleManager.scheduleManager = ZKScheduleManager.getApplicationcontext().getBean(ZKScheduleManager.class);
			}
    	}
        return ConsoleManager.scheduleManager;
    }

    /**
     * 功能描述: <br>
     * 设置调度任务
     * 1、新增
     * 2、修改 --只修改工程名、表达式、任务描述。其他关键信息不更新。修改表达式会自动更改调度
     *
     * @param taskDefine  任务定义参数
     * @throws TaskException
     */
    public static void setScheduleTask(TaskDefine taskDefine) throws TaskException{
        try {
			ConsoleManager.getScheduleManager().getScheduleDataManager().settingTask(taskDefine);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new TaskException(e);
		}
    }
    
    /**
     * 功能描述: <br>
     * 删除调度任务
     *
     * @param taskDefine
     */
    public static void delScheduleTask(TaskDefine taskDefine) {
        try {
			ConsoleManager.getScheduleManager().getScheduleDataManager().delTask(taskDefine);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
    }
    
    /**
     * 功能描述: <br>
     * 查询调度任务列表
     *
     * @return
     */
    public static List<TaskBean> queryScheduleTask() {
    	List<TaskBean> taskDefines = new ArrayList<TaskBean>();
        try {
			List<TaskBean> tasks = ConsoleManager.getScheduleManager().getScheduleDataManager().selectTask();
			taskDefines.addAll(tasks);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
        return taskDefines;
    }
    
    /**
     * 功能描述: <br>
     * 调度任务是否存在
     *
     * @param taskDefine
     * @return
     * @throws Exception
     */
    public static boolean isExistsTask(TaskDefine taskDefine) throws Exception{
        return ConsoleManager.getScheduleManager().getScheduleDataManager().isExistsTask(taskDefine);
    }
    
}
