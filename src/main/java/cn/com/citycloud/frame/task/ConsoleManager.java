package cn.com.citycloud.frame.task;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.citycloud.frame.task.core.TaskDefine;

public class ConsoleManager {
	
    private static transient Logger log = LoggerFactory.getLogger(ConsoleManager.class);
    
//    private static Gson GSON = new GsonBuilder().create();

    private static ZKScheduleManager scheduleManager;
    
    public static ZKScheduleManager getScheduleManager() throws Exception {
    	if(null == ConsoleManager.scheduleManager){
			synchronized(ConsoleManager.class) {
				ConsoleManager.scheduleManager = ZKScheduleManager.getApplicationcontext().getBean(ZKScheduleManager.class);
			}
    	}
        return ConsoleManager.scheduleManager;
    }

    public static void addScheduleTask(TaskDefine taskDefine) {
        try {
			ConsoleManager.getScheduleManager().getScheduleDataManager().settingTask(taskDefine);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
    }
    
    public static void delScheduleTask(TaskDefine taskDefine) {
        try {
			ConsoleManager.scheduleManager.getScheduleDataManager().delTask(taskDefine);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
    }
    
    public static List<TaskDefine> queryScheduleTask() {
    	List<TaskDefine> taskDefines = new ArrayList<TaskDefine>();
        try {
			List<TaskDefine> tasks = ConsoleManager.getScheduleManager().getScheduleDataManager().selectTask();
			taskDefines.addAll(tasks);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
        return taskDefines;
    }
    
    public static boolean isExistsTask(TaskDefine taskDefine) throws Exception{
        return ConsoleManager.scheduleManager.getScheduleDataManager().isExistsTask(taskDefine);
    }
    
}
