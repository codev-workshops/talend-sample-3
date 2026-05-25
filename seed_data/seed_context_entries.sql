CREATE TABLE IF NOT EXISTS analytics_dev.context_entries (
  context_name VARCHAR(100),
  contect_value VARCHAR(100),
  project VARCHAR(100),
  job VARCHAR(100)
);

-- Clear existing seed rows
DELETE FROM analytics_dev.context_entries WHERE project = 'TALEND_AWS_DI_USECASE';

-- j_usecase_customer_setup context
INSERT INTO analytics_dev.context_entries (context_name, contect_value, project, job) VALUES
('target_table_name', 'customer', 'TALEND_AWS_DI_USECASE', 'j_usecase_customer_setup'),
('cust_input_file', '/root/use_case/ps_cust.csv', 'TALEND_AWS_DI_USECASE', 'j_usecase_customer_setup');

-- j_usecase_flavor_setup context
INSERT INTO analytics_dev.context_entries (context_name, contect_value, project, job) VALUES
('target_table_name', 'flavors', 'TALEND_AWS_DI_USECASE', 'j_usecase_flavor_setup'),
('flavor_input_file', '/root/use_case/ps_flavor.csv', 'TALEND_AWS_DI_USECASE', 'j_usecase_flavor_setup');

-- j_usecase_analytics_summary context
INSERT INTO analytics_dev.context_entries (context_name, contect_value, project, job) VALUES
('flavor_input_table', 'flavors', 'TALEND_AWS_DI_USECASE', 'j_usecase_analytics_summary'),
('cust_input_table', 'customer', 'TALEND_AWS_DI_USECASE', 'j_usecase_analytics_summary'),
('summary_analytics_target_table', 'summary', 'TALEND_AWS_DI_USECASE', 'j_usecase_analytics_summary');
