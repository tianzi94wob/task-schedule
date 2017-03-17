package cn.com.citycloud.frame.task;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.citycloud.frame.task.common.ResponseVo;
import cn.com.citycloud.frame.task.core.ScheduleServer;
import cn.com.citycloud.frame.task.core.TaskDefine;
import cn.com.citycloud.frame.task.core.TaskUtils;
import cn.com.citycloud.frame.task.core.ZkInfo;
import cn.com.citycloud.frame.task.util.UUIDUtils;
import cn.com.citycloud.frame.task.util.VerificationUtil;
import cn.com.citycloud.frame.task.util.ZKTools;
import cn.com.citycloud.frame.task.web.TaskBean;

import com.alibaba.fastjson.JSON;

/**
 * zk实现类
 * 
 * @author zhaoyi
 * 
 */
public class ScheduleDataManager4ZK {
    private static final transient Logger LOG = LoggerFactory.getLogger(ScheduleDataManager4ZK.class);

    public static final String CHARSET = "UTF-8";

    private static final String NODE_SERVER = "server";
    private static final String NODE_TASK = "task";
    
    /**负责运行的机器*/
    private static final String SUB_NODE_SERVER = "server";
    /**任务最近一次耗时毫秒数*/
    private static final String SUB_NODE_MSEC = "msec";
    /**任务调度的状态*/
    private static final String SUB_NODE_STATUS = "status";
    /**任务指令*/
    private static final String SUB_NODE_INSTRUCT= "instruct";
    /**上次运行时间*/
    private static final String SUB_NODE_LAST = "lastRuningTime";
    /**下次运行时间*/
    private static final String SUB_NODE_NEXT = "nextRuningTime";
    /**执行总次数*/
    private static final String SUB_NODE_TIMES = "times";
    
    private static final long SERVER_EXPIRE_TIME = 5000 * 3;
    private ZKManager zkManager;
    private String pathServer;
    private String pathTask;
    private long zkBaseTime = 0;
    private long loclaBaseTime = 0;

    public ScheduleDataManager4ZK(ZKManager aZkManager) throws Exception {
        String appName = ZKScheduleManager.getAppName();
        this.zkManager = aZkManager;
        this.pathServer = this.zkManager.getRootPath() + "/" + NODE_SERVER + "/" + appName;
        this.pathTask = this.zkManager.getRootPath() + "/" + NODE_TASK + "/" + appName;
        if (this.getZooKeeper().exists(this.pathServer, false) == null) {
            ZKTools.createPath(getZooKeeper(), this.pathServer, CreateMode.PERSISTENT, this.zkManager.getAcl());
        }
        if (this.getZooKeeper().exists(this.pathTask, false) == null) {
            ZKTools.createPath(getZooKeeper(), this.pathTask, CreateMode.PERSISTENT, this.zkManager.getAcl());
        }
        loclaBaseTime = System.currentTimeMillis();
        String tempPath = this.zkManager.getZooKeeper().create(this.zkManager.getRootPath() + "/systime", null,
                this.zkManager.getAcl(), CreateMode.EPHEMERAL_SEQUENTIAL);
        Stat tempStat = this.zkManager.getZooKeeper().exists(tempPath, false);
        zkBaseTime = tempStat.getCtime();
        ZKTools.deleteTree(getZooKeeper(), tempPath);
        if (Math.abs(this.zkBaseTime - this.loclaBaseTime) > 5000) {
            LOG.error("请注意，Zookeeper服务器时间与本地时间相差 ： " + Math.abs(this.zkBaseTime - this.loclaBaseTime) + " ms");
        }
    }

    public ZooKeeper getZooKeeper() throws Exception {
        return this.zkManager.getZooKeeper();
    }

    /**
     * 功能描述: <br>
     * 刷新schedule server的数据
     * 
     * @param server
     * @return
     * @throws Exception
     */
    public boolean refreshScheduleServer(ScheduleServer server) throws Exception {
        Timestamp heartBeatTime = new Timestamp(this.getSystemTime());
        String zkPath = this.pathServer + "/" + server.getUuid();
        if (this.getZooKeeper().exists(zkPath, false) == null) {
            // 数据可能被清除，先清除内存数据后，重新注册数据
            server.setRegister(false);
            return false;
        }
        Timestamp oldHeartBeatTime = server.getHeartBeatTime();
        server.setHeartBeatTime(heartBeatTime);// 心跳时间
        server.setVersion(server.getVersion() + 1);// 版本号
        String valueString = JSON.toJSONString(server);
        try {
            this.getZooKeeper().setData(zkPath, valueString.getBytes(CHARSET), -1);
        } catch (Exception e) {
            // 恢复上次的心跳时间
            server.setHeartBeatTime(oldHeartBeatTime);
            server.setVersion(server.getVersion() - 1);
            throw e;
        }
        return true;
    }

    public void registerScheduleServer(ScheduleServer server) throws Exception {
        LOG.info("注册任务节点：{}成功！", server.getUuid());
        if (server.isRegister()) {
            throw new Exception(server.getUuid() + " 被重复注册");
        }
        String realPath;
        // 此处必须增加UUID作为唯一性保障
        StringBuffer id = new StringBuffer();
        id.append(server.getIp()).append("$").append(UUIDUtils.getUUID());
        String zkServerPath = pathServer + "/" + id.toString() + "$";
        realPath = this.getZooKeeper().create(zkServerPath, null, this.zkManager.getAcl(),
                CreateMode.PERSISTENT_SEQUENTIAL);
        server.setUuid(realPath.substring(realPath.lastIndexOf("/") + 1));

        Timestamp heartBeatTime = new Timestamp(getSystemTime());
        server.setHeartBeatTime(heartBeatTime);

        String valueString = JSON.toJSONString(server);
        this.getZooKeeper().setData(realPath, valueString.getBytes(CHARSET), -1);
        server.setRegister(true);
    }

    /**
     * 功能描述: <br>
     * 清除失效的调度server
     * 
     * @throws Exception
     */
    public void clearExpireScheduleServer() throws Exception {
        String zkPath = this.pathServer;
        if (this.getZooKeeper().exists(zkPath, false) == null) {
            this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
        }
        for (String name : this.zkManager.getZooKeeper().getChildren(zkPath, false)) {
            try {
                Stat stat = new Stat();
                this.getZooKeeper().getData(zkPath + "/" + name, null, stat);
                if (getSystemTime() - stat.getMtime() > SERVER_EXPIRE_TIME) {
                    ZKTools.deleteTree(this.getZooKeeper(), zkPath + "/" + name);
                    LOG.info("ScheduleServer[" + zkPath + "/" + name + "]过期清除");
                }
            } catch (Exception e) {
                // 当有多台服务器时，存在并发清理的可能，忽略异常
            }
        }
    }

    /**
     * 功能描述: <br>
     * 获取所有的task
     * 
     * @return
     * @throws Exception
     */
    public List<TaskDefine> getAllTask() throws Exception {
        String zkPath = this.pathTask;
        // get all task
        List<String> children = this.getZooKeeper().getChildren(zkPath, false);
        
        List<TaskDefine> taskList = new ArrayList<TaskDefine>();
        
        for(String taskKey : children){
            String taskPath = zkPath + "/" + taskKey;
            byte[] data = this.getZooKeeper().getData(taskPath, null, null);
            if (null != data) {
                String json = new String(data, CHARSET);
                TaskDefine taskDefine = null;
                try {
                    taskDefine = JSON.parseObject(json, TaskDefine.class);
                    taskList.add(taskDefine);
                } catch (Exception e) {
                }
            }
        }
        return taskList;
    }

    /**
     * 功能描述: <br>
     * 卸载服务 ，暂时没用到
     *
     * @param server
     * @throws Exception
     */
    public void unRegisterScheduleServer(ScheduleServer server) throws Exception {
        List<String> serverList = this.loadScheduleServerNames();

        if (server.isRegister() && this.isLeader(server.getUuid(), serverList)) {
            // delete task
            String zkPath = this.pathTask;
            String serverPath = this.pathServer;

            if (this.getZooKeeper().exists(zkPath, false) == null) {
                this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
            }

            // get all task
            List<String> children = this.getZooKeeper().getChildren(zkPath, false);
            if (null != children && children.size() > 0) {
                for (String taskName : children) {
                    String taskPath = zkPath + "/" + taskName;
                    if (this.getZooKeeper().exists(taskPath, false) != null) {
                        ZKTools.deleteTree(this.getZooKeeper(), taskPath + "/" + server.getUuid());
                    }
                }
            }

            // 删除
            if (this.getZooKeeper().exists(this.pathServer, false) == null) {
                ZKTools.deleteTree(this.getZooKeeper(), serverPath + serverPath + "/" + server.getUuid());
            }
            server.setRegister(false);
        }
    }

    public List<String> loadScheduleServerNames() throws Exception {
        String zkPath = this.pathServer;
        if (this.getZooKeeper().exists(zkPath, false) == null) {
            return new ArrayList<String>();
        }
        List<String> serverList = this.getZooKeeper().getChildren(zkPath, false);
        Collections.sort(serverList, new Comparator<String>() {
            public int compare(String u1, String u2) {
                return u1.substring(u1.lastIndexOf("$") + 1).compareTo(u2.substring(u2.lastIndexOf("$") + 1));
            }
        });
        return serverList;
    }

    /**
     * 功能描述: <br>
     * 平均的给任务分配server task1-->server1 task2-->server2 task3-->server3 task4-->server1 task5-->server2 task6-->server3
     * 
     * @param taskServerList 所有有效的server列表
     * @param taskPathList 所有task列表
     * @throws Exception
     */
    private void assignServerAvg(List<String> taskServerList, List<String> taskPathList) throws Exception {
        String zkPath = this.pathTask;

        int taskCount = 0, serverCount = 0;
        if (taskPathList.size() <= taskServerList.size()) {
            while (taskCount < taskPathList.size()) {
                assignServer(zkPath, taskServerList, taskPathList, taskCount, serverCount);
                taskCount++;
                serverCount++;
            }
        } else {
            while (taskCount < taskPathList.size()) {
                assignServer(zkPath, taskServerList, taskPathList, taskCount, serverCount);
                taskCount++;
                serverCount++;
                if (serverCount == taskServerList.size()) {
                    serverCount = 0;
                }
            }
        }
    }
    
    private void assignServer(String zkPath,List<String> taskServerList, List<String> taskPathList,int taskCount,int serverCount) throws Exception {
        String serverId=taskServerList.get(serverCount);
        
        List<ZkInfo> zkInfoList=new ArrayList<ZkInfo>();
        zkInfoList.add(new ZkInfo(zkPath + "/" + taskPathList.get(taskCount) + "/" + SUB_NODE_SERVER, serverId));
        zkInfoList.add(new ZkInfo(zkPath + "/" + taskPathList.get(taskCount) + "/" + SUB_NODE_STATUS, JobEnum.ScheduleStatus.TASK_STATUS_WAIT.getCode()));
        ZKTools.multiSetData(this.getZooKeeper(), zkInfoList);
        LOG.debug("Assign server [" + taskServerList.get(serverCount) + "]" + " to task ["
                + taskPathList.get(taskCount) + "]");
    }

    /**
     * 功能描述: <br>
     * 是否需要重新给task分配server
     * 
     * @param taskServerList 所有server列表
     * @param taskPathList 所有task列表
     * @return
     * @throws Exception
     */
    private boolean needReAssign(List<String> taskServerList, List<String> taskPathList) throws Exception {
        boolean flag = false;
        String zkPath = this.pathTask;
        // 计算每个server对应task的数量
        Map<String, Integer> serverCountMap = new HashMap<String, Integer>();
        for (String taskName : taskPathList) {
            String taskPath = zkPath + "/" + taskName;
            // task节点下的server节点
            byte[] serverVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_SERVER, null, null);
            String serverId = new String(serverVal);

            if ("0".equals(serverVal)) {
                // task下没有server，自然需要重新分配任务
                LOG.info("task下没有server，重新分配server......task,{}", taskPath);
                return true;
            }

            if(!taskServerList.contains(serverId)){
                // task下的server不在当前server列表，需要重新分配任务
                LOG.info("task下的server失效了，重新分配server......server,{}", serverId);
                return true;
            }
            
            if (serverCountMap.containsKey(serverId)) {
                serverCountMap.put(serverId, serverCountMap.get(serverId) + 1);
            } else {
                serverCountMap.put(serverId, 1);
            }
        }
        LOG.debug("每个server对应task的数量：{}", serverCountMap);

        int taskCount = taskPathList.size();
        int serverCount = taskServerList.size();

        LOG.debug("task总数：" + taskCount + "，server总数：" + serverCount);
        int count = taskCount / serverCount;

        if(count > 0){
            for (String taskServer : taskServerList) {
                if (!serverCountMap.containsKey(taskServer)) {
                    // 如果在task大于server的情况下，有server没有分配上，那就有问题
                    LOG.info("有server没有分配上，重新分配server......{}", taskServer);
                    flag = true;
                    break;
                }

                // 判断是否平均分配
                if (serverCountMap.get(taskServer) > ((taskCount % serverCount) == 0 ? count : count + 1)) {
                    // 没有平均分配到各个server
                    LOG.info("task对应的server没有平均分配，重新分配server......serverCountMap:"+serverCountMap+",taskCount:"+taskCount);
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    /**
     * 功能描述: <br>
     * 分派任务
     * 
     * @param currentUuid 当前serverID
     * @param taskServerList 所有server列表
     * @throws Exception
     */
    public void assignTask(String currentUuid, List<String> taskServerList) throws Exception {
        if (!this.isLeader(currentUuid, taskServerList)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(currentUuid + ":不是负责任务分配的Leader,直接返回");
            }
            return;
        }

        LOG.debug(currentUuid + ":我是leader，我正在执行分派任务");

        if (taskServerList.size() <= 0) {
            // 在服务器动态调整的时候，可能出现服务器列表为空的清空
            return;
        }
        if (this.zkManager.checkZookeeperState()) {
            String zkPath = this.pathTask;
            if (this.getZooKeeper().exists(zkPath, false) == null) {
                this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
            }
            List<String> taskPathList = this.getZooKeeper().getChildren(zkPath, false);
            if (null != taskPathList && taskPathList.size() > 0) {
                if (needReAssign(taskServerList, taskPathList)) {
                    // 如果task对应的server分配不均匀了，则需要重新分配
                    assignServerAvg(taskServerList, taskPathList);
                } else {
                    for (String taskName : taskPathList) {
                        String taskPath = zkPath + "/" + taskName;
                        // task节点下的server节点
                        byte[] serverVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_SERVER, null, null);
                        String serverId = new String(serverVal);// 每个task下只允许有一个server，直接这样取就好
                        if (!taskServerList.contains(serverId)) {
                            LOG.info("task下的server失效了，重新分配server......{}", currentUuid);
                            assignServerAvg(taskServerList, taskPathList);
                        } else {
                            // 不需要重置本地任务的状态
                            this.getZooKeeper().setData(taskPath + "/" + SUB_NODE_STATUS, JobEnum.ScheduleStatus.TASK_STATUS_NOMAL.getCode().getBytes(), -1);
                        }
                    }
                }
            } else {
                LOG.debug(currentUuid + ":没有集群任务");
            }
        }

    }

    public boolean isLeader(String uuid, List<String> serverList) {
        return uuid.equals(getLeader(serverList));
    }

    private String getLeader(List<String> serverList) {
        if (serverList == null || serverList.size() == 0) {
            return "";
        }
        long no = Long.MAX_VALUE;
        long tmpNo = -1;
        String leader = null;
        for (String server : serverList) {
            tmpNo = Long.parseLong(server.substring(server.lastIndexOf("$") + 1));
            if (no > tmpNo) {
                no = tmpNo;
                leader = server;
            }
        }
        return leader;
    }

    private long getSystemTime() {
        return this.zkBaseTime + (System.currentTimeMillis() - this.loclaBaseTime);
    }

    /**
     * 功能描述: <br>
     * 是否是任务的拥有者
     * 
     * @param name 任务名称
     * @param uuid server的ID
     * @return
     * @throws Exception
     */
    public boolean isOwner(String name, String uuid) {
        boolean isOwner = false;
        try {
            String zkPath = this.pathTask + "/" + name;
            // 判断是否分配给当前节点 
            if(this.getZooKeeper().exists(zkPath + "/" + SUB_NODE_SERVER, false) != null){
                byte[] serverVal = this.getZooKeeper().getData(zkPath + "/" + SUB_NODE_SERVER, null, null);
                if (serverVal != null && uuid.equals(new String(serverVal))) {
                    isOwner = true;
                }
            }
        } catch (Exception e) {
            //有脏读的可能，忽略异常
        }
        return isOwner;
    }

    /**
     * 功能描述: <br>
     * 获取下次运行时间
     *
     * @param cronExpression
     * @return
     */
    private long getNextRunTimes(String cronExpression){
        CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
        try {
            cronTriggerImpl.setCronExpression(cronExpression);
        } catch (ParseException e) {
            LOG.error("",e);
        }
        
        List<Date> dates=TriggerUtils.computeFireTimes(cronTriggerImpl, null, 1);
        if(dates.size()>=1){
            return dates.get(0).getTime()<System.currentTimeMillis()?0:dates.get(0).getTime();
        }else{
            return 0;
        }
    }
    
    /**
     * 功能描述: <br>
     * 保存运行信息
     * 
     * @param name
     * @return
     * @throws Exception
     */
    public boolean saveRunningInfo(String taskKey, String uuid) {
        try {
            String zkPath = this.pathTask + "/" + taskKey;
            // 判断是否分配给当前节点
            byte[] serverVal = this.getZooKeeper().getData(zkPath + "/" + SUB_NODE_SERVER, null, null);
            if (serverVal != null && uuid.equals(new String(serverVal))) {
                byte[] timesVal = this.getZooKeeper().getData(zkPath + "/" + SUB_NODE_TIMES, null, null);
                int times=Integer.parseInt(new String(timesVal));
                
                List<ZkInfo> zkInfoList=new ArrayList<ZkInfo>();
                zkInfoList.add(new ZkInfo(zkPath + "/" + SUB_NODE_TIMES, String.valueOf((times+1))));
                zkInfoList.add(new ZkInfo(zkPath + "/" + SUB_NODE_LAST, String.valueOf(System.currentTimeMillis())));
                ZKTools.multiSetData(this.getZooKeeper(), zkInfoList);
            }
        } catch (Exception e) {
            LOG.error("",e);
            return false;
        }
        return true;
    }
    
    /**
     * 功能描述: <br>
     * 保存运行信息
     * 
     * @param name
     * @return
     * @throws Exception
     */
    public boolean saveRunningInfo2(String taskKey,long msc,String uuid) {
        try {
            String zkPath = this.pathTask + "/" + taskKey;
            // 判断是否分配给当前节点
            byte[] serverVal = this.getZooKeeper().getData(zkPath + "/" + SUB_NODE_SERVER, null, null);
            if (serverVal != null && uuid.equals(new String(serverVal))) {
                byte[] data = this.getZooKeeper().getData(zkPath, null, null);
                TaskDefine taskDefine = null;
                try {
                    taskDefine = JSON.parseObject(new String(data, CHARSET), TaskDefine.class);
                } catch (Exception e) {
                    ZKTools.deleteTree(this.getZooKeeper(), zkPath);
                }
                
                List<ZkInfo> zkInfoList=new ArrayList<ZkInfo>();
                zkInfoList.add(new ZkInfo(zkPath + "/" + SUB_NODE_MSEC, String.valueOf(msc)));
                zkInfoList.add(new ZkInfo(zkPath + "/" + SUB_NODE_NEXT, String.valueOf(getNextRunTimes(taskDefine.getCronExpression()))));
                ZKTools.multiSetData(this.getZooKeeper(), zkInfoList);
            }
        } catch (Exception e) {
            LOG.error("",e);
            return false;
        }
        return true;
    }

    public boolean isExistsTask(TaskDefine taskDefine) throws Exception {
        String zkPath = this.pathTask + "/" + taskDefine.stringKey();
        return this.getZooKeeper().exists(zkPath, false) != null;
    }
    
    public void settingTask(TaskDefine taskDefine) throws Exception {
        settingTask(taskDefine,null);
    }
    
    /**
     * 功能描述: <br>
     * 设置任务--for zookeeper
     * 
     * @param taskDefine
     * @throws Exception
     */
    public void settingTask(TaskDefine taskDefine,String instruct) throws Exception {
        VerificationUtil.checkTaskParam(taskDefine, ZKScheduleManager.getApplicationcontext());
        
        List<Op> ops = new ArrayList<Op>();
        
        String zkPath = this.zkManager.getRootPath() + "/" + NODE_TASK + "/" + taskDefine.getPrjName() + "/" + taskDefine.stringKey();
        if (this.getZooKeeper().exists(zkPath, false) == null) {
            ops.add(Op.create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT));
            
            ops.add(Op.create(zkPath + "/" +SUB_NODE_SERVER, "0".getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
            
            ops.add(Op.create(zkPath + "/" +SUB_NODE_MSEC, "0".getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
            ops.add(Op.create(zkPath + "/" +SUB_NODE_NEXT, "0".getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
            ops.add(Op.create(zkPath + "/" +SUB_NODE_LAST, "0".getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
            ops.add(Op.create(zkPath + "/" +SUB_NODE_STATUS, JobEnum.ScheduleStatus.TASK_STATUS_WAIT.getCode().getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
            ops.add(Op.create(zkPath + "/" +SUB_NODE_INSTRUCT, JobEnum.JobInstruct.ZK_NORMAL.getCode().getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
            ops.add(Op.create(zkPath + "/" +SUB_NODE_TIMES, "0".getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT));
        } else {
            byte[] orgData = this.getZooKeeper().getData(zkPath, null, null);
            TaskDefine orgTaskDefine = JSON.parseObject(new String(orgData, CHARSET), TaskDefine.class);
            
            if(!orgTaskDefine.getCronExpression().equals(taskDefine.getCronExpression())){
                instruct = JobEnum.JobInstruct.ZK_UPDATE.getCode();
            }
            
            orgTaskDefine.setParams(taskDefine.getParams());
            orgTaskDefine.setCronExpression(taskDefine.getCronExpression());
            orgTaskDefine.setDescription(taskDefine.getDescription());
            
            taskDefine=orgTaskDefine;
        }
        String json = JSON.toJSONString(taskDefine);
        ops.add(Op.setData(zkPath, json.getBytes(CHARSET), -1));
        
        if(!StringUtils.isEmpty(instruct)){
            ops.add(Op.setData(zkPath + "/" + SUB_NODE_INSTRUCT, instruct.getBytes(), -1));
        }

        this.getZooKeeper().multi(ops);
    }

    public void delTask(TaskDefine taskDefine) throws Exception {
        String zkPath = this.pathTask;
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            zkPath = zkPath + "/" + taskDefine.stringKey();
            if (this.getZooKeeper().exists(zkPath, false) != null) {
                ZKTools.deleteTree(this.getZooKeeper(), zkPath);
            }
        }
    }

    public void delTask(String stringKey) throws Exception {
        String zkPath = this.pathTask;
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            zkPath = zkPath + "/" + stringKey;
            if (this.getZooKeeper().exists(zkPath, false) != null) {
                ZKTools.deleteTree(this.getZooKeeper(), zkPath);
            }
        }
    }
    
    private void parseTaskDefineToBean(TaskBean taskBean,TaskDefine taskDefine){
        taskBean.setParams(taskDefine.getParams());
        taskBean.setTargetBean(taskDefine.getTargetBean());
        taskBean.setTargetMethod(taskDefine.getTargetMethod());
        taskBean.setCronExpression(taskDefine.getCronExpression());
        taskBean.setType(taskDefine.getType());
        taskBean.setJobStatus(taskDefine.getJobStatus());
        
        taskBean.setPrjName(taskDefine.getPrjName());
        taskBean.setDescription(taskDefine.getDescription());
        taskBean.setJobName(taskDefine.getJobName());
        taskBean.setJobGroup(taskDefine.getJobGroup());
    }
    
    private void parseRunningInfoBean(TaskBean taskBean,String taskPath) throws Exception{
        byte[] timesVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_TIMES, null, null);
        byte[] msecVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_MSEC, null, null);
        byte[] lastVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_LAST, null, null);
        byte[] nextVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_NEXT, null, null);
        taskBean.setRunTimes(Integer.parseInt(new String(timesVal)));
        taskBean.setLastRunningTime(Long.parseLong(new String(lastVal)));
        taskBean.setMsc(Long.parseLong(new String(msecVal)));
        
        if(Long.parseLong(new String(nextVal))==0){
            taskBean.setNextRuningTime(getNextRunTimes(taskBean.getCronExpression()));
        }else{
            taskBean.setNextRuningTime(Long.parseLong(new String(nextVal)));
        }
    }

    public List<TaskBean> selectTask() throws Exception {
        String zkPath = this.pathTask;
        List<TaskBean> taskList = new ArrayList<TaskBean>();
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            List<String> childes = this.getZooKeeper().getChildren(zkPath, false);
            for (String child : childes) {
                TaskBean taskBean = new TaskBean();
                if (this.getZooKeeper().exists(zkPath + "/" + child, false) != null) {
                    byte[] data = this.getZooKeeper().getData(zkPath + "/" + child, null, null);
                    String json = new String(data, CHARSET);
                    TaskDefine taskDefine  = JSON.parseObject(json, TaskDefine.class);
                    parseTaskDefineToBean(taskBean, taskDefine);
                } else {
                    String[] names = child.split("#");
                    if (StringUtils.isNotEmpty(names[0])) {
                        taskBean.setTargetBean(names[0]);
                        taskBean.setTargetMethod(names[1]);
                        if(names.length>2){
                            taskBean.setParams(names[2]);
                        }
                    }
                }

                byte[] serverVal = this.getZooKeeper().getData(zkPath + "/" + child + "/" + SUB_NODE_SERVER, null, null);
                taskBean.setCurrentServer(new String(serverVal));
                parseRunningInfoBean(taskBean, zkPath + "/" + child);
                taskList.add(taskBean);
            }
        }
        return taskList;
    }

    /**
     * 功能描述: <br>
     * 通过task key获取task展示对象
     *
     * @param param
     * @return
     * @throws Exception
     */
    public TaskBean selectTaskByName(TaskDefine param) throws Exception {
        String zkPath = this.zkManager.getRootPath() + "/" + NODE_TASK + "/" + param.getPrjName() + "/"
                + param.stringKey();
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            byte[] data = this.getZooKeeper().getData(zkPath, null, null);
            TaskBean taskBean = new TaskBean();
            if (null != data) {
                String json = new String(data, CHARSET);
                TaskDefine taskDefine = JSON.parseObject(json, TaskDefine.class);
                parseTaskDefineToBean(taskBean, taskDefine);

                byte[] serverVal = this.getZooKeeper().getData(zkPath + "/" + SUB_NODE_SERVER, null, null);
                taskBean.setCurrentServer(new String(serverVal));
                parseRunningInfoBean(taskBean, zkPath);
            }
            return taskBean;
        }
        return null;
    }

    /**
     * 功能描述: <br>
     * 各server将各自zk上的任务实际运行
     * 
     * @param currentUuid
     * @return
     * @throws Exception
     */
    public boolean checkLocalTask(String currentUuid) throws Exception {
        LOG.debug(currentUuid + ":检查本地任务");
        if (this.zkManager.checkZookeeperState()) {
            String zkPath = this.pathTask;
            //取所有的任务，没什么问题的。就算有10W+个任务，也不会占多少内存（只是取个名字）
            List<String> children = this.getZooKeeper().getChildren(zkPath, false);
            List<String> ownerTask = new ArrayList<String>();
            if (null != children && children.size() > 0) {
                boolean needReSchedule = false;

                for (String taskName : children) {
                    if (isOwner(taskName, currentUuid)) {
                        String taskPath = zkPath + "/" + taskName;
                        byte[] serverData = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_SERVER, null, null);
                        if (null != serverData && currentUuid.equals(new String(serverData))) {
                            
                            byte[] statusData = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_STATUS, null, null);
                            // 需要重新调度任务
                            if (!JobEnum.ScheduleStatus.TASK_STATUS_NOMAL.getCode().equals(new String(statusData))) {
                                needReSchedule = true;
                            }
                            ownerTask.add(taskName);
                        }
                    }
                }
                if (needReSchedule) {
                    DynamicTaskManager.clearJob();// 清除之前的调度任务
                    LOG.info(currentUuid + "需要reschedule task:{}", ownerTask);
                    for (String taskName : ownerTask) {
                        this.scheduleTask(zkPath + "/" + taskName);
                    }
                } else {
                    // 扫描指令，根据指令完成相应操作
                    instructTask(ownerTask);
                }
            }
        }
        return false;
    }
    
    /**
     * 功能描述: <br>
     * 将指令设为正常。如果指定了下次执行时间，则重新设置
     *
     * @param taskPath
     * @param nextVal
     * @throws Exception
     */
    private void setInstructNormal(String taskPath,String nextVal) throws Exception{
        List<ZkInfo> zkInfoList=new ArrayList<ZkInfo>();
        zkInfoList.add(new ZkInfo(taskPath + "/" + SUB_NODE_INSTRUCT, JobEnum.JobInstruct.ZK_NORMAL.getCode()));
        if(!StringUtils.isEmpty(nextVal)){
            zkInfoList.add(new ZkInfo(taskPath + "/" + SUB_NODE_NEXT, nextVal));
        }
        ZKTools.multiSetData(this.getZooKeeper(), zkInfoList);
    }
    
    /**
     * 功能描述: <br>
     * 将指令设为正常
     *
     * @param taskPath
     * @throws Exception
     */
    private void setInstructNormal(String taskPath) throws Exception{
        setInstructNormal(taskPath,null);
    }
    
    /**
     * 功能描述: <br>
     * 调度任务
     *
     * @param taskPath
     * @throws Exception
     */
    private void scheduleTask(String taskPath) throws Exception{
        byte[] data = this.getZooKeeper().getData(taskPath, null, null);
        TaskDefine taskDefine = null;
        try {
            taskDefine = JSON.parseObject(new String(data, CHARSET), TaskDefine.class);
        } catch (Exception e) {
            ZKTools.deleteTree(this.getZooKeeper(), taskPath);
        }

        if(JobEnum.JobStatus.STATUS_RUNNING.getCode().equals(taskDefine.getJobStatus())){
            byte[] nextVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_NEXT, null, null);
            long nextRunTime = Long.parseLong(new String(nextVal));
            
            if(nextRunTime>0 && nextRunTime<System.currentTimeMillis()){
                LOG.warn("任务：{}未正常执行，立即执行！",taskDefine.stringKey());
                TaskUtils.invokMethod(taskDefine);
            }
            
            ResponseVo<?> responseVo = DynamicTaskManager.scheduleTask(taskDefine);
            if (!responseVo.isSuccess()) {
                LOG.warn("添加本地任务失败：{}", responseVo.getMsg());
                LOG.info("添加本地任务失败,删除该任务节点！{}", taskPath);
                this.delTask(taskDefine);
            }else{
                //设置下次执行时间
                this.getZooKeeper().setData(taskPath + "/" +SUB_NODE_NEXT, String.valueOf(getNextRunTimes(taskDefine.getCronExpression())).getBytes(), -1);
            }
        }
        
    }
    
    private void instructTask(List<String> ownerTask) throws Exception{
        for (String taskName : ownerTask) {
            String taskPath = this.pathTask + "/" + taskName;
            byte[] instructVal = this.getZooKeeper().getData(taskPath + "/" + SUB_NODE_INSTRUCT, null, null);
            String instruct = new String(instructVal, CHARSET);
            
            //如果是正常状态，则不往下执行了，继续循环下一个任务
            if (JobEnum.JobInstruct.ZK_NORMAL.getCode().equals(instruct)) {
                continue;
            }
            
            byte[] data = this.getZooKeeper().getData(taskPath, null, null);
            String json = new String(data, CHARSET);
            TaskDefine taskDefine = JSON.parseObject(json, TaskDefine.class);
            
            if (JobEnum.JobInstruct.ZK_START.getCode().equals(instruct)) {
                LOG.info("收到调度指令，调度任务！{}", taskPath);
                this.scheduleTask(taskPath);
                this.setInstructNormal(taskPath);
                continue;
            }

            if (JobEnum.JobInstruct.ZK_DEL.getCode().equals(instruct)) {
                LOG.info("任务停止,删除该任务节点！{}", taskPath);
                this.delTask(taskDefine);
                DynamicTaskManager.deleteJob(taskDefine);
                continue;
            }

            if (JobEnum.JobInstruct.ZK_UPDATE.getCode().equals(instruct)) {
                LOG.info("收到更新cron表达式指令！{}", taskPath);
                DynamicTaskManager.updateJobCron(taskDefine);
                this.setInstructNormal(taskPath,String.valueOf(getNextRunTimes(taskDefine.getCronExpression())));
                continue;
            }

            if (JobEnum.JobInstruct.ZK_IMMED.getCode().equals(instruct)) {
                LOG.info("收到立即执行指令！{}", taskPath);
                DynamicTaskManager.runAJobNow(taskDefine);
                this.setInstructNormal(taskPath);
                continue;
            }

        }
        
    }
    
}