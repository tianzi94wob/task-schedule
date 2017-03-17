package cn.com.citycloud.frame.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import cn.com.citycloud.frame.task.common.ErrorCodeEnum;
import cn.com.citycloud.frame.task.common.ResponseVo;
import cn.com.citycloud.frame.task.core.QuartzJobFactory;
import cn.com.citycloud.frame.task.core.QuartzJobFactoryDisallowConcurrentExecution;
import cn.com.citycloud.frame.task.core.TaskDefine;
import cn.com.citycloud.frame.task.entity.TaskScheduleJob;
import cn.com.citycloud.frame.task.util.VerificationUtil;

/**
 * 任务管理
 * 
 * @author zhaoyi
 */
public class DynamicTaskManager {

    private static final transient Logger logger = LoggerFactory.getLogger(DynamicTaskManager.class);

    /**
     * 启动定时任务
     * 
     * @param taskDefine
     * @param currentTime
     */
    @SuppressWarnings({ "rawtypes" })
    public static ResponseVo scheduleTask(TaskDefine taskDefine) {
        try {
            VerificationUtil.checkTaskParam(taskDefine, ZKScheduleManager.getApplicationcontext());
        } catch (Exception e) {
            return ErrorCodeEnum.ERROR_COMMON_CHECK.getResponseVo(e.getMessage());
        }
        return addJob(taskDefine);
    }

    /**
     * 添加任务
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ResponseVo addJob(TaskDefine job) {
        try {
            if (job == null ||  !JobEnum.JobStatus.STATUS_RUNNING.getCode().equals(job.getJobStatus())) {
                return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo("任务不是运行状态！当前状态：" + job.getJobStatus());
            }

            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            logger.debug("添加任务................{}", job.getDescription());
            TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobName(), job.getJobGroup());
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

            if (null != trigger) {
                return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo("trigger已经存在!");
            } 

            Class clazz = JobEnum.ScheduleJobConStatus.CONCURRENT_IS.getCode().equals(job.getIsConcurrent()) ? QuartzJobFactory.class
                    : QuartzJobFactoryDisallowConcurrentExecution.class;
            JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(job.getJobName(), job.getJobGroup())
                    .build();
            jobDetail.getJobDataMap().put("scheduleJob", job);
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
            trigger = TriggerBuilder.newTrigger().withIdentity(job.getJobName(), job.getJobGroup())
                    .withSchedule(scheduleBuilder).build();
            scheduler.scheduleJob(jobDetail, trigger);
            
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.warn("task name："+job.getJobName()+" schedule error,"+e.getMessage());
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }

    }

    /**
     * 获取所有计划中的任务列表
     * 
     * @return
     * @throws SchedulerException
     */
    public static List<TaskScheduleJob> getAllJob() throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                .getScheduler();
        GroupMatcher<JobKey> matcher = GroupMatcher.anyJobGroup();
        Set<JobKey> jobKeys = scheduler.getJobKeys(matcher);
        List<TaskScheduleJob> jobList = new ArrayList<TaskScheduleJob>();
        for (JobKey jobKey : jobKeys) {
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            for (Trigger trigger : triggers) {
                TaskScheduleJob job = new TaskScheduleJob();
                job.setJobName(jobKey.getName());
                job.setJobGroup(jobKey.getGroup());
                job.setDescription("触发器:" + trigger.getKey());
                Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                job.setJobStatus(triggerState.name());
                if (trigger instanceof CronTrigger) {
                    CronTrigger cronTrigger = (CronTrigger) trigger;
                    String cronExpression = cronTrigger.getCronExpression();
                    job.setCronExpression(cronExpression);
                }
                jobList.add(job);
            }
        }
        return jobList;
    }

    public static boolean isJobScheduled(String jobKey) {
        try {
            List<TaskScheduleJob> list = getAllJob();
            Map<String, TaskScheduleJob> map = new HashMap<String, TaskScheduleJob>();
            for (TaskScheduleJob taskScheduleJob : list) {
                map.put(taskScheduleJob.getJobName() + taskScheduleJob.getJobGroup(), taskScheduleJob);
            }
            return map.containsKey(jobKey);
        } catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException();
        }

    }

    /**
     * 所有正在运行的job
     * 
     * @return
     * @throws SchedulerException
     */
    public static List<TaskScheduleJob> getRunningJob() throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                .getScheduler();
        List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
        List<TaskScheduleJob> jobList = new ArrayList<TaskScheduleJob>(executingJobs.size());
        for (JobExecutionContext executingJob : executingJobs) {
            TaskScheduleJob job = new TaskScheduleJob();
            JobDetail jobDetail = executingJob.getJobDetail();
            JobKey jobKey = jobDetail.getKey();
            Trigger trigger = executingJob.getTrigger();
            job.setJobName(jobKey.getName());
            job.setJobGroup(jobKey.getGroup());
            job.setDescription("触发器:" + trigger.getKey());
            Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
            job.setJobStatus(triggerState.name());
            if (trigger instanceof CronTrigger) {
                CronTrigger cronTrigger = (CronTrigger) trigger;
                String cronExpression = cronTrigger.getCronExpression();
                job.setCronExpression(cronExpression);
            }
            jobList.add(job);
        }
        return jobList;
    }

    /**
     * 暂停一个job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static ResponseVo<?> pauseJob(TaskScheduleJob scheduleJob) {
        try {
            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
            scheduler.pauseJob(jobKey);
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.warn("暂停job异常:"+e.getMessage()+",JobName:"+scheduleJob.getJobName());
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }
        
    }

    /**
     * 恢复一个job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static ResponseVo<?> resumeJob(TaskScheduleJob scheduleJob) {
        try {
            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
            scheduler.resumeJob(jobKey);
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.warn("恢复job异常:"+e.getMessage()+",JobName:"+scheduleJob.getJobName());
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }
        
    }

    /**
     * 功能描述: <br>
     * 清除所有Job
     * 
     * @throws SchedulerException
     */
    public static ResponseVo<?> clearJob() {
        try {
            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            scheduler.clear();
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.error("清除所有Job异常",e);
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }
        
    }

    /**
     * 删除一个job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static ResponseVo<?> deleteJob(TaskScheduleJob scheduleJob) {
        try {
            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
            scheduler.deleteJob(jobKey);
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.warn("删除job异常:"+e.getMessage()+",JobName:"+scheduleJob.getJobName());
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }
        
    }

    /**
     * 立即执行job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static ResponseVo<?> runAJobNow(TaskScheduleJob scheduleJob) {
        try {
            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
            scheduler.triggerJob(jobKey);
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.warn("立即执行job异常:"+e.getMessage()+",JobName:"+scheduleJob.getJobName());
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }
        
    }

    /**
     * 更新job时间表达式
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static ResponseVo<?> updateJobCron(TaskScheduleJob scheduleJob) {
        try {
            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class)
                    .getScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCronExpression());
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
            scheduler.rescheduleJob(triggerKey, trigger);
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.error("updateJobCron error:"+e.getMessage()+",JobName:"+scheduleJob.getJobName());
            return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo(e.getMessage());
        }
        
    }

}
