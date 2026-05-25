package com.migration.etl.model;

import java.time.LocalDateTime;

public class LogEntry {

    private LocalDateTime moment;
    private String pid;
    private String rootPid;
    private String fatherPid;
    private String project;
    private String job;
    private String context;
    private Integer priority;
    private String type;
    private String origin;
    private String message;
    private Integer code;

    public LogEntry() {
    }

    public LogEntry(LocalDateTime moment, String pid, String rootPid, String fatherPid,
                    String project, String job, String context, Integer priority,
                    String type, String origin, String message, Integer code) {
        this.moment = moment;
        this.pid = pid;
        this.rootPid = rootPid;
        this.fatherPid = fatherPid;
        this.project = project;
        this.job = job;
        this.context = context;
        this.priority = priority;
        this.type = type;
        this.origin = origin;
        this.message = message;
        this.code = code;
    }

    public LocalDateTime getMoment() {
        return moment;
    }

    public void setMoment(LocalDateTime moment) {
        this.moment = moment;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getRootPid() {
        return rootPid;
    }

    public void setRootPid(String rootPid) {
        this.rootPid = rootPid;
    }

    public String getFatherPid() {
        return fatherPid;
    }

    public void setFatherPid(String fatherPid) {
        this.fatherPid = fatherPid;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
