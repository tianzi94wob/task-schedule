package cn.com.citycloud.frame.task.dao.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import cn.com.citycloud.frame.task.dao.TaskScheduleJobDao;
import cn.com.citycloud.frame.task.entity.TaskScheduleJob;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class TaskScheduleJobDaoJDBCImpl implements TaskScheduleJobDao {
    
    public static final String TABLE_NAME="sys_task_schedule_job";
    
    private TaskScheduleJobRowMapper accessTokenRowMapper = new TaskScheduleJobRowMapper();
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.dao.TaskScheduleJobDao#queryTaskScheduleJobList(cn.com.citycloud.frame.task.entity.TaskScheduleJob)
     */
    @Override
    public List<TaskScheduleJob> queryTaskScheduleJobList(TaskScheduleJob jobCond) {
        StringBuffer sqlBuffer=new StringBuffer();
        sqlBuffer.append(" select * from ");
        sqlBuffer.append(TABLE_NAME);
        
        sqlBuffer.append(" where job_status!=2 ");
        
        List<Object> paramList=new ArrayList<Object>();
        if(!(StringUtils.isEmpty(jobCond.getBeanClass())&&
                StringUtils.isEmpty(jobCond.getJobGroup())&&
                StringUtils.isEmpty(jobCond.getJobName())&&
                StringUtils.isEmpty(jobCond.getJobStatus())&&
                StringUtils.isEmpty(jobCond.getPrjName())&&
                StringUtils.isEmpty(jobCond.getSpringId()))){
            
            if(!StringUtils.isEmpty(jobCond.getBeanClass())){
                paramList.add(jobCond.getBeanClass());
                sqlBuffer.append(" and bean_class = ? ");
            }
            if(!StringUtils.isEmpty(jobCond.getJobGroup())){
                paramList.add(jobCond.getJobGroup());
                sqlBuffer.append(" and job_group = ? ");
            }
            if(!StringUtils.isEmpty(jobCond.getJobName())){
                paramList.add(jobCond.getJobName());
                sqlBuffer.append(" and job_name = ? ");
            }
            if(!StringUtils.isEmpty(jobCond.getJobStatus())){
                paramList.add(jobCond.getJobStatus());
                sqlBuffer.append(" and job_status = ? ");
            }
            if(!StringUtils.isEmpty(jobCond.getPrjName())){
                paramList.add(jobCond.getPrjName());
                sqlBuffer.append(" and prj_name = ? ");
            }
            if(!StringUtils.isEmpty(jobCond.getSpringId())){
                paramList.add(jobCond.getSpringId());
                sqlBuffer.append(" and spring_id = ? ");
            }
            
        }
        final List<TaskScheduleJob> list = jdbcTemplate.query(sqlBuffer.toString(), accessTokenRowMapper,  (Object[])paramList.toArray(new Object[paramList.size()]));
        return list;
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.dao.TaskScheduleJobDao#insert(cn.com.citycloud.frame.task.entity.TaskScheduleJob)
     */
    @Override
    public int insert(final TaskScheduleJob job) {
        StringBuffer sqlBuffer=new StringBuffer();
        sqlBuffer.append(" insert into ");
        sqlBuffer.append(TABLE_NAME);
        sqlBuffer.append("(job_name, job_status, job_group, cron_expression, bean_class, spring_id, method_name, is_concurrent, description, create_time , prj_name)");
        sqlBuffer.append(" values (?,?,?,?,?,?,?,?,?,?,?) ");

        return jdbcTemplate.update(sqlBuffer.toString(), new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, job.getJobName());
                ps.setString(2, job.getJobStatus());
                ps.setString(3, job.getJobGroup());
        
                ps.setString(4, job.getCronExpression());
                ps.setString(5, job.getBeanClass());
                ps.setString(6, job.getSpringId());
        
                ps.setString(7, job.getMethodName());
                ps.setString(8, job.getIsConcurrent());
                ps.setString(9, job.getDescription());
                
                ps.setTimestamp(10, new java.sql.Timestamp(job.getCreateTime().getTime()));
                ps.setString(11, job.getPrjName());
            }
        });
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.dao.TaskScheduleJobDao#selectById(java.lang.Long)
     */
    @Override
    public TaskScheduleJob selectById(Long id) {
        final String sql = " select * from "+TABLE_NAME+" where id = ? ";
        final List<TaskScheduleJob> list = jdbcTemplate.query(sql, accessTokenRowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /* (non-Javadoc)
     * @see cn.com.citycloud.frame.task.dao.TaskScheduleJobDao#updateSelectiveById(cn.com.citycloud.frame.task.entity.TaskScheduleJob)
     */
    @Override
    public int updateSelectiveById(final TaskScheduleJob job) {
        StringBuffer sqlBuffer=new StringBuffer();
        sqlBuffer.append(" update ");
        sqlBuffer.append(TABLE_NAME);
        sqlBuffer.append(" set id = ?");
        
        final Map<String, Object> map=new LinkedHashMap<String, Object>();
        
        if(!StringUtils.isEmpty(job.getBeanClass())){
            map.put("bean_class", job.getBeanClass());
            sqlBuffer.append(" , bean_class = ? ");
        }
        if(!StringUtils.isEmpty(job.getJobGroup())){
            map.put("job_group", job.getJobGroup());
            sqlBuffer.append(" , job_group = ? ");
        }
        if(!StringUtils.isEmpty(job.getJobName())){
            map.put("job_name", job.getJobName());
            sqlBuffer.append(" , job_name = ? ");
        }
        if(!StringUtils.isEmpty(job.getJobStatus())){
            map.put("job_status", job.getJobStatus());
            sqlBuffer.append(" , job_status = ? ");
        }
        if(!StringUtils.isEmpty(job.getPrjName())){
            map.put("prj_name", job.getPrjName());
            sqlBuffer.append(" , prj_name = ? ");
        }
        if(!StringUtils.isEmpty(job.getSpringId())){
            map.put("spring_id", job.getSpringId());
            sqlBuffer.append(" , spring_id = ? ");
        }
        if(!StringUtils.isEmpty(job.getCronExpression())){
            map.put("cron_expression", job.getCronExpression());
            sqlBuffer.append(" , cron_expression = ? ");
        }
        if(!StringUtils.isEmpty(job.getMethodName())){
            map.put("method_name", job.getMethodName());
            sqlBuffer.append(" , method_name = ? ");
        }
        if(!StringUtils.isEmpty(job.getDescription())){
            map.put("description", job.getDescription());
            sqlBuffer.append(" , description = ? ");
        }
        if(job.getUpdateTime()!=null){
            map.put("update_time", job.getUpdateTime());
            sqlBuffer.append(" , update_time = ? ");
        }
        
        sqlBuffer.append(" where id = ?");
        
        return jdbcTemplate.update(sqlBuffer.toString(), new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setLong(1, job.getId());
                
                Set<Map.Entry<String, Object>> entryseSet=map.entrySet();
                int index=2;
                for (Map.Entry<String, Object> entry:entryseSet) {
                    if("update_time".equals(entry.getKey())){
                        ps.setTimestamp(index, new java.sql.Timestamp(job.getUpdateTime().getTime()));
                    }else {
                        ps.setString(index, (String)entry.getValue());
                    }

                    index++;
                }
                ps.setLong(index, job.getId());
            }
        });
    }

}
