package cn.com.citycloud.frame.task.dao;

import java.util.List;

import cn.com.citycloud.frame.task.entity.TaskScheduleJob;

/**
 * TaskScheduleJob Dao
 *
 * @author zhaoyi
 */
public interface TaskScheduleJobDao {

    /**
     * 功能描述: <br>
     * 根据指定的条件查询任务列表
     *
     * @param jobCond
     * @return
     */
    List<TaskScheduleJob> queryTaskScheduleJobList(TaskScheduleJob jobCond);
    
    /**
     * 功能描述: <br>
     * 新增任务
     *
     * @param job
     * @return
     */
    int insert(TaskScheduleJob job);
    
    /**
     * 功能描述: <br>
     * 根据主键查询单条任务
     *
     * @param id
     * @return
     */
    TaskScheduleJob selectById(Long id);
    
    /**
     * 功能描述: <br>
     * 根据主键条件更新任务
     *
     * @param job
     * @return
     */
    int updateSelectiveById(TaskScheduleJob job);
    
    
}
