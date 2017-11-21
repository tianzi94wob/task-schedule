/*
 * Copyright (C), 2015-2017, 城云科技
 * FileName: ZkInfo.java
 * Author:   zhaoyi
 * Date:     2017-3-13 下午2:49:45
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.secsbrain.frame.task.core;

import java.io.Serializable;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.ACL;

/**
 * Zookeeper相关信息
 *
 * @author zhaoyi
 */
public class ZkInfo implements Serializable{

    private static final long serialVersionUID = -5206551533443665916L;

    private String path;
    
    private String data;
    
    private int version=-1;
    
    private List<ACL> acls;
    
    private CreateMode createMode;

    /**
     * @param path
     * @param data
     */
    public ZkInfo(String path, String data) {
        super();
        this.path = path;
        this.data = data;
    }

    /**
     * @param path
     * @param data
     * @param version
     */
    public ZkInfo(String path, String data, int version) {
        super();
        this.path = path;
        this.data = data;
        this.version = version;
    }

    /**
     * @param path
     * @param data
     * @param acls
     * @param createMode
     */
    public ZkInfo(String path, String data, List<ACL> acls, CreateMode createMode) {
        super();
        this.path = path;
        this.data = data;
        this.acls = acls;
        this.createMode = createMode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<ACL> getAcls() {
        return acls;
    }

    public void setAcls(List<ACL> acls) {
        this.acls = acls;
    }

    public CreateMode getCreateMode() {
        return createMode;
    }

    public void setCreateMode(CreateMode createMode) {
        this.createMode = createMode;
    }
    
}
