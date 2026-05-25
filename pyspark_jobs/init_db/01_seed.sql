CREATE DATABASE IF NOT EXISTS analytics_dev;
USE analytics_dev;

CREATE TABLE IF NOT EXISTS context_entries (
  context_name VARCHAR(100),
  contect_value VARCHAR(100),
  project VARCHAR(100),
  job VARCHAR(100)
);

INSERT INTO context_entries (context_name, contect_value, project, job) VALUES
('target_table_name', 'customer', 'TALEND_AWS_DI_USECASE', 'j_usecase_customer_setup'),
('cust_input_file', '/data/ps_cust.csv', 'TALEND_AWS_DI_USECASE', 'j_usecase_customer_setup');
