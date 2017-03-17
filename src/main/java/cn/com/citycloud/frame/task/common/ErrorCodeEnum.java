package cn.com.citycloud.frame.task.common;

import java.util.HashMap;
import java.util.Map;

/**
 * ERROR_系统代号_错误类型 <br />
 */
public enum ErrorCodeEnum {
    
    ERROR_COMMON_PARAM(1001,"参数不符合规则"),
    ERROR_COMMON_SYSTEM(1002,"系统错误"),
    ERROR_COMMON_HANDING(1003,"系统处理中"),
    ERROR_COMMON_SAVE(1004,"保存数据失败"),
    ERROR_COMMON_UNIQUE(1005,"违反了唯一约束"),
    ERROR_COMMON_CHECK(1006,"条件验证异常"),
    ERROR_COMMON_REPEAT_HANDLER(1007,"重复处理"),
    ERROR_COMMON_NETWORK_ERROR(1008,"网络超时");

    private int code;
    
    private String returnMsg;
    
    ErrorCodeEnum(int code,String returnMsg){
        this.code = code;
        this.returnMsg = returnMsg;
    }
    public int getCode(){
        return this.code;
    }
    
    @SuppressWarnings("rawtypes")
    public ResponseVo getResponseVo(){
      return  new ResponseVo(false, this.code, this.returnMsg);
    }
    
    @SuppressWarnings("rawtypes")
    public ResponseVo getResponseVo(String msg){
        return  new ResponseVo(false, this.code, msg);
     }
    
    private static Map<Integer, ErrorCodeEnum> params ;
    private static Object lock = new Object();
    
    public static String getNameByCode(int code) {
        synchronized (lock) {
            if (params == null) {
                params = new HashMap<Integer, ErrorCodeEnum>();
                for (ErrorCodeEnum e : ErrorCodeEnum.values()) {
                    params.put(e.code, e);
                }
            }
        }
        return params.get(code).returnMsg;
    }
    
    public static ErrorCodeEnum getEnumByCode(int code) {
        synchronized (lock) {
            if (params == null) {
                params = new HashMap<Integer, ErrorCodeEnum>();
                for (ErrorCodeEnum e : ErrorCodeEnum.values()) {
                    params.put(e.code, e);
                }
            }
        }
        return params.get(code);
    }
}
