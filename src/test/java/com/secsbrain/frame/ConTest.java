/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: ConTest.java
 * Author:   zhaoyi
 * Date:     2017-3-8 下午5:56:24
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.secsbrain.frame;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉
 *
 * @author zhaoyi
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class ConTest {

    public static void main(String[] args) throws Exception {
        CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
        cronTriggerImpl.setCronExpression("00 41 16 14 03 ? 2017");//这里写要准备猜测的cron表达式
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.YEAR, 2);//把统计的区间段设置为从现在到2年后的今天（主要是为了方法通用考虑，如那些1个月跑一次的任务，如果时间段设置的较短就不足20条)
        List<Date> dates = TriggerUtils.computeFireTimesBetween(cronTriggerImpl, null, now, calendar.getTime());//这个是重点，一行代码搞定~~
        System.out.println(dates.size());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for(int i =0;i < dates.size();i ++){
            if(i >19){//这个是提示的日期个数
               break;
            }
            System.out.println("aaaa:"+dateFormat.format(dates.get(i)));
        }
        
        
        List<Date> list=TriggerUtils.computeFireTimes(cronTriggerImpl, null, 1);
        
        for(int i =0;i < list.size();i ++){
            if(i >19){//这个是提示的日期个数
               break;
            }
            System.out.println("bbbb:"+dateFormat.format(list.get(i)));
        }
     }
    
}
