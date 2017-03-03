package cn.com.citycloud.frame.task;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ResponseVo scheduleTask(TaskDefine taskDefine) {
        if (taskDefine == null){
            return ErrorCodeEnum.ERROR_COMMON_CHECK.getResponseVo("参数对象为空！");
        }
        
        try {
            CronScheduleBuilder.cronSchedule(taskDefine.getCronExpression());
        } catch (Exception e) {
            return ErrorCodeEnum.ERROR_COMMON_CHECK.getResponseVo("cron表达式有误");
        }

        Object obj = null;
        try {
            if (StringUtils.isNotBlank(taskDefine.getSpringId())) {
                obj = ZKScheduleManager.getApplicationcontext().getBean(taskDefine.getSpringId());
            } else {
                Class clazz = Class.forName(taskDefine.getBeanClass());
                obj = clazz.newInstance();
            }
        } catch (Exception e) {
        }

        if (obj == null) {
            return ErrorCodeEnum.ERROR_COMMON_CHECK.getResponseVo("未找到目标类，请检查类路径或springId是否正确");
        }

        Class clazz = obj.getClass();
        Method method = null;
        try {
            method = clazz.getMethod(taskDefine.getMethodName());
        } catch (Exception e) {
        }

        if (method == null) {
            return ErrorCodeEnum.ERROR_COMMON_CHECK.getResponseVo("未找到目标方法，请检查方法名是否正确");
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
            if (job == null || (
                    JobEnum.ScheduleJobStatus.ZK_DEL.getCode().equals(job.getJobStatus())
                    ||JobEnum.ScheduleJobStatus.STATUS_NOT_RUNNING.getCode().equals(job.getJobStatus())
                )) {
                return ErrorCodeEnum.ERROR_COMMON_SYSTEM.getResponseVo("任务不是运行状态！当前状态："+job.getJobStatus());
            }

            Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
            logger.debug("添加任务................{}", job.getDescription());
            TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobName(), job.getJobGroup());
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

            // 不存在，创建一个
            if (null == trigger) {
                Class clazz = JobEnum.ScheduleJobConStatus.CONCURRENT_IS.getCode().equals(job.getIsConcurrent()) ? QuartzJobFactory.class
                        : QuartzJobFactoryDisallowConcurrentExecution.class;
                JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(job.getJobName(), job.getJobGroup())
                        .build();
                jobDetail.getJobDataMap().put("scheduleJob", job);
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
                trigger = TriggerBuilder.newTrigger().withIdentity(job.getJobName(), job.getJobGroup())
                        .withSchedule(scheduleBuilder).build();
                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                // Trigger已存在，那么更新相应的定时设置
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
                // 按新的cronExpression表达式重新构建trigger
                trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
                // 按新的trigger重新设置job执行
                scheduler.rescheduleJob(triggerKey, trigger);
            }
            return ResponseVo.getSuccessResponse();
        } catch (Exception e) {
            logger.error("", e);
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
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
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
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
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
    public static void pauseJob(TaskScheduleJob scheduleJob) throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        scheduler.pauseJob(jobKey);
    }

    /**
     * 恢复一个job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static void resumeJob(TaskScheduleJob scheduleJob) throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        scheduler.resumeJob(jobKey);
    }

    /**
     * 功能描述: <br>
     * 清除所有Job
     * 
     * @throws SchedulerException
     */
    public static void clearJob() throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
        scheduler.clear();
    }

    /**
     * 删除一个job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static void deleteJob(TaskScheduleJob scheduleJob) throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        scheduler.deleteJob(jobKey);
    }

    /**
     * 立即执行job
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static void runAJobNow(TaskScheduleJob scheduleJob) throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        scheduler.triggerJob(jobKey);
    }

    /**
     * 更新job时间表达式
     * 
     * @param scheduleJob
     * @throws SchedulerException
     */
    public static void updateJobCron(TaskScheduleJob scheduleJob) throws SchedulerException {
        Scheduler scheduler = ZKScheduleManager.getApplicationcontext().getBean(SchedulerFactoryBean.class).getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCronExpression());
        trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
        scheduler.rescheduleJob(triggerKey, trigger);
    }

}
