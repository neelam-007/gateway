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
    entity_id VARCHAR2(40),
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
    audit_oid VARCHAR2(40) NOT NULL,
    time  NUMBER(19) NOT NULL,
    component_id int,
    ordinal int,
    message_id int NOT NULL,
    exception_message CLOB,
    properties CLOB,
    FOREIGN KEY (audit_oid) REFERENCES audit_main (id) ON DELETE CASCADE
);

CREATE INDEX audit_detail_oid on audit_detail(audit_oid);
CREATE INDEX audit_detail_mid on audit_detail(message_id);
CREATE INDEX audit_detail_time on audit_detail(time);