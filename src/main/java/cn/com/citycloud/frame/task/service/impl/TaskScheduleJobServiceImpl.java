package cn.com.citycloud.frame.task.service.impl;

import java.util.Date;
import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.com.citycloud.frame.task.DynamicTaskManager;
import cn.com.citycloud.frame.task.JobEnum;
import cn.com.citycloud.frame.task.dao.TaskScheduleJobDao;
import cn.com.citycloud.frame.task.entity.TaskScheduleJob;
import cn.com.citycloud.frame.task.service.TaskScheduleJobService;

/**
 * 
 * TaskScheduleJob 表数据服务层接口实现类
 * 
 */
@Service("taskScheduleJobService")
public class TaskScheduleJobServiceImpl implements TaskScheduleJobService {
    
    @Autowired
    private TaskScheduleJobDao taskScheduleJobDao;
    
    @Override
    public void addTask(TaskScheduleJob job) throws Exception {
        TaskScheduleJob jobCond = new TaskScheduleJob();
        jobCond.setJobName(job.getJobName());
        jobCond.setJobGroup(job.getJobGroup());
        jobCond.setPrjName(job.getPrjName());
        List<TaskScheduleJob> list = taskScheduleJobDao.queryTaskScheduleJobList(jobCond);
        if (list != null && list.size() > 0) {
            throw new RuntimeException("name group 组合有重复！");
        }
        job.setCreateTime(new Date());
        taskScheduleJobDao.insert(job);
    }

    /**
     * 更改任务状态
     * 
     * @throws SchedulerException
     */
    @Override
    public TaskScheduleJob changeStatus(Long jobId, String cmd) throws SchedulerException {
        TaskScheduleJob job = taskScheduleJobDao.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("没有查询到相应的任务");
        }
        job.setUpdateTime(new Date());
        if ("stop".equals(cmd)) {
            job.setJobStatus(JobEnum.ScheduleJobStatus.STATUS_NOT_RUNNING.getCode());
        } else if ("start".equals(cmd)) {
            job.setJobStatus(JobEnum.ScheduleJobStatus.STATUS_RUNNING.getCode());
        }
        taskScheduleJobDao.updateSelectiveById(job);
        return job;
    }

    /**
     * 更新任务信息
     * 
     * @throws SchedulerException
     */
    @Override
    public TaskScheduleJob updateCron(TaskScheduleJob taskScheduleJob) throws Exception {
        TaskScheduleJob job = taskScheduleJobDao.selectById(taskScheduleJob.getId());
        if (job == null) {
            throw new RuntimeException("没有查询到相应的任务");
        }
        job.setUpdateTime(new Date());
        job.setPrjName(taskScheduleJob.getPrjName());
        job.setCronExpression(taskScheduleJob.getCronExpression());
        job.setDescription(taskScheduleJob.getDescription());
        taskScheduleJobDao.updateSelectiveById(job);
        return job;
    }

    /**
     * 更新job时间表达式
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public void updateJobCron(TaskScheduleJob scheduleJob) throws SchedulerException {
        DynamicTaskManager.updateJobCron(scheduleJob);
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.live.tdp.service.TaskScheduleJobService#queryAllRunningJob()
     */
    @Override
    public List<TaskScheduleJob> queryAllRunningJob(String prjName) {
        TaskScheduleJob taskScheduleJob=new TaskScheduleJob();
        taskScheduleJob.setJobStatus(JobEnum.ScheduleJobStatus.STATUS_RUNNING.getCode());
        taskScheduleJob.setPrjName(prjName);
        return taskScheduleJobDao.queryTaskScheduleJobList(taskScheduleJob);
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.service.TaskScheduleJobService#selectById(java.lang.Long)
     */
    @Override
    public TaskScheduleJob selectById(Long jobId) {
        return taskScheduleJobDao.selectById(jobId);
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.service.TaskScheduleJobService#updateSelectiveById(cn.com.citycloud.frame.task.entity.TaskScheduleJob)
     */
    @Override
    public void updateSelectiveById(TaskScheduleJob scheduleJob) {
        taskScheduleJobDao.updateSelectiveById(scheduleJob);
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.service.TaskScheduleJobService#queryTaskScheduleJobList(cn.com.citycloud.frame.task.entity.TaskScheduleJob)
     */
    @Override
    public List<TaskScheduleJob> queryTaskScheduleJobList(TaskScheduleJob taskScheduleJob) {
        return taskScheduleJobDao.queryTaskScheduleJobList(taskScheduleJob);
    }

}