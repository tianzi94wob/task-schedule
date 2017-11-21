/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: TaskException.java
 * Author:   zhaoyi
 * Date:     2017-3-6 上午10:16:59
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.secsbrain.frame.task.exception;

/**
 * 自定义任务异常
 *
 * @author zhaoyi
 */
public class TaskException extends RuntimeException{

    private static final long serialVersionUID = -751300866993454968L;

    public TaskException() {
        super();
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public TaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     * @param cause
     */
    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public TaskException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public TaskException(Throwable cause) {
        super(cause);
    }
    
}
