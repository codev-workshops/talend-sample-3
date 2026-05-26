package com.migration.etl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.migration.etl.config.ContextService;
import com.migration.etl.config.JobConfig;
import com.migration.etl.job.AnalyticsSummaryJob;
import com.migration.etl.job.CustomerSetupJob;
import com.migration.etl.job.FlavorSetupJob;
import com.migration.etl.service.JobLogService;
import com.migration.etl.service.StatusMailService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineRunnerTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private ContextService contextService;
    @Mock
    private CustomerSetupJob customerSetupJob;
    @Mock
    private FlavorSetupJob flavorSetupJob;
    @Mock
    private AnalyticsSummaryJob analyticsSummaryJob;
    @Mock
    private JobLogService jobLogService;
    @Mock
    private StatusMailService statusMailService;

    private JobConfig jobConfig;
    private PipelineRunner runner;

    @BeforeEach
    void setUp() {
        jobConfig = new JobConfig();
        runner = new PipelineRunner(dataSource, contextService, jobConfig,
                customerSetupJob, flavorSetupJob, analyticsSummaryJob,
                jobLogService, statusMailService);
    }

    @Test
    void testExecutionOrder() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        runner.run();

        InOrder inOrder = inOrder(customerSetupJob, flavorSetupJob, analyticsSummaryJob);
        inOrder.verify(customerSetupJob).run();
        inOrder.verify(flavorSetupJob).run();
        inOrder.verify(analyticsSummaryJob).run();
    }

    @Test
    void testMySqlConnectionFailure() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> runner.run());
        assertTrue(ex.getMessage().contains("Unable to connect to mySQL Server instance"));
    }

    @Test
    void testContextLoadFailure() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(contextService.getJobContext(anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> runner.run());
        assertTrue(ex.getMessage().contains("error loading context variables"));
    }
}
