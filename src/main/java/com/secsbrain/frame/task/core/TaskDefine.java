package com.secsbrain.frame.task.core;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.secsbrain.frame.task.JobEnum;
import com.secsbrain.frame.task.ZKScheduleManager;
import com.secsbrain.frame.task.entity.TaskScheduleJob;

/**
 * 任务定义，提供关键信息给使用者
 * 
 * @author juny.ye
 * @author zhaoyi
 * 
 */
public class TaskDefine extends TaskScheduleJob {

    private static final long serialVersionUID = 2514769644349516654L;
    
    public static final String ZK_TASK_TYPE="1";

    /**
     * 目标bean
     */
    private String targetBean;

    /**
     * 目标方法
     */
    private String targetMethod;

    private String currentServer;

    /**
     * 参数
     */
    private String params;

    /**
     * 类型 空-持久化任务，1-临时任务
     */
    private String type;

    public TaskDefine() {
        super();
    }

    public TaskDefine(TaskScheduleJob taskScheduleJob) {
        super.setCronExpression(taskScheduleJob.getCronExpression());
        super.setBeanClass(taskScheduleJob.getBeanClass());
        super.setSpringId(taskScheduleJob.getSpringId());
        super.setPrjName(taskScheduleJob.getPrjName());
        super.setJobGroup(taskScheduleJob.getJobGroup());
        super.setJobName(taskScheduleJob.getJobName());
        super.setJobStatus(taskScheduleJob.getJobStatus());
        super.setDescription(taskScheduleJob.getDescription());
        super.setMethodName(taskScheduleJob.getMethodName());
    }
    
    public TaskDefine(String springId, String jobName, String methodName, String params) {
        super();
        super.setSpringId(springId);
        super.setPrjName(ZKScheduleManager.getAppName());
        super.setJobName(jobName);
        super.setMethodName(methodName);
        this.params = params;
    }
    
    /**
     * 创建临时任务
     * 
     * @param springId         spring bean Id
     * @param jobName          任务名称，不能重复
     * @param methodName       方法名
     * @param cronExpression   cronExpression
     * @param params           额外的参数，支持字符串
     */
    public TaskDefine(String springId, String jobName, String methodName, String cronExpression, String params) {
        super();
        super.setCronExpression(cronExpression);
        super.setSpringId(springId);
        super.setPrjName(ZKScheduleManager.getAppName());
        super.setJobGroup(jobName);
        super.setJobName(jobName);
        super.setJobStatus(JobEnum.JobStatus.STATUS_RUNNING.getCode());
        super.setDescription(jobName);
        super.setMethodName(methodName);
        super.setCreateTime(new Date());
        this.params = params;
        this.type = ZK_TASK_TYPE;
    }

    public String getTargetBean() {
        if (this.targetBean != null) {
            return this.targetBean;
        }
        return StringUtils.isEmpty(getBeanClass()) ? getSpringId() : getBeanClass();
    }

    public void setTargetBean(String targetBean) {
        this.targetBean = targetBean;
    }

    public String getTargetMethod() {
        if (this.targetMethod != null) {
            return targetMethod;
        }
        return getMethodName();
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }

    public String stringKey() {
        String key=getTargetBean() + "#" + getTargetMethod();
        if(!StringUtils.isEmpty(getParams())){
            return key + "#" + getParams();
        }
        return key;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}