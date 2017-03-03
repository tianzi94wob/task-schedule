package cn.com.citycloud.frame.task.service;

import java.util.List;

import org.quartz.SchedulerException;

import cn.com.citycloud.frame.task.entity.TaskScheduleJob;

/**
 *
 * TaskScheduleJob 表数据服务层接口
 *
 */
public interface TaskScheduleJobService {
    
    /**
     * 功能描述: <br>
     * 查询所有状态为运行中的Job
     *
     * @param prjName 工程名
     * @return
     */
    List<TaskScheduleJob> queryAllRunningJob(String prjName);
    
    List<TaskScheduleJob> queryTaskScheduleJobList(TaskScheduleJob taskScheduleJob);
    
    void updateSelectiveById(TaskScheduleJob scheduleJob);
    
    TaskScheduleJob selectById(Long jobId);
    
    void addTask(TaskScheduleJob job) throws Exception;
    
    TaskScheduleJob changeStatus(Long jobId, String cmd) throws SchedulerException;
    
    TaskScheduleJob updateCron(TaskScheduleJob taskScheduleJob) throws Exception;
    
}