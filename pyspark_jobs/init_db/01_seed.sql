CREATE DATABASE IF NOT EXISTS analytics_dev;
USE analytics_dev;

CREATE TABLE IF NOT EXISTS context_entries (
  context_name VARCHAR(100),
  contect_value VARCHAR(100),
  project VARCHAR(100),
  job VARCHAR(100)
);

-- Seed context rows for j_usecase_flavor_setup
DELETE FROM context_entries WHERE project = 'TALEND_AWS_DI_USECASE' AND job = 'j_usecase_flavor_setup';

INSERT INTO context_entries (context_name, contect_value, project, job) VALUES
('target_table_name', 'flavors', 'TALEND_AWS_DI_USECASE', 'j_usecase_flavor_setup'),
('flavor_input_file', '/opt/spark-data/ps_flavor.csv', 'TALEND_AWS_DI_USECASE', 'j_usecase_flavor_setup');
