package com.secsbrain.frame.task;

/**
 * 对枚举数据的定义
 * 
 * @author zhaoyi
 * 
 */
public class JobEnum {
    
    /**
     * 任务状态
     * 
     * @author zhaoyi
     * 
     */
    public enum JobStatus {

        STATUS_NOT_RUNNING("0", "不在运行"), 
        STATUS_RUNNING("1", "运行"), 
        STATUS_DEL("2", "删除");

        private String code;
        private String desc;

        JobStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return this.code;
        }

        public String getDesc() {
            return this.desc;
        }
    }
    
    /**
     * 任务调度状态
     * 
     * @author zhaoyi
     * 
     */
    public enum ScheduleStatus {

        TASK_STATUS_NOMAL("0", "已调度"), 
        TASK_STATUS_WAIT("1", "等待调度");

        private String code;
        private String desc;

        ScheduleStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return this.code;
        }

        public String getDesc() {
            return this.desc;
        }
    }
    
    /**
     * 任务指令
     *
     * @author zhaoyi
     */
    public enum JobInstruct {
        //暂停，恢复根据需要添加
        ZK_NORMAL("normal", "常态标识"), 
        ZK_START("start", "开始运行标识"), 
        ZK_UPDATE("cron", "更改表达式标识"), 
        ZK_IMMED("immed", "立即运行标识"), 
        ZK_DEL("delete", "删除任务标识");

        private String code;
        private String desc;

        JobInstruct(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return this.code;
        }

        public String getDesc() {
            return this.desc;
        }
    }

    /**
     * 任务同步状态
     * 
     * @author zhaoyi
     * 
     */
    public enum ScheduleJobConStatus {

        CONCURRENT_NOT("0", "不同步"), CONCURRENT_IS("1", "同步");

        private String code;
        private String desc;

        ScheduleJobConStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return this.code;
        }

        public String getDesc() {
            return this.desc;
        }
    }

}
