package cn.com.citycloud.frame.task.web;

import java.io.Serializable;

/**
 * 任务信息，界面展示对象
 * 
 * @author zhaoyi
 * 
 */
public class TaskBean implements Serializable{

    private static final long serialVersionUID = -3752548631339258573L;
    
    private String prjName;//工程名
    
    private String description;//描述
    
    private String jobName;//任务名称
    
    private String jobGroup;//任务组

    private String targetBean;//目标bean

    private String targetMethod;//目标方法
    
    private String cronExpression;//表达式

    private String currentServer;//当前server

    private String params;//参数

    private String type;//类型 空-持久化任务，1-临时任务

    private int runTimes;//运行次数
    
    private String jobStatus;//任务状态
    
    private long msc;//任务最近一次耗时毫秒数

    private long lastRunningTime;//上次运行时间

    private long nextRuningTime;//下次运行时间

    public String getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(String targetBean) {
        this.targetBean = targetBean;
    }

    public String getTargetMethod() {
        return targetMethod;
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

    public int getRunTimes() {
        return runTimes;
    }

    public void setRunTimes(int runTimes) {
        this.runTimes = runTimes;
    }

    public long getLastRunningTime() {
        return lastRunningTime;
    }

    public void setLastRunningTime(long lastRunningTime) {
        this.lastRunningTime = lastRunningTime;
    }

    public long getNextRuningTime() {
        return nextRuningTime;
    }

    public void setNextRuningTime(long nextRuningTime) {
        this.nextRuningTime = nextRuningTime;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public long getMsc() {
        return msc;
    }

    public void setMsc(long msc) {
        this.msc = msc;
    }

    public String getPrjName() {
        return prjName;
    }

    public void setPrjName(String prjName) {
        this.prjName = prjName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }
    
}