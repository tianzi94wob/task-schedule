/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: RunningInfo.java
 * Author:   zhaoyi
 * Date:     2017-1-9 下午5:54:10
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.secsbrain.frame.task.core;

/**
 * 运行信息
 *
 * @author zhaoyi
 */
public class RunningInfo {
    
    /**任务正常*/
    public static final String NOMAL="0";
    
    /**任务等待分配*/
    public static final String WAIT="1";

    private int times;//执行次数
    
    private long lastRuningTime;//上次运行时间
    
    private String status;//状态

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public long getLastRuningTime() {
        return lastRuningTime;
    }

    public void setLastRuningTime(long lastRuningTime) {
        this.lastRuningTime = lastRuningTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
}
