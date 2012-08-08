CREATE TABLE audit_main (
    id VARCHAR2(40) NOT NULL PRIMARY KEY ,
    nodeid VARCHAR2(40) NOT NULL,
    time NUMBER(19) NOT NULL,
    type VARCHAR2(50),
    audit_level VARCHAR2(12) NOT NULL,
    name VARCHAR2(255),
    message VARCHAR2(255) NOT NULL,
    ip_address VARCHAR2(39),
    user_name VARCHAR2(255),
    user_id VARCHAR2(255),
    provider_oid VARCHAR2(40) DEFAULT '-1' NOT NULL ,
    signature VARCHAR2(1024),
    properties CLOB,
    entity_class VARCHAR2(255),
    entity_id VARCHAR2(20),
    status int,
    request_id VARCHAR2(40),
    service_oid VARCHAR2(40),
    operation_name VARCHAR2(255),
    authenticated CHAR(1) default '0',
    authenticationType VARCHAR2(40),
    request_length int,
    response_length int,
    request_xml BLOB ,
    response_xml BLOB ,
    response_status int,
    routing_latency int,
    component_id int,
    action VARCHAR2(32)
);

CREATE TABLE audit_detail  (
    audit_oid VARCHAR2(40) NOT NULL,
    time  NUMBER(19) NOT NULL,
    component_id int,
    ordinal int,
    message_id int NOT NULL,
    exception_message CLOB,
    properties CLOB,
    FOREIGN KEY (audit_oid) REFERENCES audit_main (id) ON DELETE CASCADE
);
