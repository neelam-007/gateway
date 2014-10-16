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
);

CREATE INDEX audit_main_time on audit_main(time);
CREATE INDEX audit_main_level on audit_main(audit_level);
CREATE INDEX audit_main_node on audit_main(nodeid);
CREATE INDEX audit_main_type on audit_main(type);
CREATE INDEX audit_main_name on audit_main(name);
CREATE INDEX audit_main_username on audit_main(user_name);
CREATE INDEX audit_main_userid on audit_main(user_id);
CREATE INDEX audit_main_message on audit_main(message);
CREATE INDEX audit_main_e_class on audit_main(entity_class);
CREATE INDEX audit_main_e_id on audit_main(entity_id);
CREATE INDEX audit_main_reqid on audit_main(request_id);
CREATE INDEX audit_main_ip on audit_main(ip_address);

CREATE TABLE audit_detail  (
    audit_oid VARCHAR(40) NOT NULL,
    time BIGINT NOT NULL,
    component_id INTEGER,
    ordinal INTEGER,
    message_id INTEGER NOT NULL,
    exception_message LONG VARCHAR,
    properties LONG VARCHAR,
    FOREIGN KEY (audit_oid) REFERENCES audit_main (id) ON DELETE CASCADE
);

CREATE INDEX audit_detail_oid on audit_detail(audit_oid);
CREATE INDEX audit_detail_mid on audit_detail(message_id);
CREATE INDEX audit_detail_time on audit_detail(time);