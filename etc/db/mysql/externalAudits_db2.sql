CREATE TABLE audit_main (
    id VARCHAR(40) NOT NULL,
    nodeid VARCHAR(40) NOT NULL,
    time BIGINT NOT NULL,
    type VARCHAR(50),
    audit_level VARCHAR(12) NOT NULL,
    name VARCHAR(255),
    message VARCHAR(255) NOT NULL,
    ip_address VARCHAR(39),
    user_name VARCHAR(255),
    user_id VARCHAR(255),
    provider_oid VARCHAR(40) NOT NULL DEFAULT -1,
    signature VARCHAR(1024),
    properties LONG VARCHAR,
    entity_class VARCHAR(255),
    entity_id VARCHAR(40),
    status INTEGER,
    request_id VARCHAR(40),
    service_oid VARCHAR(40),
    operation_name VARCHAR(255),
    authenticated SMALLINT default '0',
    authenticationType VARCHAR(40),
    request_length INTEGER,
    response_length INTEGER,
    request_xml BLOB,
    response_xml BLOB,
    response_status INTEGER,
    routing_latency INTEGER,
    component_id INTEGER,
    action VARCHAR(32),
    PRIMARY KEY (id)
) 

CREATE TABLE audit_detail  (
    audit_oid VARCHAR(40) NOT NULL,
    time BIGINT NOT NULL,
    component_id INTEGER,
    ordinal INTEGER,
    message_id INTEGER NOT NULL,
    exception_message LONG VARCHAR,
    properties LONG VARCHAR,
    FOREIGN KEY (audit_oid) REFERENCES audit_main (id) ON DELETE CASCADE
)
