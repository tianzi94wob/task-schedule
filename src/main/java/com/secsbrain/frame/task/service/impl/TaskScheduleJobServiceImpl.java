package com.secsbrain.frame.task.service.impl;

import java.util.Date;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.secsbrain.frame.task.JobEnum;
import com.secsbrain.frame.task.ZKScheduleManager;
import com.secsbrain.frame.task.core.TaskDefine;
import com.secsbrain.frame.task.dao.TaskScheduleJobDao;
import com.secsbrain.frame.task.entity.TaskScheduleJob;
import com.secsbrain.frame.task.exception.TaskException;
import com.secsbrain.frame.task.service.TaskScheduleJobService;
import com.secsbrain.frame.task.util.VerificationUtil;
import com.secsbrain.frame.task.web.TaskBean;

/**
 * TaskScheduleJob 表数据服务层接口实现类
 * 
 * @author zhaoyi
 */
@Service("taskScheduleJobService")
public class TaskScheduleJobServiceImpl implements TaskScheduleJobService {
    
    @Autowired
    private TaskScheduleJobDao taskScheduleJobDao;
    
    @Autowired
    private ZKScheduleManager scheduleManager;
    
    /* (non-Javadoc)
     * @see com.secsbrain.frame.task.service.TaskScheduleJobService#queryTaskScheduleJobList(com.secsbrain.frame.task.entity.TaskScheduleJob)
     */
    @Override
    public List<TaskScheduleJob> queryTaskScheduleJobList(TaskScheduleJob taskScheduleJob) {
        return taskScheduleJobDao.queryTaskScheduleJobList(taskScheduleJob);
    }
    
    /* (non-Javadoc)
     * @see com.secsbrain.frame.task.service.TaskScheduleJobService#addTask(com.secsbrain.frame.task.core.TaskDefine)
     */
    @Override
    @Transactional
    public void addTask(TaskDefine taskDefine) throws Exception {
        VerificationUtil.checkTaskParam(taskDefine, ZKScheduleManager.getApplicationcontext());
        
        TaskScheduleJob jobCond = new TaskScheduleJob();
        jobCond.setJobName(taskDefine.getJobName());
        jobCond.setJobGroup(taskDefine.getJobGroup());
        jobCond.setPrjName(taskDefine.getPrjName());
        List<TaskScheduleJob> list = taskScheduleJobDao.queryTaskScheduleJobList(jobCond);
        if (list != null && list.size() > 0) {
            throw new TaskException("name group 组合有重复！");
        }
        taskDefine.setCreateTime(new Date());
        taskScheduleJobDao.insert(taskDefine);
        
        scheduleManager.getScheduleDataManager().settingTask(taskDefine);
    }
    
    /* (non-Javadoc)
     * @see com.secsbrain.frame.task.service.TaskScheduleJobService#updateTask(com.secsbrain.frame.task.entity.TaskScheduleJob)
     */
    @Override
    @Transactional
    public void updateTask(TaskScheduleJob taskScheduleJob) throws Exception {
        try {
            CronScheduleBuilder.cronSchedule(taskScheduleJob.getCronExpression());
        } catch (Exception e) {
            throw new TaskException("cron表达式有误");
        }
        
        TaskScheduleJob job = taskScheduleJobDao.selectById(taskScheduleJob.getId());
        if (job == null) {
            throw new TaskException("没有查询到相应的任务");
        }
        
        job.setUpdateTime(new Date());
        job.setPrjName(taskScheduleJob.getPrjName());
        job.setCronExpression(taskScheduleJob.getCronExpression());
        job.setDescription(taskScheduleJob.getDescription());
        taskScheduleJobDao.updateSelectiveById(job);
        
        if (JobEnum.JobStatus.STATUS_RUNNING.getCode().equals(job.getJobStatus())) {
            scheduleManager.getScheduleDataManager().settingTask(new TaskDefine(job),JobEnum.JobInstruct.ZK_UPDATE.getCode());
        }
    }

    /* (non-Javadoc)
     * @see com.secsbrain.frame.task.service.TaskScheduleJobService#deleteTask(java.lang.Long)
     */
    @Override
    @Transactional
    public void deleteTask(Long jobId) throws Exception {
        TaskScheduleJob scheduleJob=taskScheduleJobDao.selectById(jobId);
        if(!JobEnum.JobStatus.STATUS_NOT_RUNNING.getCode().equals(scheduleJob.getJobStatus())){
            throw new TaskException("只能删除不在运行中的任务！");
        }
        
        scheduleJob.setJobStatus(JobEnum.JobStatus.STATUS_DEL.getCode());
        taskScheduleJobDao.updateSelectiveById(scheduleJob);
        
        TaskDefine taskDefine=new TaskDefine(scheduleJob);
        scheduleManager.getScheduleDataManager().settingTask(taskDefine,JobEnum.JobInstruct.ZK_DEL.getCode());
    }
    
    /* (non-Javadoc)
     * @see com.secsbrain.frame.task.service.TaskScheduleJobService#selectTaskDetail(java.lang.Long)
     */
    @Override
    public TaskBean selectTaskDetail(Long jobId) throws Exception {
        TaskScheduleJob taskScheduleJob=taskScheduleJobDao.selectById(jobId);
        TaskDefine taskDefine=new TaskDefine(taskScheduleJob);
        return scheduleManager.getScheduleDataManager().selectTaskByName(taskDefine);
    }

    /* (non-Javadoc)
     * @see com.secsbrain.frame.task.service.TaskScheduleJobService#changeStatus(java.lang.Long, java.lang.String)
     */
    @Override
    @Transactional
    public void changeStatus(Long jobId, String cmd) throws Exception {
        TaskScheduleJob job = taskScheduleJobDao.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("没有查询到相应的任务");
        }
        job.setUpdateTime(new Date());
        
        String instruct="";
        if ("stop".equals(cmd)) {
            instruct = (JobEnum.JobInstruct.ZK_DEL.getCode());
            job.setJobStatus(JobEnum.JobStatus.STATUS_NOT_RUNNING.getCode());
        } else if ("start".equals(cmd)) {
            instruct = (JobEnum.JobInstruct.ZK_START.getCode());
            job.setJobStatus(JobEnum.JobStatus.STATUS_RUNNING.getCode());
        } else if ("immed".equals(cmd)) {
            instruct = (JobEnum.JobInstruct.ZK_IMMED.getCode());
        }
        
        if("stop".equals(cmd)||"start".equals(cmd)){
            taskScheduleJobDao.updateSelectiveById(job);
        }
        
        TaskDefine taskDefine=new TaskDefine(job);
        scheduleManager.getScheduleDataManager().settingTask(taskDefine,instruct);//这里既要更新任务信息，又要更新指令
    }
    
}