/* Hibernate's SchemaExport generated file */
drop table ssg_logs cascade constraints
drop table jms_connection cascade constraints
drop table published_service cascade constraints
drop table client_cert cascade constraints
drop table service_usage cascade constraints
drop table cluster_info cascade constraints
drop table service_resolution cascade constraints
drop table identity_provider cascade constraints
drop table internal_group cascade constraints
drop table internal_user_group cascade constraints
drop table jms_endpoint cascade constraints
drop table internal_user cascade constraints
drop table hibernate_unique_key cascade constraints
create table ssg_logs (objectid NUMBER(19,0) not null, nodeid VARCHAR2(255), message VARCHAR2(255), strlvl VARCHAR2(255), loggername VARCHAR2(255), millis NUMBER(19,0), sourceclassname VARCHAR2(255), sourcemethodname VARCHAR2(255), strrequestid VARCHAR2(255), primary key (objectid))
create table jms_connection (objectid NUMBER(19,0) not null, version NUMBER(10,0) not null, name VARCHAR2(255), jndi_url VARCHAR2(255), factory_classname VARCHAR2(255), destination_factory_url VARCHAR2(255), queue_factory_url VARCHAR2(255), topic_factory_url VARCHAR2(255), username VARCHAR2(255), password VARCHAR2(255), primary key (objectid))
create table published_service (objectid NUMBER(19,0) not null, version NUMBER(10,0) not null, name VARCHAR2(255), policy_xml VARCHAR2(255), wsdl_url VARCHAR2(255), wsdl_xml VARCHAR2(255), disabled NUMBER(1,0), primary key (objectid))
create table client_cert (objectid NUMBER(19,0) not null, provider NUMBER(19,0), login VARCHAR2(255), cert VARCHAR2(255), reset_counter NUMBER(10,0), primary key (objectid))
create table service_usage (serviceid NUMBER(19,0) not null, nodeid VARCHAR2(255) not null, requestnr NUMBER(19,0), authorizedreqnr NUMBER(19,0), completedreqnr NUMBER(19,0), primary key (serviceid, nodeid))
create table cluster_info (mac VARCHAR2(255) not null, name VARCHAR2(255), address VARCHAR2(255), ismaster NUMBER(1,0), uptime NUMBER(19,0), avgload DOUBLE PRECISION, statustimestamp NUMBER(19,0), primary key (mac))
create table service_resolution (urn VARCHAR2(255) not null, soapaction VARCHAR2(255) not null, serviceid NUMBER(19,0), primary key (urn, soapaction))
create table identity_provider (objectid NUMBER(19,0) not null, version NUMBER(10,0) not null, description VARCHAR2(255), name VARCHAR2(255), type NUMBER(10,0), properties VARCHAR2(255), primary key (objectid))
create table internal_group (objectid NUMBER(19,0) not null, version NUMBER(10,0) not null, description VARCHAR2(255), name VARCHAR2(255), primary key (objectid))
create table internal_user_group (internal_user NUMBER(19,0) not null, internal_group NUMBER(19,0) not null, primary key (internal_user, internal_group))
create table jms_endpoint (objectid NUMBER(19,0) not null, version NUMBER(10,0) not null, connection_oid NUMBER(19,0), name VARCHAR2(255), destination_name VARCHAR2(255), reply_type NUMBER(5,0), username VARCHAR2(255), password VARCHAR2(255), max_concurrent_requests NUMBER(10,0), is_message_source NUMBER(1,0), primary key (objectid))
create table internal_user (objectid NUMBER(19,0) not null, version NUMBER(10,0) not null, name VARCHAR2(255), login VARCHAR2(255), password VARCHAR2(255), first_name VARCHAR2(255), last_name VARCHAR2(255), email VARCHAR2(255), title VARCHAR2(255), primary key (objectid))
create table hibernate_unique_key ( next_hi NUMBER(10,0) )
insert into hibernate_unique_key values ( 0 )
