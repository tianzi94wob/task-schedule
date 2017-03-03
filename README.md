# task-schedule
 基于zookeeper+quartz的分布式任务调度组件，非常小巧，使用简单，只需要引入jar包，不需要单独部署服务端。确保所有任务在集群中均匀分布，不重复，不遗漏的执行。
 支持动态添加和删除任务。


# 功能概述

1. 基于zookeeper+quartz的分布任务调度系统。
2. 确保每个任务在集群节点均匀分布，上不重复不遗漏的执行。
3. 单个任务节点故障时自动转移到其他任务节点继续执行。
4. 任务节点启动时必须保证zookeeper可用，任务节点运行期zookeeper集群不可用时任务节点保持可用前状态运行，zookeeper集群恢复正常运期。
5. 添加ip黑名单，过滤不需要执行任务的节点。
6. 管理页面，支持动态添加和删除任务。

------------------------------------------------------------------------
## spring配置

 <context:component-scan base-package="cn.com.citycloud.frame.task" />
 
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
              <entry key="zkConnectString" value="${zookeeper.address}" />
              <entry key="rootPath" value="/task1.0/schedule" />
              <entry key="zkSessionTimeout" value="60000" />
              <entry key="userName" value="test" />
              <entry key="password" value="test" />
           </map>
    	</property>
	</bean>
	
## API

1 动态添加任务

ConsoleManager.addScheduleTask(TaskDefine taskDefine);

2 动态删除任务

ConsoleManager.delScheduleTask(String targetBean, String targetMethod);

3 查询任务列表

ConsoleManager.queryScheduleTask();

------------------------------------------------------------------------

	
# task-schedule管理后台

访问URL：项目名称/taskSchedule/home，如果servlet3.x以下，请手动配置web.xml文件
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
	
------------------------------------------------------------------------

# 关于

改进于uncode-schedule，作者：冶卫军（ywj_316@qq.com,微信:yeweijun）

作者：赵毅（531559024@qq.com）
