/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: ReflectTest.java
 * Author:   zhaoyi
 * Date:     2017-3-8 下午3:39:44
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package cn.com.citycloud.frame;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.com.citycloud.frame.task.util.VerificationUtil;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉
 *
 * @author zhaoyi
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class ReflectTest {

    public void testInvoke(){
        try {
            System.out.println("休息10秒");
            Thread.sleep(10000);
            System.out.println("休息完了，开始干活吧");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("cn.com.citycloud.frame.ReflectTest");
        final Object object = clazz.newInstance();
        clazz = object.getClass();
        final Method method = VerificationUtil.getMethod(clazz, "testInvoke");
        
        long startTime=System.currentTimeMillis();
        
        ExecutorService executorService = Executors.newCachedThreadPool(); 
        executorService.execute(new Runnable() {
            
            @Override
            public void run() {
                try {
                    method.invoke(object);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
        
        long endTime=System.currentTimeMillis();
        
        System.out.println("反射方法耗时："+(endTime-startTime)+" ms");
    }
    
}
