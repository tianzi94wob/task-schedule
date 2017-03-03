package cn.com.citycloud.frame.task.core;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import cn.com.citycloud.frame.task.entity.TaskScheduleJob;

/**
 * 任务定义，提供关键信息给使用者
 * 
 * @author juny.ye
 * @author zhaoyi
 * 
 */
public class TaskDefine extends TaskScheduleJob {

    private static final long serialVersionUID = 2514769644349516654L;

    /**
     * 目标bean
     */
    private String targetBean;

    /**
     * 目标方法
     */
    private String targetMethod;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 周期（秒）
     */
    private long period;

    private String currentServer;

    /**
     * 参数
     */
    private String params;

    /**
     * 类型
     */
    private String type;

    private int runTimes;

    private long lastRunningTime;

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

    public boolean begin(Date sysTime) {
        return null != sysTime && sysTime.after(startTime);
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

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }

    public String stringKey() {
        return getTargetBean() + "#" + getTargetMethod();
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

}