package com.migration.etl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@ExtendWith(MockitoExtension.class)
class ContextServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ContextService contextService;

    @Test
    void getJobContext_usesCorrectSqlAndParameters() {
        String expectedSql = "SELECT context_name, contect_value "
                           + "FROM context_entries "
                           + "WHERE project = ? AND job = ?";

        contextService.getJobContext("TALEND_AWS_DI_USECASE", "j_usecase_customer_setup");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(),
                any(RowCallbackHandler.class));

        assertEquals(expectedSql, sqlCaptor.getValue());
        Object[] params = paramsCaptor.getValue();
        assertEquals(2, params.length);
        assertEquals("TALEND_AWS_DI_USECASE", params[0]);
        assertEquals("j_usecase_customer_setup", params[1]);
    }

    @Test
    void getJobContext_returnsEmptyMapWhenNoResults() {
        Map<String, String> result = contextService.getJobContext("UNKNOWN", "UNKNOWN");

        assertEquals(0, result.size());
    }
}
