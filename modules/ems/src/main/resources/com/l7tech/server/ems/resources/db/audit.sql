-----------------------------------------------------------------------------
-- Schema for audit tables
-----------------------------------------------------------------------------
--
-- This is created from a script since we need a delete cascade that is not
-- supported by hibernate:
--
-- http://opensource.atlassian.com/projects/hibernate/browse/ANN-747
--

create table audit_admin (
    action char(1),
    entity_class varchar(255),
    entity_id bigint,
    objectid bigint not null,
    primary key (objectid)
);

create table audit_detail (
    objectid bigint not null,
    audit_oid bigint not null,
    component_id integer,
    exception_message clob(2147483647),
    message_id integer not null,
    ordinal integer,
    time bigint not null,
    primary key (objectid)
);

create table audit_detail_params (
    audit_detail_oid bigint not null,
    value clob(2147483647),
    position integer not null,
    primary key (audit_detail_oid, position)
);

create table audit_main (
    objectid bigint not null,
    provider_oid bigint,
    ip_address varchar(32),
    message varchar(255) not null,
    time bigint not null,
    name varchar(255),
    nodeid varchar(32) not null,
    signature varchar(175),
    audit_level varchar(12) not null,
    user_id varchar(255),
    user_name varchar(255),
    primary key (objectid)
);

create table audit_message (
    authenticated smallint,
    authenticationType integer,
    mapping_values_oid bigint,
    operation_name varchar(255),
    request_length integer not null,
    request_zipxml blob(2147483647),
    response_length integer,
    response_status integer,
    response_zipxml blob(2147483647),
    routing_latency integer,
    service_oid bigint,
    status integer not null,
    request_id varchar(40) not null,
    objectid bigint not null,
    primary key (objectid)
);

create table audit_system (
    action varchar(32) not null,
    component_id integer not null,
    objectid bigint not null,
    primary key (objectid)
);

create table message_context_mapping_keys (
    objectid bigint not null,
    create_time bigint,
    guid varchar(255),
    mapping1_key varchar(128),
    mapping1_type varchar(36),
    mapping2_key varchar(128),
    mapping2_type varchar(36),
    mapping3_key varchar(128),
    mapping3_type varchar(36),
    mapping4_key varchar(128),
    mapping4_type varchar(36),
    mapping5_key varchar(128),
    mapping5_type varchar(36),
    version integer,
    primary key (objectid)
);

create table message_context_mapping_values (
    objectid bigint not null,
    create_time bigint,
    guid varchar(255),
    mapping1_value varchar(255),
    mapping2_value varchar(255),
    mapping3_value varchar(255),
    mapping4_value varchar(255),
    mapping5_value varchar(255),
    mapping_keys_oid bigint,
    primary key (objectid)
);

alter table audit_admin
    add constraint FK364471EB7AEF109A
    foreign key (objectid)
    references audit_main
    on delete cascade;

alter table audit_detail
    add constraint FK97797D35810D4766
    foreign key (audit_oid)
    references audit_main
    on delete cascade;

alter table audit_detail_params
    add constraint FK990923D0753897C0
    foreign key (audit_detail_oid)
    references audit_detail
    on delete cascade;

alter table audit_message
    add constraint FK33C837A384D0EAD
    foreign key (mapping_values_oid)
    references message_context_mapping_values;

alter table audit_message
    add constraint FK33C837A37AEF109A
    foreign key (objectid)
    references audit_main
    on delete cascade;

alter table audit_system
    add constraint FKB22BD7137AEF109A
    foreign key (objectid)
    references audit_main
    on delete cascade;

alter table message_context_mapping_values
    add constraint FKABF3A97B4B03F6D1
    foreign key (mapping_keys_oid)
    references message_context_mapping_keys;

