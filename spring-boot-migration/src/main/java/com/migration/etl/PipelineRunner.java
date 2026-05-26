package com.migration.etl;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.migration.etl.config.ContextService;
import com.migration.etl.config.JobConfig;
import com.migration.etl.job.AnalyticsSummaryJob;
import com.migration.etl.job.CustomerSetupJob;
import com.migration.etl.job.FlavorSetupJob;
import com.migration.etl.service.JobLogService;
import com.migration.etl.service.StatusMailService;

@Component
@ConditionalOnProperty(name = "app.pipeline.enabled", havingValue = "true", matchIfMissing = true)
public class PipelineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final DataSource dataSource;
    private final ContextService contextService;
    private final JobConfig jobConfig;
    private final CustomerSetupJob customerSetupJob;
    private final FlavorSetupJob flavorSetupJob;
    private final AnalyticsSummaryJob analyticsSummaryJob;
    private final JobLogService jobLogService;
    private final StatusMailService statusMailService;

    public PipelineRunner(DataSource dataSource,
                          ContextService contextService,
                          JobConfig jobConfig,
                          CustomerSetupJob customerSetupJob,
                          FlavorSetupJob flavorSetupJob,
                          AnalyticsSummaryJob analyticsSummaryJob,
                          JobLogService jobLogService,
                          StatusMailService statusMailService) {
        this.dataSource = dataSource;
        this.contextService = contextService;
        this.jobConfig = jobConfig;
        this.customerSetupJob = customerSetupJob;
        this.flavorSetupJob = flavorSetupJob;
        this.analyticsSummaryJob = analyticsSummaryJob;
        this.jobLogService = jobLogService;
        this.statusMailService = statusMailService;
    }

    @Override
    public void run(String... args) throws Exception {
        verifyMySqlConnectivity();
        loadContext();

        log.info("Starting ETL pipeline execution");

        customerSetupJob.run();
        log.info("CustomerSetupJob completed");

        flavorSetupJob.run();
        log.info("FlavorSetupJob completed");

        analyticsSummaryJob.run();
        log.info("AnalyticsSummaryJob completed");

        jobLogService.log(jobConfig.getProject(), "PipelineRunner",
                "Pipeline completed successfully", 0, 6, "INFO");

        statusMailService.sendStatusMail("ETL pipeline completed successfully. "
                + "Jobs executed: CustomerSetupJob, FlavorSetupJob, AnalyticsSummaryJob.");

        log.info("ETL pipeline finished successfully");
    }

    private void verifyMySqlConnectivity() {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("SELECT 1");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to connect to mySQL Server instance. Code: 4", e);
        }
    }

    private void loadContext() {
        try {
            contextService.getJobContext(jobConfig.getProject(), "CustomerSetupJob");
            contextService.getJobContext(jobConfig.getProject(), "FlavorSetupJob");
            contextService.getJobContext(jobConfig.getProject(), "AnalyticsSummaryJob");
        } catch (Exception e) {
            throw new RuntimeException(
                    "error loading context variables. Code: 8", e);
        }
    }
}
