package cn.com.citycloud.frame.task.core;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import cn.com.citycloud.frame.task.entity.TaskScheduleJob;

/**
 * @Description: 计划任务执行处 无状态
 * @author zhaoyi
 */
public class QuartzJobFactory implements Job {
    
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
        final TaskScheduleJob scheduleJob = (TaskScheduleJob) context.getMergedJobDataMap().get("scheduleJob");
        System.out.println(scheduleJob.getJobName());
        TaskUtils.invokMethod(scheduleJob); 
	}
	
}