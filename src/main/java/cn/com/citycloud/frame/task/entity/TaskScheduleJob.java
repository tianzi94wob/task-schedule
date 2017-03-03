package cn.com.citycloud.frame.task.entity;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * 定时任务配置表  sys_task_schedule_job
 *
 */
public class TaskScheduleJob implements Serializable {

    private static final long serialVersionUID = 2024251841726005286L;

    private Long id;

	/** 任务名称 */
	private String jobName;

	/** 任务状态 */
	private String jobStatus;

	/** 任务组 */
	private String jobGroup;

	/** 表达式 */
	private String cronExpression;

	/** 类路径 */
	private String beanClass;

	/** springId */
	private String springId;

	/** 方法名 */
	private String methodName;

	/** 是否同步 */
	private String isConcurrent;
	
	/** 应用名 */
	private String prjName;

	/** 描述 */
	private String description;

	/** 创建时间 */
	private Date createTime;

	/** 更新时间 */
	private Date updateTime;

	public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
		return this.jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getJobStatus() {
		return this.jobStatus;
	}

	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}

	public String getJobGroup() {
		return this.jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public String getCronExpression() {
		return this.cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public String getBeanClass() {
		return this.beanClass;
	}

	public void setBeanClass(String beanClass) {
		this.beanClass = beanClass;
	}

	public String getSpringId() {
		return this.springId;
	}

	public void setSpringId(String springId) {
		this.springId = springId;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getIsConcurrent() {
		return this.isConcurrent;
	}

	public void setIsConcurrent(String isConcurrent) {
		this.isConcurrent = isConcurrent;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return this.updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

    public String getPrjName() {
        return prjName;
    }

    public void setPrjName(String prjName) {
        this.prjName = prjName;
    }

}
