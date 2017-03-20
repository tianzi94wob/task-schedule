task-schedule
====================
基于zookeeper+quartz的分布式任务调度组件，非常小巧，使用简单，只需要引入jar包，不需要单独部署服务端。确保所有任务在集群中均匀分布，不重复，不遗漏的执行。
支持动态添加和删除任务。

功能概述
====================
1. 基于zookeeper+quartz的分布任务调度系统，适合多任务的系统使用，合理分配资源。
2. 确保每个任务在集群节点均匀分布，不重复不遗漏的执行。
3. 单个任务节点故障时自动转移到其他任务节点继续执行。
4. 任务节点启动时必须保证zookeeper可用，任务节点运行期zookeeper集群不可用时任务节点保持可用前状态运行，zookeeper集群恢复正常运期。
5. 添加ip黑名单，过滤不需要执行任务的节点。
6. 提供简单管理页面、任务管理的接口。
7. 任务持久化。

模块架构
====================
![image](https://github.com/tianzi94wob/task-schedule/blob/master/src/main/resources/view/images/task.png)

![image](https://github.com/tianzi94wob/task-schedule/blob/master/src/main/resources/view/images/zujian.png)

![image](https://github.com/tianzi94wob/task-schedule/blob/master/src/main/resources/view/images/zk.png)

			
管理页面
=========================================
![image](https://github.com/tianzi94wob/task-schedule/blob/master/src/main/resources/view/images/admin.png)

访问URL：项目名称/taskSchedule/index，如果servlet3.x以下，请手动配置web.xml文件
```
    <servlet>
	    <servlet-name>TaskSchedule</servlet-name>
	    <servlet-class>cn.com.citycloud.frame.task.web.HomeServlet</servlet-class>
	</servlet>
	<servlet-mapping>
	    <servlet-name>TaskSchedule</servlet-name>
	    <url-pattern>/taskSchedule/*</url-pattern>
	</servlet-mapping>
```

任务持久化脚本
====================
	-- ----------------------------
	-- Table structure for `sys_task_schedule_job`
	-- ----------------------------
	DROP TABLE IF EXISTS `sys_task_schedule_job`;
	CREATE TABLE `sys_task_schedule_job` (
	    `id` int(11) NOT NULL AUTO_INCREMENT,
	    `job_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
	    `job_status` varchar(1) DEFAULT NULL COMMENT '任务状态 0-停止，1-运行，2-删除',
	    `job_group` varchar(255) NOT NULL COMMENT '任务组',
	    `cron_expression` varchar(255) DEFAULT NULL COMMENT '表达式',
	    `bean_class` varchar(255) DEFAULT NULL COMMENT '类路径',
	    `spring_id` varchar(255) DEFAULT NULL COMMENT 'springId',
	    `method_name` varchar(255) DEFAULT NULL COMMENT '方法名',
	    `is_concurrent` varchar(1) DEFAULT NULL COMMENT '是否同步',
	    `description` varchar(500) DEFAULT NULL COMMENT '描述',
	    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
	    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
	    `prj_name` varchar(50) DEFAULT NULL COMMENT '工程名',
	    PRIMARY KEY (`id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='任务调度配置表';

	-- ----------------------------
	-- Records of sys_task_schedule_job
	-- ----------------------------

	INSERT INTO `sys_task_schedule_job` VALUES ('12', 'job-test', '1', 'monitor', '0 0/10 * * * ?', 		'cn.com.citycloud.live.mgr.job', '', 'gogogo', '1', '任务测试', '2017-01-10 17:05:06', '2017-01-12 17:07:34', 'live-mgr');

    新建类，用于测试JOB：
	package cn.com.citycloud.live.mgr.job;
	
	/**
	 * 任务测试类
	 *
	 * @author zhaoyi
	 */
	public class JobTest {

	    public void gogogo(){
		    System.out.println("如果觉得快乐你就拍拍手");
	    }
	}
		
spring配置
====================
        <!-- 扫描路径 -->
	    <context:component-scan base-package="cn.com.citycloud.frame.task"/>

	    <!-- 数据源注入 -->
	    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
		    <property name="dataSource" ref="dataSource"/>
	    </bean>

	    <!-- 定时任务管理类 -->
	    <bean id="schedulerFactoryBean" class="org.springframework.scheduling.quartz.SchedulerFactoryBean">
		    <property name="quartzProperties">
		        <props>
			        <prop key="org.quartz.scheduler.skipUpdateCheck">true</prop> 
		        </props>
		    </property>	
	    </bean>	

	    <!-- 分布式任务管理器-->
	    <bean id="zkScheduleManager" class="cn.com.citycloud.frame.task.ZKScheduleManager" init-method="init">
		    <property name="zkConfig">
		        <map>
		            <entry key="zkConnectString" value="127.0.0.1:2181" /><!-- zk地址 -->
		            <entry key="rootPath" value="/task1.0/schedule" /><!-- 根目录，除非组件升级，尽量不要改动-->
		            <entry key="zkSessionTimeout" value="60000" /><!-- 连接超时时间 -->
		            <entry key="userName" value="test" /><!-- 自定义用户名 -->
		            <entry key="password" value="test" /><!-- 自定义密码 -->
		            <entry key="ipBlacklist" value="127.0.0.2,127.0.0.3" /><!-- 黑名单-->
		        </map>
		    </property>
	    </bean>
	    
	    注：如果觉得日志过多，可以屏蔽，以Logback为例：
	    <logger name="cn.com.citycloud.frame.task" level="info" />

API
====================
1 动态设置任务

ConsoleManager.setScheduleTask(TaskDefine taskDefine);

2 动态删除任务

ConsoleManager.delScheduleTask(String targetBean, String targetMethod);

3 查询任务列表

ConsoleManager.queryScheduleTask();

持久化任务管理
====================
cn.com.citycloud.frame.task.service.TaskScheduleJobService
用户按需使用接口开发持久化任务管理界面

不足
====================
目前从新增任务到调度有1~2秒的延迟，用户在实际业务场景中注意规避这个问题。
我的第一次开源尝试！反反复复修改了好几个月。这个组件应用场景目前还比较单一，可能还会存在一些问题，大家一起来优化吧！

大家都在用
====================
- [千里目直播](http://www.qlmzhibo.com/)
- [苏学网](http://www.jszy1d1.com/)
- [易笔账](http://money.innovatelife.net/)


关于
====================
改进于uncode-schedule，作者：冶卫军（ywj_316@qq.com,微信:yeweijun）

作者：赵毅（531559024@qq.com）

