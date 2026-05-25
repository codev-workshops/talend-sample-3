package com.migration.etl.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class AnalyticsSummaryJobTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AnalyticsSummaryJob job;

    @BeforeEach
    void setUp() {
        job = new AnalyticsSummaryJob(jdbcTemplate);
    }

    @Test
    void testDdlAndInsertExecutedInCorrectOrder() {
        job.run();

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).execute(eq("DROP TABLE IF EXISTS summary"));
        inOrder.verify(jdbcTemplate).execute(contains("CREATE TABLE summary"));
        inOrder.verify(jdbcTemplate).execute(contains("INSERT INTO summary"));
    }

    @Test
    void testExceptionPropagates() {
        doThrow(new RuntimeException("DB error"))
                .when(jdbcTemplate)
                .execute(eq("DROP TABLE IF EXISTS summary"));

        assertThrows(RuntimeException.class, () -> job.run());
    }

    @Test
    void testInsertSqlMatchesExpectedQuery() {
        job.run();

        String expectedSql =
                "INSERT INTO summary (count_flavor_id, flavor_name, state_cd) "
                + "SELECT COUNT(f.flavor_id), f.flavor_name, c.state_cd "
                + "FROM flavors f "
                + "INNER JOIN customer c ON f.flavor_id = c.flavor "
                + "GROUP BY c.state_cd, f.flavor_name "
                + "HAVING COUNT(f.flavor_id) > 1";

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).execute(eq("DROP TABLE IF EXISTS summary"));
        inOrder.verify(jdbcTemplate).execute(contains("CREATE TABLE summary"));
        inOrder.verify(jdbcTemplate).execute(eq(expectedSql));
    }
}
