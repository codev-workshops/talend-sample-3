package com.migration.etl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.migration.etl.integration.TestMailConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class ContextServiceIntegrationTest {

    @Autowired
    private ContextService contextService;

    @Test
    void getJobContext_customerSetup_returnsExpectedEntries() {
        Map<String, String> context = contextService.getJobContext(
                "TALEND_AWS_DI_USECASE", "j_usecase_customer_setup");

        assertEquals(2, context.size());
        assertEquals("customer", context.get("target_table_name"));
        assertEquals("/root/use_case/ps_cust.csv", context.get("cust_input_file"));
    }

    @Test
    void getJobContext_flavorSetup_returnsExpectedEntries() {
        Map<String, String> context = contextService.getJobContext(
                "TALEND_AWS_DI_USECASE", "j_usecase_flavor_setup");

        assertEquals(2, context.size());
        assertEquals("flavors", context.get("target_table_name"));
        assertEquals("/root/use_case/ps_flavor.csv", context.get("flavor_input_file"));
    }
}
