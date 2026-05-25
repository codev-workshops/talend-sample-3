package com.migration.etl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.job")
public class JobConfig {

    private String project = "TALEND_AWS_DI_USECASE";

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
