package cn.com.citycloud.frame.task;

/**
 * 对枚举数据的定义
 * 
 * @author zhaoyi
 * 
 */
public class JobEnum {
    
    /**
     * 任务执行状态
     * 
     * @author zhaoyi
     * 
     */
    public enum ScheduleJobStatus {

        STATUS_NOT_RUNNING("0", "不在运行"), 
        STATUS_RUNNING("1", "运行"), 
        ZK_START("start", "开始运行标识"), 
        ZK_UPDATE("cron", "更改表达式标识"), 
        ZK_IMMED("immed", "立即运行标识"), 
        ZK_DEL("2", "删除");

        private String code;
        private String desc;

        ScheduleJobStatus(String code, String desc) {
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
