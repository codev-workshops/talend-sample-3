CREATE TABLE IF NOT EXISTS context_entries (
    context_name VARCHAR(100),
    contect_value VARCHAR(100),
    project VARCHAR(100),
    job VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS t_log_catcher_stats (
    moment DATETIME,
    pid VARCHAR(20),
    root_pid VARCHAR(20),
    father_pid VARCHAR(20),
    project VARCHAR(50),
    job VARCHAR(255),
    context VARCHAR(50),
    priority INT,
    type VARCHAR(255),
    origin VARCHAR(255),
    message VARCHAR(255),
    code INT
);
