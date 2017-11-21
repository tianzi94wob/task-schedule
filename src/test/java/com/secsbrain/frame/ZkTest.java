/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: ZkTest.java
 * Author:   zhaoyi
 * Date:     2017-3-9 下午4:57:42
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.secsbrain.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉
 *
 * @author zhaoyi
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class ZkTest {
    
    public static void testSetData() throws Exception{
        final ZooKeeper zk = new ZooKeeper("10.10.77.105:2181",60000, new Watcher() {
            public void process(WatchedEvent event) {
                sessionEvent(new CountDownLatch(1), event);
            }
        });
        
        List<String> list = new ArrayList<String>();
        for(int i=0;i<10;i++){
            list.add(""+i);
        }
        
        ExecutorService executorService = Executors.newCachedThreadPool(); 
        for(final String str:list){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        
                        zk.setData("/test", str.getBytes(), -1);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
            });
        }
       
    }
    
    private static void sessionEvent(CountDownLatch connectionLatch, WatchedEvent event) {
        if (event.getState() == KeeperState.SyncConnected) {
            System.out.println("收到ZK连接成功事件！");
            connectionLatch.countDown();
        } else if (event.getState() == KeeperState.Expired) {
            System.out.println("会话超时，等待重新建立ZK连接...");
            try {
                //reConnection();
            } catch (Exception e) {
            }
        } // Disconnected：Zookeeper会自动处理Disconnected状态重连
    }

    public static void main(String[] args) {
        try {
            testSetData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
