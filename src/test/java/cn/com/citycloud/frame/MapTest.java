/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: MapTest.java
 * Author:   zhaoyi
 * Date:     2017-3-15 上午10:00:28
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package cn.com.citycloud.frame;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉
 *
 * @author zhaoyi
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class MapTest {

    public static void main(String[] args) {
        Map<String, Integer> map=new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        map.put("1b", 5);
        map.put("c", 2);
        map.put("ad", 10);
        System.out.println(map);
        
        
        Map<String, Integer> treeMap=new TreeMap<String, Integer>();
        treeMap.put("a", 1);
        treeMap.put("1b", 5);
        treeMap.put("c", 2);
        treeMap.put("ad", 10);
        System.out.println(treeMap);
        
    }
    
}
