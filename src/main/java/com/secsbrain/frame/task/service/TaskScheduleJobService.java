package com.secsbrain.frame.task.service;

import java.util.List;

import com.secsbrain.frame.task.core.TaskDefine;
import com.secsbrain.frame.task.entity.TaskScheduleJob;
import com.secsbrain.frame.task.web.TaskBean;

/**
 * TaskScheduleJob 表数据服务层接口
 * 
 * @author zhaoyi
 */
public interface TaskScheduleJobService {
    
    /**
     * 功能描述: <br>
     * 查询持久化任务列表
     * TODO 后期支持分页
     *
     * @param taskScheduleJob
     * @return
     */
    List<TaskScheduleJob> queryTaskScheduleJobList(TaskScheduleJob taskScheduleJob);

    /**
     * 功能描述: <br>
     * 添加持久化任务
     * 1.校验任务参数是否合法
     * 2.入库
     * 
     * @param taskDefine
     * @throws Exception
     */
    void addTask(TaskDefine taskDefine) throws Exception;
    
    /**
     * 功能描述: <br>
     * 通过任务ID更新，工程名、表达式、任务描述。其他关键信息不更新
     *
     * @param taskScheduleJob
     * @throws Exception
     */
    void updateTask(TaskScheduleJob taskScheduleJob) throws Exception;
    
    /**
     * 功能描述: <br>
     * 逻辑删除任务
     *
     * @param jobId
     * @throws Exception
     */
    void deleteTask(Long jobId) throws Exception;
    
    /**
     * 功能描述: <br>
     * 查看任务实际运行的详情
     *
     * @param jobId
     * @throws Exception
     * @return
     */
    TaskBean selectTaskDetail(Long jobId) throws Exception;
    
    /**
     * 功能描述: <br>
     * 更改任务状态
     *
     * @param jobId
     * @param cmd    start:开始任务，stop：停止任务，immed：立即执行
     * @return
     * @throws Exception
     */
    void changeStatus(Long jobId, String cmd) throws Exception;
    
}