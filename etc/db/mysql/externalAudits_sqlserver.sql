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
    provider_oid varchar(40) NOT NULL DEFAULT '-1',
    signature varchar(1024),
    properties varchar(max),
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
    authenticated bit default '0',
    authenticationType varchar(40),
    request_length int,
    response_length int,
    request_xml varbinary(max),
    response_xml varbinary(max),
    response_status int,
    routing_latency int,
    --
    -- system specific
    --
    component_id int,
    action varchar(32)
    --
    PRIMARY KEY (id)
) ;

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
    audit_oid varchar(40) NOT NULL,
    time bigint NOT NULL,
    component_id integer,
    ordinal integer,
    message_id integer NOT NULL,
    exception_message varchar(max),
    properties varchar(max),
    PRIMARY KEY (audit_oid),
    FOREIGN KEY (audit_oid) REFERENCES audit_main (id) ON DELETE CASCADE
);

CREATE INDEX audit_detail_mid on audit_detail(message_id);
CREATE INDEX audit_detail_time on audit_detail(time);