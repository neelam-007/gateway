CREATE TABLE audit_main (
    id varchar(40) NOT NULL,
    nodeid varchar(40) NOT NULL,
    time bigint NOT NULL,
    type varchar(50),
    audit_level varchar(12) NOT NULL,
    name varchar(255),
    message varchar(255) NOT NULL,
    ip_address varchar(39),
    user_name varchar(255),
    user_id varchar(255),
    provider_oid varchar(40) NOT NULL DEFAULT -1,
    signature varchar(1024),
    properties MEDIUMTEXT,
    --
    -- admin specific
    entity_class varchar(255),
    entity_id varchar(40),
    --  action char(1), use from system
    --
    -- message specific
    status int,
    request_id varchar(40),
    service_oid varchar(40),
    operation_name varchar(255),
    authenticated tinyint(1) default '0',
    authenticationType varchar(40),
    request_length int,
    response_length int,
    request_xml MEDIUMBLOB,
    response_xml MEDIUMBLOB,
    response_status int,
    routing_latency int,
    --
    -- system specific
    --
    component_id int,
    action varchar(32),
    --
    PRIMARY KEY (id),
    INDEX audit_main_time (time),
    INDEX audit_main_level (audit_level),
    INDEX audit_main_node (nodeid),
    INDEX audit_main_type (type),
    INDEX audit_main_name (name),
    INDEX audit_main_username (user_name),
    INDEX audit_main_userid (user_id),
    INDEX audit_main_message (message),
    INDEX audit_main_e_class (entity_class),
    INDEX audit_main_e_id (entity_id),
    INDEX audit_main_reqid (request_id),
    INDEX audit_main_ip (ip_address)
);

CREATE TABLE audit_detail  (
    audit_oid varchar(40) NOT NULL,
    time bigint(20) NOT NULL,
    component_id integer,
    ordinal integer,
    message_id integer NOT NULL,
    exception_message MEDIUMTEXT,
    properties MEDIUMTEXT,
    FOREIGN KEY (audit_oid) REFERENCES audit_main (id) ON DELETE CASCADE,
    PRIMARY KEY (audit_oid),
    INDEX audit_detail_mid (message_id),
    INDEX audit_detail_time (time)
);