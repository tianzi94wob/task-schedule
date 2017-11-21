package com.secsbrain.frame.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import com.secsbrain.frame.task.core.ScheduleServer;
import com.secsbrain.frame.task.core.TaskDefine;
import com.secsbrain.frame.task.dao.TaskScheduleJobDao;
import com.secsbrain.frame.task.dao.jdbc.TaskScheduleJobDaoJDBCImpl;
import com.secsbrain.frame.task.entity.TaskScheduleJob;

/**
 * 调度器核心管理
 * 
 * @author juny.ye
 * @author zhaoyi
 */
public class ZKScheduleManager implements ApplicationContextAware{

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ZKScheduleManager.class);

    private Map<String, String> zkConfig;//zk配置

    protected ZKManager zkManager;//zk连接

    private ScheduleDataManager4ZK scheduleDataManager;//调度任务zk管理类
    
    private static ApplicationContext applicationContext;//spring 上下文
    
    /**
     * 当前调度服务的信息
     */
    protected ScheduleServer currenScheduleServer;

    /**
     * 是否启动调度管理，如果只是做系统管理，应该设置为false
     */
    public boolean start = true;

    /**
     * 心跳间隔
     */
    private int timerInterval = 2000;

    /**
     * 是否注册成功
     */
    private boolean isScheduleServerRegister = false;

    private Timer hearBeatTimer;
    private Lock initLock = new ReentrantLock();
    private boolean isStopSchedule = false;
    private Lock registerLock = new ReentrantLock();

    private volatile String errorMessage = "No config Zookeeper connect information";

    public ZKScheduleManager() {
        this.currenScheduleServer = ScheduleServer.createScheduleServer(null);
    }

    public void init() throws Exception {
        LOGGER.info("Task Schedule Init......");
        
        Properties properties = new Properties();
        for (Map.Entry<String, String> e : this.zkConfig.entrySet()) {
            properties.put(e.getKey(), e.getValue());
        }
        this.init(properties);
    }

    public void reInit(Properties p) throws Exception {
        if (this.start || this.hearBeatTimer != null) {
            throw new Exception("调度器有任务处理，不能重新初始化");
        }
        this.init(p);
    }

    private void init(Properties p) throws Exception {
        this.initLock.lock();
        try {
            this.scheduleDataManager = null;
            if (this.zkManager != null) {
                this.zkManager.close();
            }
            this.zkManager = new ZKManager(p);//连接zookeeper
            this.errorMessage = "Zookeeper connecting ......" + this.zkManager.getConnectStr();

            int count = 0;
            while (!this.zkManager.checkZookeeperState()) {
                count = count + 1;
                if (count % 50 == 0) {
                    this.errorMessage = "Zookeeper connecting ......" + this.zkManager.getConnectStr() + " spendTime:"
                            + count * 20 + "(ms)";
                    LOGGER.error(this.errorMessage);
                }
                Thread.sleep(20);
            }
            this.initialData();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            this.initLock.unlock();
        }
    }

    /**
     * 在Zk状态正常后回调数据初始化
     * 
     * @throws Exception
     */
    private void initialData() throws Exception {
        this.zkManager.initial();// 初始化根节点,task-schedule-1.0.0
        this.scheduleDataManager = new ScheduleDataManager4ZK(this.zkManager);// 实例化管理组件，初始化子节点server,task
       
        this.synchronizeDBJob();//同步数据库的任务
        
        if (this.start) {
            // 注册调度管理器
            this.scheduleDataManager.registerScheduleServer(this.currenScheduleServer);
            if (hearBeatTimer == null) {
                hearBeatTimer = new Timer("ScheduleManager-" + this.currenScheduleServer.getUuid() + "-HearBeat");
            }
            hearBeatTimer.schedule(new HeartBeatTimerTask(this), 2000, this.timerInterval);
        }
        
        LOGGER.info("Task Schedule Init Success!");
    }

    /**
     * 功能描述: <br>
     * 同步数据库任务
     * @throws Exception 
     *
     */
    private void synchronizeDBJob() throws Exception{
        TaskScheduleJobDao taskScheduleJobDao=applicationContext.getBean(TaskScheduleJobDaoJDBCImpl.class);
        TaskScheduleJob taskScheduleJob=new TaskScheduleJob();
        taskScheduleJob.setPrjName(getAppName());
        List<TaskScheduleJob> jobList = taskScheduleJobDao.queryTaskScheduleJobList(taskScheduleJob);
        List<TaskDefine> taskList = this.scheduleDataManager.getAllTask();
        if(!CollectionUtils.isEmpty(jobList)){
            if(!CollectionUtils.isEmpty(taskList)){
                List<String> taskKeyNameRemainList=new ArrayList<String>();//与数据库不一致的任务，需要删除的
                for(TaskDefine taskDefine:taskList){
                    if(!TaskDefine.ZK_TASK_TYPE.equals(taskDefine.getType())){
                        taskKeyNameRemainList.add(taskDefine.stringKey());
                    }
                }
                
                for (TaskDefine taskDefineOrg : taskList) {
                    for (TaskScheduleJob job : jobList) {
                        TaskDefine taskDefine=new TaskDefine(job);
                        String taskKeyName = taskDefineOrg.stringKey();
                        if(taskKeyName!=null&&taskKeyName.equals(taskDefine.stringKey())&&!TaskDefine.ZK_TASK_TYPE.equals(taskDefineOrg.getType())){
                            taskKeyNameRemainList.remove(taskKeyName);
                        }
                    }
                }
                
                //删除多余的任务
                for(String taskKeyName : taskKeyNameRemainList){
                    LOGGER.info("删除跟数据库不匹配的目录:{}",taskKeyName);
                    this.scheduleDataManager.delTask(taskKeyName);
                }
                
            }
            
            //添加任务调度
            for (TaskScheduleJob job : jobList) {
                TaskDefine taskDefine=new TaskDefine(job);
                LOGGER.info("设置任务信息:{}",taskDefine.stringKey());
                try {
                    scheduleDataManager.settingTask(taskDefine);
                } catch (Exception e) {
                    LOGGER.error("",e);
                }
            }
            
        }else{
            for (TaskDefine taskDefine:taskList) {
                if(!TaskDefine.ZK_TASK_TYPE.equals(taskDefine.getType())){
                    LOGGER.info("删除跟数据库不匹配的目录:{}",taskDefine.stringKey());
                    this.scheduleDataManager.delTask(taskDefine.stringKey());
                }
            }
        }
    }
    
    /**
     * 功能描述: <br>
     * 获取应用名称
     *
     * @return
     */
    public static String getAppName(){
        String appName=applicationContext.getApplicationName();
        if(StringUtils.isEmpty(appName)){
            return "BLANK";
        }
        return appName.substring(1, appName.length());
    }
    
    /**
     * 功能描述: <br>
     * 重写调度server信息
     * 
     * @throws Exception
     */
    private void rewriteScheduleInfo() throws Exception {
        registerLock.lock();
        try {
            LOGGER.debug("重写调度信息......{}", currenScheduleServer.getUuid());

            if (this.isStopSchedule) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("外部命令终止调度,不再注册调度服务，避免遗留垃圾数据：" + currenScheduleServer.getUuid());
                }
                return;
            }
            // 先发送心跳信息
            if (errorMessage != null) {
                this.currenScheduleServer.setDealInfoDesc(errorMessage);
            }
            if (!this.scheduleDataManager.refreshScheduleServer(this.currenScheduleServer)) {
                // 更新信息失败，清除内存数据后重新注册
                this.clearMemoInfo();
                this.scheduleDataManager.registerScheduleServer(this.currenScheduleServer);
            }
            isScheduleServerRegister = true;
        } finally {
            registerLock.unlock();
        }
    }

    /**
     * 清除内存中所有的已经取得的数据和任务队列,在心跳更新失败，或者发现注册中心的调度信息被删除
     */
    public void clearMemoInfo() {
        try {

        } finally {
        }
    }

    /**
     * 定时向数据配置中心更新当前服务器的心跳信息。 如果发现本次更新的时间如果已经超过了，服务器死亡的心跳周期，则不能在向服务器更新信息。 而应该当作新的服务器，进行重新注册。
     * 
     * @throws Exception
     */
    public void refreshScheduleServer() throws Exception {
        try {
            // 重写调度信息
            rewriteScheduleInfo();
            // 如果任务信息没有初始化成功，不做任务相关的处理
            if (!this.isScheduleServerRegister) {
                return;
            }

            // 重新分配任务-zk（leader分配）
            this.assignScheduleTask();
            // 检查本地任务
            this.checkLocalTask();
        } catch (Exception e) {
            // 清除内存中所有的已经取得的数据和任务队列,避免心跳线程失败时候导致的数据重复
            this.clearMemoInfo();
            if (e instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new Exception(e.getMessage(), e);
            }
        }
    }
    
    /**
     * 根据当前调度服务器的信息，重新计算分配所有的调度任务
     * 
     * 1、清除已经超过心跳周期的服务器注册信息
     * 2、获取所有的服务器注册信息和任务队列信息
     * 3、重新计算任务分配
     * 
     * @throws Exception
     */
    public void assignScheduleTask() throws Exception {
        scheduleDataManager.clearExpireScheduleServer();// 清除失效的调度server
        List<String> serverList = scheduleDataManager.loadScheduleServerNames();
        if (!scheduleDataManager.isLeader(this.currenScheduleServer.getUuid(), serverList)) {
            // 不是leader直接返回
            return;
        }
        // 黑名单
        for (String ip : zkManager.getIpBlacklist()) {
            int index = serverList.indexOf(ip);
            if (index > -1) {
                serverList.remove(index);
            }
        }
        // 设置初始化成功标准，避免在leader转换的时候，新增的线程组初始化失败
        scheduleDataManager.assignTask(this.currenScheduleServer.getUuid(), serverList);
    }

    public void checkLocalTask() throws Exception {
        // 检查系统任务执行情况
        scheduleDataManager.checkLocalTask(this.currenScheduleServer.getUuid());
    }
    
    /**
     * 功能描述: <br>
     * 获取当前serverID
     *
     * @return
     */
    public String getCurrenScheduleServerUuid(){
        return this.currenScheduleServer.getUuid();
    }
    
    
    class HeartBeatTimerTask extends java.util.TimerTask {
        private transient final Logger log = LoggerFactory.getLogger(HeartBeatTimerTask.class);
        ZKScheduleManager manager;

        public HeartBeatTimerTask(ZKScheduleManager aManager) {
            manager = aManager;
        }

        public void run() {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                manager.refreshScheduleServer();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    public ScheduleDataManager4ZK getScheduleDataManager() {
        return scheduleDataManager;
    }

    public void setZkManager(ZKManager zkManager) {
        this.zkManager = zkManager;
    }

    public ZKManager getZkManager() {
        return zkManager;
    }

    public void setZkConfig(Map<String, String> zkConfig) {
        this.zkConfig = zkConfig;
    }

    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ZKScheduleManager.applicationContext = applicationContext;
    }
    
    public static ApplicationContext getApplicationcontext() {
        return ZKScheduleManager.applicationContext;
    }

}