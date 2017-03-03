package cn.com.citycloud.frame.task;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import cn.com.citycloud.frame.task.common.ResponseVo;
import cn.com.citycloud.frame.task.JobEnum.ScheduleJobStatus;
import cn.com.citycloud.frame.task.core.RunningInfo;
import cn.com.citycloud.frame.task.core.ScheduleServer;
import cn.com.citycloud.frame.task.core.TaskDefine;
import cn.com.citycloud.frame.task.util.UUIDUtils;
import cn.com.citycloud.frame.task.util.ZKTools;

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
     * 获取所有task的关键字名
     * 
     * @return
     * @throws Exception
     */
    public List<String> getAllTaskKey() throws Exception {
        String zkPath = this.pathTask;
        // get all task
        List<String> children = this.getZooKeeper().getChildren(zkPath, false);
        return children;
    }

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
        // 先把task下的所有server全部清空
        String zkPath = this.pathTask;
        for (String taskPath : taskPathList) {
            List<String> taskServerIds = this.getZooKeeper().getChildren(zkPath + "/" + taskPath, false);
            for (String taskServer : taskServerIds) {
                ZKTools.deleteTree(this.getZooKeeper(), zkPath + "/" + taskPath + "/" + taskServer);
            }
        }

        RunningInfo runningInfo = new RunningInfo();
        runningInfo.setStatus(RunningInfo.WAIT);// 需要重新分配本地任务

        int taskCount = 0, serverCount = 0;
        if (taskPathList.size() <= taskServerList.size()) {
            while (taskCount < taskPathList.size()) {
                String realPath = this.getZooKeeper().create(
                        zkPath + "/" + taskPathList.get(taskCount) + "/" + taskServerList.get(serverCount), null,
                        this.zkManager.getAcl(), CreateMode.PERSISTENT);
                this.getZooKeeper().setData(realPath, JSON.toJSONString(runningInfo).getBytes(CHARSET), -1);

                LOG.debug("Assign server [" + taskServerList.get(serverCount) + "]" + " to task ["
                        + taskPathList.get(taskCount) + "]");
                taskCount++;
                serverCount++;
            }
        } else {
            while (taskCount < taskPathList.size()) {
                String realPath = this.getZooKeeper().create(
                        zkPath + "/" + taskPathList.get(taskCount) + "/" + taskServerList.get(serverCount), null,
                        this.zkManager.getAcl(), CreateMode.PERSISTENT);
                this.getZooKeeper().setData(realPath, JSON.toJSONString(runningInfo).getBytes(CHARSET), -1);
                LOG.debug("Assign server [" + taskServerList.get(serverCount) + "]" + " to task ["
                        + taskPathList.get(taskCount) + "]");
                taskCount++;
                serverCount++;
                if (serverCount == taskServerList.size()) {
                    serverCount = 0;
                }
            }
        }
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
            if (this.getZooKeeper().exists(taskPath, false) == null) {
                this.getZooKeeper().create(taskPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
            }
            // task节点下的server节点
            List<String> taskServerIds = this.getZooKeeper().getChildren(taskPath, false);
            if (null == taskServerIds || taskServerIds.size() == 0) {
                // task下没有server，自然需要重新分配任务
                LOG.info("task下没有server，重新分配server......task,{}", taskPath);
                return true;
            }

            String serverId = taskServerIds.get(0);
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
                    LOG.info("task对应的server没有平均分配，重新分配server......{}", taskServer);
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    public static void main(String[] args) {
        System.out.println(3 % 4);
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
                        List<String> taskServerIds = this.getZooKeeper().getChildren(taskPath, false);
                        String serverId = taskServerIds.get(0);// 每个task下只允许有一个server，直接这样取就好
                        if (!taskServerList.contains(serverId)) {
                            LOG.info("task下的server失效了，重新分配server......{}", currentUuid);
                            assignServerAvg(taskServerList, taskPathList);
                        } else {
                            byte[] data = this.getZooKeeper().getData(taskPath + "/" + serverId, null, null);
                            RunningInfo runningInfo = JSON.parseObject(new String(data, CHARSET), RunningInfo.class);
                            if (!RunningInfo.NOMAL.equals(runningInfo.getStatus())) {
                                runningInfo.setStatus(RunningInfo.NOMAL);// 不需要重置本地任务
                                this.getZooKeeper().setData(taskPath + "/" + serverId,
                                        JSON.toJSONString(runningInfo).getBytes(CHARSET), -1);
                            }
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
    public boolean isOwner(String name, String uuid) throws Exception {
        boolean isOwner = false;
        // 查看集群中是否注册当前任务，如果没有就自动注册
        String zkPath = this.pathTask + "/" + name;
        // 判断是否分配给当前节点
        zkPath = zkPath + "/" + uuid;
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            isOwner = true;
        }
        return isOwner;
    }

    /**
     * 功能描述: <br>
     * 保存运行信息
     * 
     * @param name
     * @return
     * @throws Exception
     */
    public boolean saveRunningInfo(String name, String uuid) throws Exception {
        String zkPath = this.pathTask + "/" + name;
        // 判断是否分配给当前节点
        zkPath = zkPath + "/" + uuid;
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            try {
                byte[] dataVal = this.getZooKeeper().getData(zkPath, null, null);
                String jsonVal = new String(dataVal, CHARSET);
                RunningInfo runningInfo = JSON.parseObject(jsonVal, RunningInfo.class);
                runningInfo.setTimes(runningInfo.getTimes() + 1);
                runningInfo.setLastRuningTime(System.currentTimeMillis());
                this.getZooKeeper().setData(zkPath, JSON.toJSONString(runningInfo).getBytes(CHARSET), -1);
            } catch (Exception e) {
            }
        }
        return true;
    }

    public boolean isExistsTask(TaskDefine taskDefine) throws Exception {
        String zkPath = this.pathTask + "/" + taskDefine.stringKey();
        return this.getZooKeeper().exists(zkPath, false) != null;
    }

    /**
     * 功能描述: <br>
     * 设置任务
     * 
     * @param taskDefine
     * @throws Exception
     */
    public void settingTask(TaskDefine taskDefine) throws Exception {
        String zkPath = this.zkManager.getRootPath() + "/" + NODE_TASK + "/" + taskDefine.getPrjName() + "/"
                + taskDefine.stringKey();
        if (this.getZooKeeper().exists(zkPath, false) == null) {
            this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
        }
        String json = JSON.toJSONString(taskDefine);
        this.getZooKeeper().setData(zkPath, json.getBytes(CHARSET), -1);
    }

    public void delTask(String targetBean, String targetMethod) throws Exception {
        String zkPath = this.pathTask;
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            zkPath = zkPath + "/" + targetBean + "#" + targetMethod;
            if (this.getZooKeeper().exists(zkPath, false) != null) {
                ZKTools.deleteTree(this.getZooKeeper(), zkPath);
            }
        }
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

    public List<TaskDefine> selectTask() throws Exception {
        String zkPath = this.pathTask;
        List<TaskDefine> taskDefines = new ArrayList<TaskDefine>();
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            List<String> childes = this.getZooKeeper().getChildren(zkPath, false);
            for (String child : childes) {
                byte[] data = this.getZooKeeper().getData(zkPath + "/" + child, null, null);
                TaskDefine taskDefine = null;
                if (null != data) {
                    String json = new String(data, CHARSET);
                    taskDefine = JSON.parseObject(json, TaskDefine.class);
                    taskDefine.setType("quartz task");
                } else {
                    String[] names = child.split("#");
                    if (StringUtils.isNotEmpty(names[0])) {
                        taskDefine = new TaskDefine();
                        taskDefine.setTargetBean(names[0]);
                        taskDefine.setTargetMethod(names[1]);
                        taskDefine.setType("quartz task");
                    }
                }

                List<String> sers = this.getZooKeeper().getChildren(zkPath + "/" + child, false);
                if (taskDefine != null && sers != null && sers.size() > 0) {
                    taskDefine.setCurrentServer(sers.get(0));
                    byte[] dataVal = this.getZooKeeper().getData(zkPath + "/" + child + "/" + sers.get(0), null, null);
                    if (dataVal != null) {
                        String val = new String(dataVal, CHARSET);
                        RunningInfo runningInfo=JSON.parseObject(val, RunningInfo.class);
                        taskDefine.setRunTimes(runningInfo.getTimes());
                        taskDefine.setLastRunningTime(runningInfo.getLastRuningTime());
                    }
                }
                taskDefines.add(taskDefine);
            }
        }
        return taskDefines;
    }

    public TaskDefine selectTaskByName(TaskDefine param) throws Exception {
        String zkPath = this.zkManager.getRootPath() + "/" + NODE_TASK + "/" + param.getPrjName() + "/"
                + param.stringKey();
        if (this.getZooKeeper().exists(zkPath, false) != null) {
            byte[] data = this.getZooKeeper().getData(zkPath, null, null);
            TaskDefine taskDefine = null;
            if (null != data) {
                String json = new String(data, CHARSET);
                taskDefine = JSON.parseObject(json, TaskDefine.class);
                taskDefine.setType("uncode task");

                List<String> sers = this.getZooKeeper().getChildren(zkPath, false);
                if(CollectionUtils.isEmpty(sers)){
                    return null;
                }
                taskDefine.setCurrentServer(sers.get(0));
                byte[] dataVal = this.getZooKeeper().getData(zkPath + "/" + sers.get(0), null, null);
                if (dataVal != null) {
                    String jsonStr = new String(dataVal, CHARSET);
                    RunningInfo runningInfo = JSON.parseObject(jsonStr, RunningInfo.class);
                    taskDefine.setRunTimes(runningInfo.getTimes());
                    taskDefine.setLastRunningTime(runningInfo.getLastRuningTime());
                }
            }
            return taskDefine;
        }
        return null;
    }

    /**
     * 功能描述: <br>
     * 将zk上的任务实际运行
     * 
     * @param currentUuid
     * @return
     * @throws Exception
     */
    public boolean checkLocalTask(String currentUuid) throws Exception {
        LOG.debug(currentUuid + ":检查本地任务");
        if (this.zkManager.checkZookeeperState()) {
            String zkPath = this.pathTask;
            List<String> children = this.getZooKeeper().getChildren(zkPath, false);
            List<String> ownerTask = new ArrayList<String>();
            if (null != children && children.size() > 0) {
                boolean needReSchedule = false;

                for (String taskName : children) {
                    if (isOwner(taskName, currentUuid)) {
                        String taskPath = zkPath + "/" + taskName;
                        byte[] data = this.getZooKeeper().getData(taskPath + "/" + currentUuid, null, null);
                        if (null != data) {
                            String jsonData = new String(data, CHARSET);
                            RunningInfo runningInfo = JSON.parseObject(jsonData, RunningInfo.class);
                            // 需要重新调度任务
                            if (!RunningInfo.NOMAL.equals(runningInfo.getStatus())) {
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
                        String taskPath = zkPath + "/" + taskName;
                        byte[] data = this.getZooKeeper().getData(taskPath, null, null);
                        if (null != data) {
                            String json = new String(data, CHARSET);
                            TaskDefine taskDefine = null;
                            try {
                                taskDefine = JSON.parseObject(json, TaskDefine.class);
                            } catch (Exception e) {
                                ZKTools.deleteTree(this.getZooKeeper(), taskPath);
                            }

                            ResponseVo<?> responseVo = DynamicTaskManager.scheduleTask(taskDefine);
                            if (!responseVo.isSuccess()) {
                                LOG.error("添加本地任务失败：{}", responseVo.getMsg());
                                LOG.warn("添加本地任务失败,删除该任务节点！{}", taskPath);
                                this.delTask(taskDefine);
                            }

                        }
                    }
                } else {
                    // 根据指令完成相应操作
                    for (String taskName : ownerTask) {
                        String taskPath = zkPath + "/" + taskName;
                        byte[] data = this.getZooKeeper().getData(taskPath, null, null);
                        if (null != data) {
                            String json = new String(data, CHARSET);
                            TaskDefine taskDefine = JSON.parseObject(json, TaskDefine.class);

                            if (JobEnum.ScheduleJobStatus.ZK_START.getCode().equals(taskDefine.getJobStatus())) {
                                LOG.info("任务开始，重新调度！{}", taskPath);
                                ResponseVo<?> responseVo = DynamicTaskManager.scheduleTask(taskDefine);
                                LOG.info("重新调度结果，{}，{}", responseVo.isSuccess(), responseVo.getMsg());
                                taskDefine.setJobStatus(ScheduleJobStatus.STATUS_RUNNING.getCode());
                                settingTask(taskDefine);
                                continue;
                            }

                            if (JobEnum.ScheduleJobStatus.ZK_DEL.getCode().equals(taskDefine.getJobStatus())
                                    || JobEnum.ScheduleJobStatus.STATUS_NOT_RUNNING.getCode().equals(
                                            taskDefine.getJobStatus())) {
                                LOG.info("任务停止,删除该任务节点！{}", taskPath);
                                this.delTask(taskDefine);
                                DynamicTaskManager.deleteJob(taskDefine);
                                continue;
                            }

                            if (JobEnum.ScheduleJobStatus.ZK_UPDATE.getCode().equals(taskDefine.getJobStatus())) {
                                LOG.info("收到更新cron表达式指令！{}", taskPath);
                                DynamicTaskManager.updateJobCron(taskDefine);
                                taskDefine.setJobStatus(ScheduleJobStatus.STATUS_RUNNING.getCode());
                                settingTask(taskDefine);
                                continue;
                            }

                            if (JobEnum.ScheduleJobStatus.ZK_IMMED.getCode().equals(taskDefine.getJobStatus())) {
                                LOG.info("收到立即执行指令！{}", taskPath);
                                DynamicTaskManager.runAJobNow(taskDefine);
                                taskDefine.setJobStatus(ScheduleJobStatus.STATUS_RUNNING.getCode());
                                settingTask(taskDefine);
                                continue;
                            }

                        }
                    }
                }
            }
        }
        return false;
    }

}