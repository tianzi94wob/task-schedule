/*
 * Copyright (C), 2015-2016, 城云科技
 * FileName: NumberUtil.java
 * Author:   Administrator
 * Date:     2016-7-20 下午1:25:54
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.secsbrain.frame.task.util;

import java.net.InetAddress;

/**
 * IP工具类
 *
 */
public class IpUtils {

    public static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }
    
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "";
        }
    }
    
}
