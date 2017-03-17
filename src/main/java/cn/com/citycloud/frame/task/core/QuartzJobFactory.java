package cn.com.citycloud.frame.task.core;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @Description: 计划任务执行处 无状态
 * @author zhaoyi
 */
public class QuartzJobFactory implements Job {
    
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
	    //TODO 测试下这个map里的值何时释放
        final TaskDefine scheduleJob = (TaskDefine) context.getMergedJobDataMap().get("scheduleJob");
        System.out.println(scheduleJob.getJobName());
        TaskUtils.invokMethod(scheduleJob); 
	}
	
}