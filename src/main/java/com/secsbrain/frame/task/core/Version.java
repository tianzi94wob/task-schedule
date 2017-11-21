package com.secsbrain.frame.task.core;

/**
 * 
 * @author zhaoyi
 * 
 */
public class Version {

    private final static String version = "task-schedule-1.0.0";

    public static String getVersion() {
        return version;
    }

    public static boolean isCompatible(String dataVersion) {
        return version.compareTo(dataVersion) >= 0;
    }

}
