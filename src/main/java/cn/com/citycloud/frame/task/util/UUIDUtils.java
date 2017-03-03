/*
 * Copyright (C), 2015-2016, 城云科技
 * FileName: UUIDUtils.java
 * Author:   zhaoyi
 * Date:     2016-7-27 下午2:45:50
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package cn.com.citycloud.frame.task.util;

import java.util.UUID;

/**
 * UUID生成工具
 * 
 * @author zhaoyi
 */
public class UUIDUtils {
    
    public static String generateUUIDForStringValue() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    
    public static String getUUID(){
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }
    
}