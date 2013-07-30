-- --------------------------------------------------------------------------
-- Schema for audit tables
-- --------------------------------------------------------------------------
--
-- This is created from a script since we need a delete cascade that is not
-- supported by hibernate:
--
-- http://opensource.atlassian.com/projects/hibernate/browse/ANN-747
--
-- See Core_Dev_Useful_Info#Database_Changes on Layer 7 wiki
--

--
-- FUNCTION --
--
-- These are helper functions used by derby. Derby's built in functions are ver limited. These help deal with goid's

-- converts a high and a low bigint to a goid
CREATE FUNCTION toGoid(high bigint, low bigint) RETURNS char(16) for bit data
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.toGoid';

-- returns a goid as a hex string
CREATE FUNCTION goidToString(bytes char(16) for bit data) RETURNS CHAR(32)
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.goidToString';

-- returns the first parameter if it is not null otherwise returns the second parameter.
CREATE FUNCTION ifNull(v1 VARCHAR(128), v2 VARCHAR(128)) RETURNS VARCHAR(128)
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.ifNull';

-- returns a random long using our random long utils
CREATE FUNCTION randomLong() RETURNS bigint
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.randomLong';

-- returns a random long using our random long utils. This long is guaranteed to not be 0
CREATE FUNCTION randomLongNotReserved() RETURNS bigint
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.randomLongNotReserved';

CREATE procedure setVariable(keyParam CHAR(128), valueParam CHAR(128))
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.setVariable';

CREATE FUNCTION getVariable(keyParam CHAR(128)) RETURNS CHAR(128)
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.getVariable';


create table security_zone (
  goid CHAR(16) FOR BIT DATA not null,
  version integer not null,
  name varchar(128) not null unique,
  description varchar(255) not null,
  entity_types varchar(4096) not null,
  PRIMARY KEY (goid)
);

create table audit_admin (
    action char(1),
    entity_class varchar(1024),
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
    signature varchar(1024),
    audit_level varchar(12) not null,
    user_id varchar(255),
    user_name varchar(255),
    primary key (objectid)
);

create table audit_message (
    authenticated smallint default 0,
    authenticationType integer,
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
    mapping_values_oid bigint,
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
    version integer,
    digested varchar(36) not null,
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
    create_time bigint,
    primary key (objectid)
);

create table message_context_mapping_values (
    objectid bigint not null,
    digested varchar(36) not null,
    mapping_keys_oid bigint,
    auth_user_provider_id bigint,
    auth_user_id varchar(255),
    auth_user_unique_id varchar(255),
    service_operation varchar(255),
    mapping1_value varchar(255),
    mapping2_value varchar(255),
    mapping3_value varchar(255),
    mapping4_value varchar(255),
    mapping5_value varchar(255),
    create_time bigint,
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
    add constraint FK33C837A37AEF109A
    foreign key (objectid)
    references audit_main
    on delete cascade;

alter table audit_message
    add constraint message_context_mapping
    foreign key (mapping_values_oid)
    references message_context_mapping_values (objectid);

alter table audit_system
    add constraint FKB22BD7137AEF109A
    foreign key (objectid)
    references audit_main
    on delete cascade;

alter table message_context_mapping_values
    add constraint FKABF3A97B4B03F6D1
    foreign key (mapping_keys_oid)
    references message_context_mapping_keys;

-- --------------------------------------------------------------------------
-- Schema for schema versioning
-- --------------------------------------------------------------------------
--
-- This is created from a script since we use JDBC for access
--

create table ssg_version (
    current_version varchar(10) not null
);

insert into ssg_version (current_version) VALUES ('8.0.0');

-- --------------------------------------------------------------------------
-- Schema for hibernate support
-- --------------------------------------------------------------------------

--
-- Initialize the object identifier high value to 1
--
create sequence hibernate_sequence start with 1;

-- --------------------------------------------------------------------------
-- Schema for everything else, not very tested / reviewed
-- --------------------------------------------------------------------------

create table active_connector (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(128) not null,
    version integer,
    enabled smallint,
    hardwired_service_oid bigint,
    type varchar(64) not null,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    old_objectid bigint,
    primary key (goid)
);

create table active_connector_property (
    connector_goid CHAR(16) FOR BIT DATA not null,
    value varchar(32672) not null,
    name varchar(128) not null,
    primary key (connector_goid, name)
);

create table client_cert (
    objectid bigint not null,
    cert clob(2147483647),
    issuer_dn varchar(2048),
    serial varchar(255),
    ski varchar(64),
    subject_dn varchar(2048),
    thumbprint_sha1 varchar(64),
    login varchar(255),
    provider bigint not null,
    reset_counter integer not null,
    user_id varchar(255),
    primary key (objectid)
);

create table cluster_info (
    nodeid varchar(32) not null,
    address varchar(39) not null,
    avgload double not null,
    uptime bigint not null,
    esm_address varchar(39) not null,
    statustimestamp bigint not null,
    mac varchar(18) not null,
    multicast_address varchar(39),
    name varchar(128) not null,
    primary key (nodeid)
);

create table cluster_master (
  nodeid varchar(32),
  touched_time bigint not null,
  version integer not null
);

create table cluster_properties (
    goid CHAR(16) FOR BIT DATA not null,
    propKey varchar(255) not null unique,
    version integer,
    propValue clob(2147483647) not null,
    properties clob(2147483647),
    primary key (goid)
);

create table connector (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(128) not null,
    version integer,
    client_auth integer,
    enabled smallint,
    endpoints varchar(255) not null,
    key_alias varchar(255),
    keystore_oid bigint,
    port integer,
    scheme varchar(128) not null,
    secure smallint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (goid)
);

create table connector_property (
    connector_goid CHAR(16) FOR BIT DATA  not null,
    value varchar(32672) not null,
    name varchar(128) not null,
    primary key (connector_goid, name)
);

create table counters (
    counterid bigint not null,
    countername varchar(255) not null,
    cnt_sec bigint default 0,
    cnt_min bigint default 0,
    cnt_hr bigint default 0,
    cnt_day bigint default 0,
    cnt_mnt bigint default 0,
    last_update bigint default 0,
    primary key (counterId),
    unique (countername)
);

create table email_listener (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(128) not null,
    version integer,
    active smallint not null,
    delete_on_receive smallint not null,
    folder varchar(255) not null,
    host varchar(128) not null,
    password varchar(32) not null,
    poll_interval integer not null,
    port integer not null,
    properties clob(2147483647),
    server_type varchar(4) not null,
    use_ssl smallint not null,
    username varchar(255) not null,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (goid)
);

create table email_listener_state (
    goid CHAR(16) FOR BIT DATA not null,
    last_message_id bigint,
    last_poll_time bigint,
    owner_node_id varchar(36),
    version integer,
    email_listener_goid CHAR(16) FOR BIT DATA not null,
    primary key (goid),
    unique (email_listener_goid)
);

create table fed_group (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    description varchar(4096),
    provider_oid bigint not null,
    primary key (objectid)
);

create table fed_group_virtual (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    description varchar(4096),
    provider_oid bigint not null,
    saml_email_pattern varchar(128),
    x509_subject_dn_pattern varchar(1024),
    properties clob(2147483647),
    primary key (objectid)
);

create table fed_user (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    email varchar(128),
    first_name varchar(32),
    last_name varchar(32),
    login varchar(255) not null,
    provider_oid bigint,
    subject_dn varchar(255),
    primary key (objectid)
);

create table fed_user_group (
    provider_oid bigint not null,
    fed_group_oid bigint not null,
    fed_user_oid bigint not null,
    primary key (provider_oid, fed_group_oid, fed_user_oid)
);

create table folder (
    objectid bigint not null,
    version integer not null,
    name varchar(255),
    parent_folder_oid bigint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table http_configuration (
    objectid bigint not null,
    timeout_connect integer,
    follow_redirects smallint,
    host varchar(128) not null,
    ntlm_domain varchar(255),
    ntlm_host varchar(128),
    password_oid bigint,
    path varchar(4096),
    port integer,
    protocol varchar(255),
    proxy_host varchar(128),
    proxy_password_oid bigint,
    proxy_port integer,
    proxy_username varchar(255),
    proxy_use varchar(255),
    timeout_read integer,
    tls_cipher_suites varchar(4096),
    tls_key_use varchar(255),
    tls_key_alias varchar(255),
    tls_keystore_oid bigint,
    tls_version varchar(8),
    username varchar(255),
    version integer,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table identity_provider (
    objectid bigint not null,
    version integer,
    name varchar(128) not null,
    description clob(2147483647),
    type integer not null,
    properties clob(2147483647),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table internal_group (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    description varchar(4096),
    enabled smallint default 1,
    primary key (objectid)
);

create table internal_user (
    objectid bigint not null,
    version integer,
    name varchar(128) not null,
    login varchar(255) not null,
    password varchar(256) not null,
    digest varchar(32),
    first_name varchar(32),
    last_name varchar(32),
    email varchar(128),
    description varchar(255),
    expiration bigint not null,
    password_expiry bigint,
    change_password smallint default 1,
    enabled smallint default 1,
    properties clob(2147483647),
    primary key (objectid)
);

create table internal_user_group (
    objectid bigint not null,
    provider_oid bigint not null,
    subgroup_id varchar(255),
    user_id bigint not null,
    internal_group bigint not null,
    version integer,
    primary key (objectid)
);

create table jdbc_connection (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(128) not null,
    version integer,
    driver_class varchar(1024) not null,
    enabled smallint,
    jdbc_url varchar(4096) not null,
    max_pool_size integer,
    min_pool_size integer,
    password varchar(255) not null,
    additional_properties clob(2147483647),
    user_name varchar(255) not null,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (goid)
);

create table siteminder_configuration (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  name varchar(128) not null,
  agent_name varchar(256) not null,
  address varchar(128) not null,
  secret varchar(4096) not null,
  ipcheck smallint default 0,
  update_sso_token smallint default 0,
  enabled smallint,
  hostname varchar(255) not null,
  fipsmode integer not null default 0,
  host_configuration varchar(256),
  user_name varchar(256),
  password_oid bigint,
  noncluster_failover smallint default 0,
  cluster_threshold integer DEFAULT 50,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (goid)
);

CREATE TABLE siteminder_configuration_property (
  goid CHAR(16) FOR BIT DATA references siteminder_configuration(goid) on delete cascade,
  name varchar(128) not null,
  value varchar(32672) not null,
  primary key (goid,name)
);

create table jms_connection (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(128) not null,
    version integer,
    destination_factory_url varchar(4096),
    factory_classname varchar(1024),
    jndi_url varchar(255),
    password varchar(255),
    properties clob(2147483647),
    provider_type varchar(255),
    queue_factory_url varchar(255),
    is_template smallint default 0,
    topic_factory_url varchar(255),
    username varchar(255),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (goid)
);

create table jms_endpoint (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(128) not null,
    version integer,
    acknowledgement_type varchar(255),
    connection_goid CHAR(16) FOR BIT DATA not null,
    destination_name varchar(128),
    disabled smallint,
    failure_destination_name varchar(128),
    max_concurrent_requests integer,
    is_message_source smallint default 0,
    outbound_message_type varchar(255),
    password varchar(255),
    destination_type smallint,
    reply_to_queue_name varchar(128),
    reply_type integer default 0,
    request_max_size bigint not null,
    is_template smallint default 0,
    use_message_id_for_correlation smallint,
    username varchar(255),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    old_objectid bigint,
    primary key (goid)
);

create table keystore_file (
    objectid bigint not null,
    version integer,
    name varchar(128) not null,
    format varchar(255),
    databytes blob(2147483647),
    properties clob(2147483647),
    primary key (objectid)
);

create table keystore_key_metadata (
  objectid bigint not null,
  version integer,
  keystore_file_oid bigint not null references keystore_file(objectid) on delete cascade,
  alias varchar(255) not null,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (objectid),
  unique (keystore_file_oid, alias)
);

create table logon_info (
    goid CHAR(16) FOR BIT DATA not null,
    fail_count integer,
    last_activity bigint,
    last_attempted bigint,
    login varchar(255),
    provider_oid bigint,
    state varchar(255),
    version integer,
    primary key (goid)
);

create table message_id (
  messageid varchar(255) not null,
  expires bigint not null,
  primary key (messageid)
);

create table password_history (
    objectid bigint not null,
    last_changed bigint,
    prev_password varchar(255) not null,
    internal_user_oid bigint not null,
    primary key (objectid)
);

create table password_policy (
    objectid bigint not null,
    version integer,
    properties clob(2147483647),
    internal_identity_provider_oid bigint,
    primary key (objectid)
);

create table policy (
    objectid bigint not null,
    version integer not null,
    guid varchar(255),
    name varchar(255),
    policy_type varchar(255),
    "xml" clob(2147483647),
    soap smallint,
    internal_tag varchar(64),
    folder_oid bigint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table policy_alias (
    objectid bigint not null,
    version integer not null,
    policy_oid bigint,
    folder_oid bigint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table policy_version (
    objectid bigint not null,
    version integer not null,
    name varchar(255),
    policy_oid bigint,
    ordinal bigint,
    time bigint,
    user_provider_oid bigint,
    user_login varchar(255),
    "xml"  clob(2147483647),
    active smallint,
    primary key (objectid)
);

create table published_service (
    objectid bigint not null,
    version integer not null,
    name varchar(255),
    wsdl_url varchar(4096),
    wsdl_xml clob(2147483647),
    disabled smallint,
    soap smallint,
    internal smallint,
    routing_uri varchar(255),
    default_routing_url varchar(255),
    http_methods varchar(255),
    lax_resolution smallint,
    wss_processing smallint,
    tracing smallint,
    soap_version varchar(255),
    policy_oid bigint,
    folder_oid bigint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table published_service_alias (
    objectid bigint not null,
    version integer not null,
    published_service_oid bigint,
    folder_oid bigint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table rbac_assignment (
    objectid bigint not null,
    provider_oid bigint not null,
    role_oid bigint not null,
    identity_id varchar(255) not null,
    entity_type varchar(50) not null,
    primary key (objectid),
    unique (provider_oid, role_oid, identity_id, entity_type)
);

create table rbac_permission (
    objectid bigint not null,
    version integer,
    role_oid bigint not null,
    operation_type varchar(16) not null,
    other_operation varchar(255),
    entity_type varchar(255) not null,
    primary key (objectid)
);

create table rbac_predicate (
    objectid bigint not null,
    version integer,
    permission_oid bigint not null,
    primary key (objectid)
);

create table rbac_predicate_attribute (
    objectid bigint not null,
    attribute varchar(255) not null,
    value varchar(255),
    mode varchar(255),
    primary key (objectid)
);

create table rbac_predicate_entityfolder (
    objectid bigint not null,
    entity_type varchar(64),
    entity_id varchar(255),
    primary key (objectid)
);

create table rbac_predicate_folder (
    objectid bigint not null,
    folder_oid bigint not null,
    transitive smallint not null,
    primary key (objectid)
);

create table rbac_predicate_security_zone (
    objectid bigint not null references rbac_predicate(objectid) on delete cascade,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete cascade,
    primary key (objectid)
);

create table rbac_predicate_oid (
    objectid bigint not null,
    entity_id varchar(255) not null,
    primary key (objectid)
);

create table rbac_role (
    objectid bigint not null,
    version integer,
    name varchar(128) not null,
    tag varchar(36),
    entity_type varchar(255),
    entity_oid bigint,
    entity_goid CHAR(16) FOR BIT DATA,
    description varchar(255),
    user_created smallint not null default 0,
    primary key (objectid)
);

create table assertion_access (
  objectid bigint not null,
  version integer,
  name varchar(255) not null unique,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (objectid)
);

create table resolution_configuration (
    objectid bigint not null,
    version integer,
    name varchar(128) not null,
    path_required smallint default 0,
    path_case_sensitive smallint default 0,
    use_url_header smallint default 0,
    use_service_oid smallint default 0,
    use_soap_action smallint default 0,
    use_soap_namespace smallint default 0,
    primary key (objectid)
);

create table resource_entry (
    objectid bigint not null,
    version integer,
    description varchar(2048),
    uri varchar(4096),
    uri_hash varchar(128),
    type varchar(32),
    content_type varchar(1024),
    content clob(2147483647),
    resource_key1 varchar(4096),
    resource_key2 varchar(4096),
    resource_key3 varchar(4096),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table revocation_check_policy (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    continue_server_unavailable smallint default 0,
    default_policy smallint default 0,
    default_success smallint default 0,
    revocation_policy_xml clob(2147483647),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table sample_messages (
    goid CHAR(16) FOR BIT DATA not null,
    name varchar(255),
    operation_name varchar(255),
    published_service_oid bigint,
    "xml" clob(2147483647),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (goid)
);

create table secure_password (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    description varchar(256),
    encoded_password clob(65535) not null,
    last_update bigint,
    type varchar(64) not null,
    usage_from_variable smallint,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table service_documents (
    objectid bigint not null,
    version integer not null,
    service_oid bigint,
    uri varchar(4096),
    type varchar(255),
    content_type varchar(1024),
    content clob(2147483647),
    primary key (objectid)
);

create table service_metrics (
    goid CHAR(16) FOR BIT DATA not null,
    nodeid varchar(255),
    published_service_oid bigint,
    resolution integer,
    period_start bigint,
    start_time bigint,
    end_time bigint,
    attempted integer,
    authorized integer,
    completed integer,
    back_min integer,
    back_sum bigint,
    back_max integer,
    front_min integer,
    front_sum bigint,
    front_max integer,
    interval_size integer,
    service_state varchar(16),
    primary key (goid)
);

create table service_metrics_details (
    service_metrics_goid CHAR(16) FOR BIT DATA not null,
    mapping_values_oid bigint not null,
    attempted integer,
    authorized integer,
    completed integer,
    back_min integer,
    back_sum bigint,
    back_max integer,
    front_min integer,
    front_sum bigint,
    front_max integer,
    primary key (service_metrics_goid, mapping_values_oid)
);

create table service_usage (
    serviceid bigint not null,
    nodeid varchar(255) not null,
    requestnr bigint,
    authorizedreqnr bigint,
    completedreqnr bigint,
    primary key (serviceid, nodeid)
);

create table shared_keys (
    encodingid varchar(32) not null,
    b64edval varchar(2048) not null,
    primary key (encodingid)
);

create table sink_config (
    objectid bigint not null,
    version integer,
    name varchar(32) not null,
    description clob(2147483647),
    type varchar(32),
    enabled smallint,
    severity varchar(32),
    categories clob(2147483647),
    properties clob(2147483647),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table trusted_cert (
    objectid bigint not null,
    cert_base64 clob(2147483647),
    issuer_dn varchar(2048),
    serial varchar(1024),
    ski varchar(64),
    subject_dn varchar(2048),
    thumbprint_sha1 varchar(64),
    name varchar(128) not null,
    revocation_policy_oid bigint,
    revocation_type varchar(128) not null,
    trust_anchor smallint,
    trusted_as_saml_attesting_entity smallint default 0,
    trusted_for_saml smallint default 0,
    trusted_for_client smallint default 0,
    trusted_for_server smallint default 0,
    trusted_for_ssl smallint default 0,
    verify_hostname smallint default 0,
    version integer,
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table trusted_esm (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    trusted_cert_oid bigint not null,
    primary key (objectid)
);

create table trusted_esm_user (
    objectid bigint not null,
    esm_user_display_name varchar(128),
    esm_user_id varchar(128),
    provider_oid bigint,
    user_id varchar(128),
    version integer,
    trusted_esm_oid bigint not null,
    primary key (objectid)
);

create table uddi_business_service_status (
    objectid bigint not null,
    published_service_oid bigint,
    metrics_reference_status varchar(255),
    uddi_metrics_tmodel_key varchar(255),
    uddi_policy_publish_url varchar(4096),
    policy_status varchar(255),
    uddi_policy_tmodel_key varchar(255),
    uddi_policy_url varchar(4096),
    uddi_registry_oid bigint,
    uddi_service_key varchar(255),
    uddi_service_name varchar(255),
    version integer,
    primary key (objectid)
);

create table uddi_proxied_service (
    objectid bigint not null,
    uddi_service_key varchar(255),
    uddi_service_name varchar(255),
    version integer,
    wsdl_service_name varchar(255),
    wsdl_service_namespace varchar(255),
    uddi_proxied_service_info_oid bigint not null,
    primary key (objectid)
);

create table uddi_proxied_service_info (
    objectid bigint not null,
    created_from_existing smallint,
    metrics_enabled smallint,
    publish_type varchar(255),
    publish_wspolicy_enabled smallint,
    publish_wspolicy_full smallint,
    publish_wspolicy_inlined smallint,
    published_service_oid bigint,
    remove_other_bindings smallint,
    properties clob(2147483647),
    uddi_business_key varchar(255),
    uddi_business_name varchar(255),
    uddi_registry_oid bigint,
    update_proxy_on_local_change smallint,
    version integer,
    wsdl_hash varchar(512),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table uddi_publish_status (
    objectid bigint not null,
    fail_count integer,
    publish_status varchar(255),
    uddi_proxied_service_info_oid bigint not null,
    version integer,
    primary key (objectid)
);

create table uddi_registries (
    objectid bigint not null,
    name varchar(128) not null,
    version integer,
    base_url varchar(4096),
    client_auth smallint,
    enabled smallint,
    inquiry_url varchar(4096),
    key_alias varchar(255),
    keystore_oid bigint,
    metrics_publish_frequency bigint,
    metrics_enabled smallint,
    monitoring_enabled smallint,
    monitor_frequency bigint,
    publish_url varchar(4096),
    password varchar(255),
    user_name varchar(255),
    security_url varchar(4096),
    subscribe_for_notifications smallint,
    subscription_url varchar(4096),
    registry_type varchar(255),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table uddi_registry_subscription (
    objectid bigint not null,
    uddi_subscription_check_time bigint,
    uddi_subscription_expiry_time bigint,
    uddi_subscription_key varchar(255),
    uddi_subscription_notified_time bigint,
    uddi_registry_oid bigint,
    version integer,
    primary key (objectid)
);

create table uddi_service_control (
    objectid bigint not null,
    disable_service_on_change smallint,
    has_been_overwritten smallint,
    has_had_endpoints_removed smallint,
    metrics_enabled smallint,
    monitoring_enabled smallint,
    publish_wspolicy_enabled smallint,
    publish_wspolicy_full smallint,
    publish_wspolicy_inlined smallint,
    published_service_oid bigint,
    uddi_business_key varchar(255),
    uddi_business_name varchar(255),
    uddi_registry_oid bigint,
    uddi_service_key varchar(255),
    uddi_service_name varchar(255),
    under_uddi_control smallint,
    update_wsdl_on_change smallint,
    version integer,
    wsdl_port_binding varchar(255),
    wsdl_port_binding_namespace varchar(255),
    wsdl_port_name varchar(255),
    wsdl_service_name varchar(255),
    security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
    primary key (objectid)
);

create table uddi_service_control_monitor_runtime (
    objectid bigint not null,
    access_point_url varchar(4096),
    last_uddi_modified_timestamp bigint,
    uddi_service_control_oid bigint,
    version integer,
    primary key (objectid)
);

create table wsdm_subscription (
    objectid bigint not null,
    esm_service_oid bigint not null,
    last_notification bigint,
    notification_policy_guid varchar(36),
    owner_node_id varchar(64),
    published_service_oid bigint not null,
    callback_url varchar(4096) not null,
    reference_parameters varchar(16384),
    termination_time bigint not null,
    topic integer not null,
    uuid varchar(36) not null unique,
    version integer,
    primary key (objectid)
);

create table wssc_session (
    objectid bigint not null,
    created bigint,
    encrypted_key varchar(255),
    expires bigint,
    identifier varchar(4096) not null,
    inbound smallint,
    namespace varchar(4096),
    provider_id bigint,
    service_url varchar(4096),
    session_key_hash varchar(128) not null unique,
    token varchar(32672),
    user_id varchar(255),
    user_login varchar(255),
    primary key (objectid)
);

alter table active_connector_property
    add constraint FK58920F603AEA90B6
    foreign key (connector_goid)
    references active_connector
    on delete cascade;

alter table connector_property
    add constraint FK7EC2A187BA66EE5C
    foreign key (connector_goid)
    references connector
    on delete cascade;

alter table email_listener_state
    add constraint FK5A708C492FC43EC3
    foreign key (email_listener_goid)
    references email_listener
    on delete cascade;

alter table folder
    add constraint FKB45D1C6EF8097918
    foreign key (parent_folder_oid)
    references folder;

alter table password_history
    add constraint FKF16E7AF0C9B8DFC1
    foreign key (internal_user_oid)
    references internal_user;

alter table policy
    add constraint FKC56DA532DB935A63
    foreign key (folder_oid)
    references folder;

alter table policy_alias
    add constraint FKA07B7103DB935A63
    foreign key (folder_oid)
    references folder
    on delete cascade;

alter table published_service
    add constraint FK25874164DB935A63
    foreign key (folder_oid)
    references folder;

alter table published_service
    add constraint FK25874164DAFA444B
    foreign key (policy_oid)
    references policy;

alter table published_service_alias
    add constraint FK6AE79FB5DB935A63
    foreign key (folder_oid)
    references folder
    on delete cascade;

alter table rbac_assignment
    add constraint FK51FEC6DACCD6DF3E
    foreign key (role_oid)
    references rbac_role;

alter table rbac_permission
    add constraint FKF5F905DCCCD6DF3E
    foreign key (role_oid)
    references rbac_role
    on delete cascade;

alter table rbac_predicate
    add constraint FKB894B40A45FC8430
    foreign key (permission_oid)
    references rbac_permission
    on delete cascade;

alter table rbac_predicate_attribute
    add constraint FK563B54A7918005E4
    foreign key (objectid)
    references rbac_predicate
    on delete cascade;

alter table rbac_predicate_entityfolder
    add constraint FK6AE46026918005E4
    foreign key (objectid)
    references rbac_predicate
    on delete cascade;

alter table rbac_predicate_folder
    add constraint FKF111A643DB935A63
    foreign key (folder_oid)
    references folder
    on delete cascade;

alter table rbac_predicate_folder
    add constraint FKF111A643918005E4
    foreign key (objectid)
    references rbac_predicate
    on delete cascade;

alter table rbac_predicate_oid
    add constraint FK37D47C15918005E4
    foreign key (objectid)
    references rbac_predicate
    on delete cascade;

alter table trusted_esm
    add constraint FK581D7A373ACD94B6
    foreign key (trusted_cert_oid)
    references trusted_cert;

alter table trusted_esm_user
    add constraint FKC48AF4D34548A1A6
    foreign key (trusted_esm_oid)
    references trusted_esm
    on delete cascade;

alter table uddi_proxied_service
    add constraint FK127C390874249C8B
    foreign key (uddi_proxied_service_info_oid)
    references uddi_proxied_service_info
    on delete cascade;

-- --------------------------------------------------------------------------
-- Populate initial data
-- --------------------------------------------------------------------------

INSERT INTO identity_provider (objectid,name,description,type,properties,version,security_zone_goid) VALUES (-2,'Internal Identity Provider','Internal Identity Provider',1,'<java version="1.6.0_01" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>adminEnabled</string><boolean>true</boolean></void></object></java>',0,NULL);

-- The same hash from resetAdmin.sh is used here. Digest property is set to NULL by default.
INSERT INTO internal_user VALUES (3,0,'admin','admin','$6$S7Z3HcudYNsObgs8$SjwZ3xtCkSjXOK2vHfOVEg2dJES3cgvtIUdHbEN/KdCBXoI6uuPSbxTEwcH.av6lpcb1p6Lu.gFeIX04FBxiJ.',NULL,'','','','',-1,1577865600000,0,1,NULL);

INSERT INTO folder VALUES (-5002, 0, 'Root Node', NULL, NULL);

INSERT INTO resolution_configuration (objectid, version, name, path_case_sensitive, use_url_header, use_service_oid, use_soap_action, use_soap_namespace) VALUES (-2, 0, 'Default', 1, 1, 1, 1, 1);

INSERT INTO cluster_master (nodeid, touched_time, version) VALUES (NULL, 0, 0);

-- placeholder for legacy Software Static, never loaded or saved
insert into keystore_file values (0, 0, 'Software Static', 'ss', null, null);

-- tar.gz of items in sca 6000 keydata directory
insert into keystore_file values (1, 0, 'HSM', 'hsm.sca.targz', null, null);

-- bytes of a PKCS#12 keystore
insert into keystore_file values (2, 0, 'Software DB', 'sdb.pkcs12', null, null);

-- placeholder for ID reserved for Luna, never loaded or saved
insert into keystore_file values (3, 0, 'SafeNet HSM', 'luna', null, null);

-- serialized NcipherKeyStoreData for an nCipher keystore
insert into keystore_file values (4, 0, 'nCipher HSM', 'hsm.NcipherKeyStoreData', null, null);

-- STIG default:
INSERT INTO password_policy (objectid, version, properties, internal_identity_provider_oid) VALUES (-2, 0, '<?xml version="1.0" encoding="UTF-8"?><java version="1.6.0_21" class="java.beans.XMLDecoder"> <object class="java.util.TreeMap">  <void method="put">   <string>allowableChangesPerDay</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>charDiffMinimum</string>   <int>4</int>  </void>  <void method="put">   <string>forcePasswordChangeNewUser</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>lowerMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>maxPasswordLength</string>   <int>32</int>  </void>  <void method="put">   <string>minPasswordLength</string>   <int>8</int>  </void>  <void method="put">   <string>noRepeatingCharacters</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>numberMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>passwordExpiry</string>   <int>90</int>  </void>  <void method="put">   <string>repeatFrequency</string>   <int>10</int>  </void>  <void method="put">   <string>symbolMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>upperMinimum</string>   <int>1</int>  </void> </object></java>', -2);

-- Default global resources
INSERT INTO resource_entry (objectid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (-3,0,'http://schemas.xmlsoap.org/soap/envelope/','hC3quuokv29o8XDUK1vtJg29ywKS/fDsnJsj2chtn0maXa6J/7ga3LQxz12tlDYbLmJVWV/iP4PJsmBZ7lGiaQ==','XML_SCHEMA','text/xml','<?xml version=''1.0'' encoding=''UTF-8'' ?>\n<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n           xmlns:tns=\"http://schemas.xmlsoap.org/soap/envelope/\"\n           targetNamespace=\"http://schemas.xmlsoap.org/soap/envelope/\" >\n  <!-- Envelope, header and body -->\n  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n  <xs:complexType name=\"Envelope\" >\n    <xs:sequence>\n      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n  <xs:element name=\"Header\" type=\"tns:Header\" />\n  <xs:complexType name=\"Header\" >\n    <xs:sequence>\n      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n  <xs:element name=\"Body\" type=\"tns:Body\" />\n  <xs:complexType name=\"Body\" >\n    <xs:sequence>\n      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" >\n          <xs:annotation>\n            <xs:documentation>\n                  Prose in the spec does not specify that attributes are allowed on the Body element\n                </xs:documentation>\n          </xs:annotation>\n        </xs:anyAttribute>\n  </xs:complexType>\n  <!-- Global Attributes.  The following attributes are intended to be usable via qualified attribute names on any complex type referencing them.  -->\n  <xs:attribute name=\"mustUnderstand\" >\n     <xs:simpleType>\n     <xs:restriction base=''xs:boolean''>\n           <xs:pattern value=''0|1'' />\n         </xs:restriction>\n   </xs:simpleType>\n  </xs:attribute>\n  <xs:attribute name=\"actor\" type=\"xs:anyURI\" />\n  <xs:simpleType name=\"encodingStyle\" >\n    <xs:annotation>\n          <xs:documentation>\n            ''encodingStyle'' indicates any canonicalization conventions followed in the contents of the containing element.  For example, the value ''http://schemas.xmlsoap.org/soap/encoding/'' indicates the pattern described in SOAP specification\n          </xs:documentation>\n        </xs:annotation>\n    <xs:list itemType=\"xs:anyURI\" />\n  </xs:simpleType>\n  <xs:attribute name=\"encodingStyle\" type=\"tns:encodingStyle\" />\n  <xs:attributeGroup name=\"encodingStyle\" >\n    <xs:attribute ref=\"tns:encodingStyle\" />\n  </xs:attributeGroup>  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n  <xs:complexType name=\"Fault\" final=\"extension\" >\n    <xs:annotation>\n          <xs:documentation>\n            Fault reporting structure\n          </xs:documentation>\n        </xs:annotation>\n    <xs:sequence>\n      <xs:element name=\"faultcode\" type=\"xs:QName\" />\n      <xs:element name=\"faultstring\" type=\"xs:string\" />\n      <xs:element name=\"faultactor\" type=\"xs:anyURI\" minOccurs=\"0\" />\n      <xs:element name=\"detail\" type=\"tns:detail\" minOccurs=\"0\" />\n    </xs:sequence>\n  </xs:complexType>\n  <xs:complexType name=\"detail\">\n    <xs:sequence>\n      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" />\n  </xs:complexType>\n</xs:schema>','http://schemas.xmlsoap.org/soap/envelope/',NULL);
INSERT INTO resource_entry (objectid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (-4,0,'http://www.w3.org/2003/05/soap-envelope/','/IwS8Jif23iT/LGYVajOwoHmLxd/Acxqv8VZoeG7SN/5Qp0gcKmM+pnzTYc1qeaqg0YucLMOt3mmhPzH/tcpUQ==','XML_SCHEMA','text/xml','<?xml version=''1.0''?>\n<!-- Schema defined in the SOAP Version 1.2 Part 1 specification\n     Recommendation:\n     http://www.w3.org/TR/2003/REC-soap12-part1-20030624/\n\n     Copyright (C)2003 W3C(R) (MIT, ERCIM, Keio), All Rights Reserved.\n     W3C viability, trademark, document use and software licensing rules\n     apply.\n     http://www.w3.org/Consortium/Legal/\n\n     This document is governed by the W3C Software License [1] as\n     described in the FAQ [2].\n\n     [1] http://www.w3.org/Consortium/Legal/copyright-software-19980720\n     [2] http://www.w3.org/Consortium/Legal/IPR-FAQ-20000620.html#DTD\n-->\n\n<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n           xmlns:tns=\"http://www.w3.org/2003/05/soap-envelope\"\n           targetNamespace=\"http://www.w3.org/2003/05/soap-envelope\" \n		   elementFormDefault=\"qualified\" >\n\n  <xs:import namespace=\"http://www.w3.org/XML/1998/namespace\" \n             schemaLocation=\"http://www.w3.org/2001/xml.xsd\"/>\n\n  <!-- Envelope, header and body -->\n  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n  <xs:complexType name=\"Envelope\" >\n    <xs:sequence>\n      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n\n  <xs:element name=\"Header\" type=\"tns:Header\" />\n  <xs:complexType name=\"Header\" >\n    <xs:annotation>\n	  <xs:documentation>\n	  Elements replacing the wildcard MUST be namespace qualified, but can be in the targetNamespace\n	  </xs:documentation>\n	</xs:annotation>\n    <xs:sequence>\n      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"  />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n  \n  <xs:element name=\"Body\" type=\"tns:Body\" />\n  <xs:complexType name=\"Body\" >\n    <xs:sequence>\n      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n\n  <!-- Global Attributes.  The following attributes are intended to be\n  usable via qualified attribute names on any complex type referencing\n  them.  -->\n  <xs:attribute name=\"mustUnderstand\" type=\"xs:boolean\" default=\"0\" />\n  <xs:attribute name=\"relay\" type=\"xs:boolean\" default=\"0\" />\n  <xs:attribute name=\"role\" type=\"xs:anyURI\" />\n\n  <!-- ''encodingStyle'' indicates any canonicalization conventions\n  followed in the contents of the containing element.  For example, the\n  value ''http://www.w3.org/2003/05/soap-encoding'' indicates the pattern\n  described in the SOAP Version 1.2 Part 2: Adjuncts Recommendation -->\n\n  <xs:attribute name=\"encodingStyle\" type=\"xs:anyURI\" />\n\n  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n  <xs:complexType name=\"Fault\" final=\"extension\" >\n    <xs:annotation>\n	  <xs:documentation>\n	    Fault reporting structure\n	  </xs:documentation>\n	</xs:annotation>\n    <xs:sequence>\n      <xs:element name=\"Code\" type=\"tns:faultcode\" />\n      <xs:element name=\"Reason\" type=\"tns:faultreason\" />\n      <xs:element name=\"Node\" type=\"xs:anyURI\" minOccurs=\"0\" />\n	  <xs:element name=\"Role\" type=\"xs:anyURI\" minOccurs=\"0\" />\n      <xs:element name=\"Detail\" type=\"tns:detail\" minOccurs=\"0\" />\n    </xs:sequence>\n  </xs:complexType>\n\n  <xs:complexType name=\"faultreason\" >\n    <xs:sequence>\n	  <xs:element name=\"Text\" type=\"tns:reasontext\" \n                  minOccurs=\"1\"  maxOccurs=\"unbounded\" />\n	</xs:sequence>\n  </xs:complexType>\n\n  <xs:complexType name=\"reasontext\" >\n    <xs:simpleContent>\n	  <xs:extension base=\"xs:string\" >\n	    <xs:attribute ref=\"xml:lang\" use=\"required\" />\n	  </xs:extension>\n	</xs:simpleContent>\n  </xs:complexType>\n  \n  <xs:complexType name=\"faultcode\">\n    <xs:sequence>\n      <xs:element name=\"Value\"\n                  type=\"tns:faultcodeEnum\"/>\n      <xs:element name=\"Subcode\"\n                  type=\"tns:subcode\"\n                  minOccurs=\"0\"/>\n    </xs:sequence>\n  </xs:complexType>\n\n  <xs:simpleType name=\"faultcodeEnum\">\n    <xs:restriction base=\"xs:QName\">\n      <xs:enumeration value=\"tns:DataEncodingUnknown\"/>\n      <xs:enumeration value=\"tns:MustUnderstand\"/>\n      <xs:enumeration value=\"tns:Receiver\"/>\n      <xs:enumeration value=\"tns:Sender\"/>\n      <xs:enumeration value=\"tns:VersionMismatch\"/>\n    </xs:restriction>\n  </xs:simpleType>\n\n  <xs:complexType name=\"subcode\">\n    <xs:sequence>\n      <xs:element name=\"Value\"\n                  type=\"xs:QName\"/>\n      <xs:element name=\"Subcode\"\n                  type=\"tns:subcode\"\n                  minOccurs=\"0\"/>\n    </xs:sequence>\n  </xs:complexType>\n\n  <xs:complexType name=\"detail\">\n    <xs:sequence>\n      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"  />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" /> \n  </xs:complexType>\n\n  <!-- Global element declaration and complex type definition for header entry returned due to a mustUnderstand fault -->\n  <xs:element name=\"NotUnderstood\" type=\"tns:NotUnderstoodType\" />\n  <xs:complexType name=\"NotUnderstoodType\" >\n    <xs:attribute name=\"qname\" type=\"xs:QName\" use=\"required\" />\n  </xs:complexType>\n\n\n  <!-- Global element and associated types for managing version transition as described in Appendix A of the SOAP Version 1.2 Part 1 Recommendation  -->  <xs:complexType name=\"SupportedEnvType\" >\n    <xs:attribute name=\"qname\" type=\"xs:QName\" use=\"required\" />\n  </xs:complexType>\n\n  <xs:element name=\"Upgrade\" type=\"tns:UpgradeType\" />\n  <xs:complexType name=\"UpgradeType\" >\n    <xs:sequence>\n	  <xs:element name=\"SupportedEnvelope\" type=\"tns:SupportedEnvType\" minOccurs=\"1\" maxOccurs=\"unbounded\" />\n	</xs:sequence>\n  </xs:complexType>\n\n\n</xs:schema>','http://www.w3.org/2003/05/soap-envelope',NULL);
INSERT INTO resource_entry (objectid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (-5,0,'http://www.w3.org/2001/xml.xsd','hVcrKrS/aEB3urzQRjRATz5Jr2R4ai52xKbb/R2iaclst0ENOxLEU+IPdEtmrfiKGq0HOlCG3JDTTliMnoL0Zg==','XML_SCHEMA','text/xml','<?xml version=''1.0''?>\n<xs:schema targetNamespace=\"http://www.w3.org/XML/1998/namespace\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xml:lang=\"en\">\n\n <xs:annotation>\n  <xs:documentation>\n   See http://www.w3.org/XML/1998/namespace.html and\n   http://www.w3.org/TR/REC-xml for information about this namespace.\n\n    This schema document describes the XML namespace, in a form\n    suitable for import by other schema documents.\n\n    Note that local names in this namespace are intended to be defined\n    only by the World Wide Web Consortium or its subgroups.  The\n    following names are currently defined in this namespace and should\n    not be used with conflicting semantics by any Working Group,\n    specification, or document instance:\n\n    base (as an attribute name): denotes an attribute whose value\n         provides a URI to be used as the base for interpreting any\n         relative URIs in the scope of the element on which it\n         appears; its value is inherited.  This name is reserved\n         by virtue of its definition in the XML Base specification.\n\n    id   (as an attribute name): denotes an attribute whose value\n         should be interpreted as if declared to be of type ID.\n         This name is reserved by virtue of its definition in the\n         xml:id specification.\n\n    lang (as an attribute name): denotes an attribute whose value\n         is a language code for the natural language of the content of\n         any element; its value is inherited.  This name is reserved\n         by virtue of its definition in the XML specification.\n\n    space (as an attribute name): denotes an attribute whose\n         value is a keyword indicating what whitespace processing\n         discipline is intended for the content of the element; its\n         value is inherited.  This name is reserved by virtue of its\n         definition in the XML specification.\n\n    Father (in any context at all): denotes Jon Bosak, the chair of\n         the original XML Working Group.  This name is reserved by\n         the following decision of the W3C XML Plenary and\n         XML Coordination groups:\n\n             In appreciation for his vision, leadership and dedication\n             the W3C XML Plenary on this 10th day of February, 2000\n             reserves for Jon Bosak in perpetuity the XML name\n             xml:Father\n  </xs:documentation>\n </xs:annotation>\n\n <xs:annotation>\n  <xs:documentation>This schema defines attributes and an attribute group\n        suitable for use by\n        schemas wishing to allow xml:base, xml:lang, xml:space or xml:id\n        attributes on elements they define.\n\n        To enable this, such a schema must import this schema\n        for the XML namespace, e.g. as follows:\n        &lt;schema . . .>\n         . . .\n         &lt;import namespace=\"http://www.w3.org/XML/1998/namespace\"\n                    schemaLocation=\"http://www.w3.org/2001/xml.xsd\"/>\n\n        Subsequently, qualified reference to any of the attributes\n        or the group defined below will have the desired effect, e.g.\n\n        &lt;type . . .>\n         . . .\n         &lt;attributeGroup ref=\"xml:specialAttrs\"/>\n \n         will define a type which will schema-validate an instance\n         element with any of those attributes</xs:documentation>\n </xs:annotation>\n\n <xs:annotation>\n  <xs:documentation>In keeping with the XML Schema WG''s standard versioning\n   policy, this schema document will persist at\n   http://www.w3.org/2007/08/xml.xsd.\n   At the date of issue it can also be found at\n   http://www.w3.org/2001/xml.xsd.\n   The schema document at that URI may however change in the future,\n   in order to remain compatible with the latest version of XML Schema\n   itself, or with the XML namespace itself.  In other words, if the XML\n   Schema or XML namespaces change, the version of this document at\n   http://www.w3.org/2001/xml.xsd will change\n   accordingly; the version at\n   http://www.w3.org/2007/08/xml.xsd will not change.\n  </xs:documentation>\n </xs:annotation>\n\n <xs:attribute name=\"lang\">\n  <xs:annotation>\n   <xs:documentation>Attempting to install the relevant ISO 2- and 3-letter\n         codes as the enumerated possible values is probably never\n         going to be a realistic possibility.  See\n         RFC 3066 at http://www.ietf.org/rfc/rfc3066.txt and the IANA registry\n         at http://www.iana.org/assignments/lang-tag-apps.htm for\n         further information.\n\n         The union allows for the ''un-declaration'' of xml:lang with\n         the empty string.</xs:documentation>\n  </xs:annotation>\n  <xs:simpleType>\n   <xs:union memberTypes=\"xs:language\">\n    <xs:simpleType>\n     <xs:restriction base=\"xs:string\">\n      <xs:enumeration value=\"\"/>\n     </xs:restriction>\n    </xs:simpleType>\n   </xs:union>\n  </xs:simpleType>\n </xs:attribute>\n\n <xs:attribute name=\"space\">\n  <xs:simpleType>\n   <xs:restriction base=\"xs:NCName\">\n    <xs:enumeration value=\"default\"/>\n    <xs:enumeration value=\"preserve\"/>\n   </xs:restriction>\n  </xs:simpleType>\n </xs:attribute>\n\n <xs:attribute name=\"base\" type=\"xs:anyURI\">\n  <xs:annotation>\n   <xs:documentation>See http://www.w3.org/TR/xmlbase/ for\n                     information about this attribute.</xs:documentation>\n  </xs:annotation>\n </xs:attribute>\n\n <xs:attribute name=\"id\" type=\"xs:ID\">\n  <xs:annotation>\n   <xs:documentation>See http://www.w3.org/TR/xml-id/ for\n                     information about this attribute.</xs:documentation>\n  </xs:annotation>\n </xs:attribute>\n\n <xs:attributeGroup name=\"specialAttrs\">\n  <xs:attribute ref=\"xml:base\"/>\n  <xs:attribute ref=\"xml:lang\"/>\n  <xs:attribute ref=\"xml:space\"/>\n  <xs:attribute ref=\"xml:id\"/>\n </xs:attributeGroup>\n\n</xs:schema>','http://www.w3.org/XML/1998/namespace',NULL);
INSERT INTO resource_entry (objectid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (-6,0,'http://www.w3.org/2001/datatypes.dtd','CnGeQLLg3aDZGm+VXQAHZEimjslNt6DgjHWn3RZ8VH3haj30QvOihEtZxgzq9y68dj9YSJ8JP71BQVEJ+9ycYg==','DTD','text/plain','<!--\n        DTD for XML Schemas: Part 2: Datatypes\n        $Id: datatypes.dtd,v 1.23 2001/03/16 17:36:30 ht Exp $\n        Note this DTD is NOT normative, or even definitive. - - the\n        prose copy in the datatypes REC is the definitive version\n        (which shouldn''t differ from this one except for this comment\n        and entity expansions, but just in case)\n  -->\n\n<!--\n        This DTD cannot be used on its own, it is intended\n        only for incorporation in XMLSchema.dtd, q.v.\n  -->\n\n<!-- Define all the element names, with optional prefix -->\n<!ENTITY % simpleType \"%p;simpleType\">\n<!ENTITY % restriction \"%p;restriction\">\n<!ENTITY % list \"%p;list\">\n<!ENTITY % union \"%p;union\">\n<!ENTITY % maxExclusive \"%p;maxExclusive\">\n<!ENTITY % minExclusive \"%p;minExclusive\">\n<!ENTITY % maxInclusive \"%p;maxInclusive\">\n<!ENTITY % minInclusive \"%p;minInclusive\">\n<!ENTITY % totalDigits \"%p;totalDigits\">\n<!ENTITY % fractionDigits \"%p;fractionDigits\">\n<!ENTITY % length \"%p;length\">\n<!ENTITY % minLength \"%p;minLength\">\n<!ENTITY % maxLength \"%p;maxLength\">\n<!ENTITY % enumeration \"%p;enumeration\">\n<!ENTITY % whiteSpace \"%p;whiteSpace\">\n<!ENTITY % pattern \"%p;pattern\">\n\n<!--\n        Customisation entities for the ATTLIST of each element\n        type. Define one of these if your schema takes advantage\n        of the anyAttribute=''##other'' in the schema for schemas\n  -->\n\n<!ENTITY % simpleTypeAttrs \"\">\n<!ENTITY % restrictionAttrs \"\">\n<!ENTITY % listAttrs \"\">\n<!ENTITY % unionAttrs \"\">\n<!ENTITY % maxExclusiveAttrs \"\">\n<!ENTITY % minExclusiveAttrs \"\">\n<!ENTITY % maxInclusiveAttrs \"\">\n<!ENTITY % minInclusiveAttrs \"\">\n<!ENTITY % totalDigitsAttrs \"\">\n<!ENTITY % fractionDigitsAttrs \"\">\n<!ENTITY % lengthAttrs \"\">\n<!ENTITY % minLengthAttrs \"\">\n<!ENTITY % maxLengthAttrs \"\">\n<!ENTITY % enumerationAttrs \"\">\n<!ENTITY % whiteSpaceAttrs \"\">\n<!ENTITY % patternAttrs \"\">\n\n<!-- Define some entities for informative use as attribute\n        types -->\n<!ENTITY % URIref \"CDATA\">\n<!ENTITY % XPathExpr \"CDATA\">\n<!ENTITY % QName \"NMTOKEN\">\n<!ENTITY % QNames \"NMTOKENS\">\n<!ENTITY % NCName \"NMTOKEN\">\n<!ENTITY % nonNegativeInteger \"NMTOKEN\">\n<!ENTITY % boolean \"(true|false)\">\n<!ENTITY % simpleDerivationSet \"CDATA\">\n<!--\n        #all or space-separated list drawn from derivationChoice\n  -->\n\n<!--\n        Note that the use of ''facet'' below is less restrictive\n        than is really intended:  There should in fact be no\n        more than one of each of minInclusive, minExclusive,\n        maxInclusive, maxExclusive, totalDigits, fractionDigits,\n        length, maxLength, minLength within datatype,\n        and the min- and max- variants of Inclusive and Exclusive\n        are mutually exclusive. On the other hand,  pattern and\n        enumeration may repeat.\n  -->\n<!ENTITY % minBound \"(%minInclusive; | %minExclusive;)\">\n<!ENTITY % maxBound \"(%maxInclusive; | %maxExclusive;)\">\n<!ENTITY % bounds \"%minBound; | %maxBound;\">\n<!ENTITY % numeric \"%totalDigits; | %fractionDigits;\">\n<!ENTITY % ordered \"%bounds; | %numeric;\">\n<!ENTITY % unordered\n   \"%pattern; | %enumeration; | %whiteSpace; | %length; |\n   %maxLength; | %minLength;\">\n<!ENTITY % facet \"%ordered; | %unordered;\">\n<!ENTITY % facetAttr \n        \"value CDATA #REQUIRED\n        id ID #IMPLIED\">\n<!ENTITY % fixedAttr \"fixed %boolean; #IMPLIED\">\n<!ENTITY % facetModel \"(%annotation;)?\">\n<!ELEMENT %simpleType;\n        ((%annotation;)?, (%restriction; | %list; | %union;))>\n<!ATTLIST %simpleType;\n    name      %NCName; #IMPLIED\n    final     %simpleDerivationSet; #IMPLIED\n    id        ID       #IMPLIED\n    %simpleTypeAttrs;>\n<!-- name is required at top level -->\n<!ELEMENT %restriction; ((%annotation;)?,\n                         (%restriction1; |\n                          ((%simpleType;)?,(%facet;)*)),\n                         (%attrDecls;))>\n<!ATTLIST %restriction;\n    base      %QName;                  #IMPLIED\n    id        ID       #IMPLIED\n    %restrictionAttrs;>\n<!--\n        base and simpleType child are mutually exclusive,\n        one is required.\n\n        restriction is shared between simpleType and\n        simpleContent and complexContent (in XMLSchema.xsd).\n        restriction1 is for the latter cases, when this\n        is restricting a complex type, as is attrDecls.\n  -->\n<!ELEMENT %list; ((%annotation;)?,(%simpleType;)?)>\n<!ATTLIST %list;\n    itemType      %QName;             #IMPLIED\n    id        ID       #IMPLIED\n    %listAttrs;>\n<!--\n        itemType and simpleType child are mutually exclusive,\n        one is required\n  -->\n<!ELEMENT %union; ((%annotation;)?,(%simpleType;)*)>\n<!ATTLIST %union;\n    id            ID       #IMPLIED\n    memberTypes   %QNames;            #IMPLIED\n    %unionAttrs;>\n<!--\n        At least one item in memberTypes or one simpleType\n        child is required\n  -->\n\n<!ELEMENT %maxExclusive; %facetModel;>\n<!ATTLIST %maxExclusive;\n        %facetAttr;\n        %fixedAttr;\n        %maxExclusiveAttrs;>\n<!ELEMENT %minExclusive; %facetModel;>\n<!ATTLIST %minExclusive;\n        %facetAttr;\n        %fixedAttr;\n        %minExclusiveAttrs;>\n\n<!ELEMENT %maxInclusive; %facetModel;>\n<!ATTLIST %maxInclusive;\n        %facetAttr;\n        %fixedAttr;\n        %maxInclusiveAttrs;>\n<!ELEMENT %minInclusive; %facetModel;>\n<!ATTLIST %minInclusive;\n        %facetAttr;\n        %fixedAttr;\n        %minInclusiveAttrs;>\n\n<!ELEMENT %totalDigits; %facetModel;>\n<!ATTLIST %totalDigits;\n        %facetAttr;\n        %fixedAttr;\n        %totalDigitsAttrs;>\n<!ELEMENT %fractionDigits; %facetModel;>\n<!ATTLIST %fractionDigits;\n        %facetAttr;\n        %fixedAttr;\n        %fractionDigitsAttrs;>\n\n<!ELEMENT %length; %facetModel;>\n<!ATTLIST %length;\n        %facetAttr;\n        %fixedAttr;\n        %lengthAttrs;>\n<!ELEMENT %minLength; %facetModel;>\n<!ATTLIST %minLength;\n        %facetAttr;\n        %fixedAttr;\n        %minLengthAttrs;>\n<!ELEMENT %maxLength; %facetModel;>\n<!ATTLIST %maxLength;\n        %facetAttr;\n        %fixedAttr;\n        %maxLengthAttrs;>\n\n<!-- This one can be repeated -->\n<!ELEMENT %enumeration; %facetModel;>\n<!ATTLIST %enumeration;\n        %facetAttr;\n        %enumerationAttrs;>\n\n<!ELEMENT %whiteSpace; %facetModel;>\n<!ATTLIST %whiteSpace;\n        %facetAttr;\n        %fixedAttr;\n        %whiteSpaceAttrs;>\n\n<!-- This one can be repeated -->\n<!ELEMENT %pattern; %facetModel;>\n<!ATTLIST %pattern;\n        %facetAttr;\n        %patternAttrs;>\n','datatypes',NULL);
INSERT INTO resource_entry (objectid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (-7,0,'http://www.w3.org/2001/XMLSchema.dtd','8yxOhhglB4ig2jm9Tl3Jb7wJ53OS0+aRQBJgpdleDH/HFJ9+XjbMys52YTDpRTqn8q1Zt8xAUMQEl9kEdjAlMw==','DTD','text/plain','<!-- DTD for XML Schemas: Part 1: Structures\n     Public Identifier: \"-//W3C//DTD XMLSCHEMA 200102//EN\"\n     Official Location: http://www.w3.org/2001/XMLSchema.dtd -->\n<!-- $Id: XMLSchema.dtd,v 1.31 2001/10/24 15:50:16 ht Exp $ -->\n<!-- Note this DTD is NOT normative, or even definitive. -->           <!--d-->\n<!-- prose copy in the structures REC is the definitive version -->    <!--d-->\n<!-- (which shouldn''t differ from this one except for this -->         <!--d-->\n<!-- comment and entity expansions, but just in case) -->              <!--d-->\n<!-- With the exception of cases with multiple namespace\n     prefixes for the XML Schema namespace, any XML document which is\n     not valid per this DTD given redefinitions in its internal subset of the\n     ''p'' and ''s'' parameter entities below appropriate to its namespace\n     declaration of the XML Schema namespace is almost certainly not\n     a valid schema. -->\n\n<!-- The simpleType element and its constituent parts\n     are defined in XML Schema: Part 2: Datatypes -->\n<!ENTITY % xs-datatypes PUBLIC ''datatypes'' ''datatypes.dtd'' >\n\n<!ENTITY % p ''xs:''> <!-- can be overriden in the internal subset of a\n                         schema document to establish a different\n                         namespace prefix -->\n<!ENTITY % s '':xs''> <!-- if %p is defined (e.g. as foo:) then you must\n                         also define %s as the suffix for the appropriate\n                         namespace declaration (e.g. :foo) -->\n<!ENTITY % nds ''xmlns%s;''>\n\n<!-- Define all the element names, with optional prefix -->\n<!ENTITY % schema \"%p;schema\">\n<!ENTITY % complexType \"%p;complexType\">\n<!ENTITY % complexContent \"%p;complexContent\">\n<!ENTITY % simpleContent \"%p;simpleContent\">\n<!ENTITY % extension \"%p;extension\">\n<!ENTITY % element \"%p;element\">\n<!ENTITY % unique \"%p;unique\">\n<!ENTITY % key \"%p;key\">\n<!ENTITY % keyref \"%p;keyref\">\n<!ENTITY % selector \"%p;selector\">\n<!ENTITY % field \"%p;field\">\n<!ENTITY % group \"%p;group\">\n<!ENTITY % all \"%p;all\">\n<!ENTITY % choice \"%p;choice\">\n<!ENTITY % sequence \"%p;sequence\">\n<!ENTITY % any \"%p;any\">\n<!ENTITY % anyAttribute \"%p;anyAttribute\">\n<!ENTITY % attribute \"%p;attribute\">\n<!ENTITY % attributeGroup \"%p;attributeGroup\">\n<!ENTITY % include \"%p;include\">\n<!ENTITY % import \"%p;import\">\n<!ENTITY % redefine \"%p;redefine\">\n<!ENTITY % notation \"%p;notation\">\n\n<!-- annotation elements -->\n<!ENTITY % annotation \"%p;annotation\">\n<!ENTITY % appinfo \"%p;appinfo\">\n<!ENTITY % documentation \"%p;documentation\">\n\n<!-- Customisation entities for the ATTLIST of each element type.\n     Define one of these if your schema takes advantage of the\n     anyAttribute=''##other'' in the schema for schemas -->\n\n<!ENTITY % schemaAttrs ''''>\n<!ENTITY % complexTypeAttrs ''''>\n<!ENTITY % complexContentAttrs ''''>\n<!ENTITY % simpleContentAttrs ''''>\n<!ENTITY % extensionAttrs ''''>\n<!ENTITY % elementAttrs ''''>\n<!ENTITY % groupAttrs ''''>\n<!ENTITY % allAttrs ''''>\n<!ENTITY % choiceAttrs ''''>\n<!ENTITY % sequenceAttrs ''''>\n<!ENTITY % anyAttrs ''''>\n<!ENTITY % anyAttributeAttrs ''''>\n<!ENTITY % attributeAttrs ''''>\n<!ENTITY % attributeGroupAttrs ''''>\n<!ENTITY % uniqueAttrs ''''>\n<!ENTITY % keyAttrs ''''>\n<!ENTITY % keyrefAttrs ''''>\n<!ENTITY % selectorAttrs ''''>\n<!ENTITY % fieldAttrs ''''>\n<!ENTITY % includeAttrs ''''>\n<!ENTITY % importAttrs ''''>\n<!ENTITY % redefineAttrs ''''>\n<!ENTITY % notationAttrs ''''>\n<!ENTITY % annotationAttrs ''''>\n<!ENTITY % appinfoAttrs ''''>\n<!ENTITY % documentationAttrs ''''>\n\n<!ENTITY % complexDerivationSet \"CDATA\">\n      <!-- #all or space-separated list drawn from derivationChoice -->\n<!ENTITY % blockSet \"CDATA\">\n      <!-- #all or space-separated list drawn from\n                      derivationChoice + ''substitution'' -->\n\n<!ENTITY % mgs ''%all; | %choice; | %sequence;''>\n<!ENTITY % cs ''%choice; | %sequence;''>\n<!ENTITY % formValues ''(qualified|unqualified)''>\n\n\n<!ENTITY % attrDecls    ''((%attribute;| %attributeGroup;)*,(%anyAttribute;)?)''>\n\n<!ENTITY % particleAndAttrs ''((%mgs; | %group;)?, %attrDecls;)''>\n\n<!-- This is used in part2 -->\n<!ENTITY % restriction1 ''((%mgs; | %group;)?)''>\n\n%xs-datatypes;\n\n<!-- the duplication below is to produce an unambiguous content model\n     which allows annotation everywhere -->\n<!ELEMENT %schema; ((%include; | %import; | %redefine; | %annotation;)*,\n                    ((%simpleType; | %complexType;\n                      | %element; | %attribute;\n                      | %attributeGroup; | %group;\n                      | %notation; ),\n                     (%annotation;)*)* )>\n<!ATTLIST %schema;\n   targetNamespace      %URIref;               #IMPLIED\n   version              CDATA                  #IMPLIED\n   %nds;                %URIref;               #FIXED ''http://www.w3.org/2001/XMLSchema''\n   xmlns                CDATA                  #IMPLIED\n   finalDefault         %complexDerivationSet; ''''\n   blockDefault         %blockSet;             ''''\n   id                   ID                     #IMPLIED\n   elementFormDefault   %formValues;           ''unqualified''\n   attributeFormDefault %formValues;           ''unqualified''\n   xml:lang             CDATA                  #IMPLIED\n   %schemaAttrs;>\n<!-- Note the xmlns declaration is NOT in the Schema for Schemas,\n     because at the Infoset level where schemas operate,\n     xmlns(:prefix) is NOT an attribute! -->\n<!-- The declaration of xmlns is a convenience for schema authors -->\n \n<!-- The id attribute here and below is for use in external references\n     from non-schemas using simple fragment identifiers.\n     It is NOT used for schema-to-schema reference, internal or\n     external. -->\n\n<!-- a type is a named content type specification which allows attribute\n     declarations-->\n<!-- -->\n\n<!ELEMENT %complexType; ((%annotation;)?,\n                         (%simpleContent;|%complexContent;|\n                          %particleAndAttrs;))>\n\n<!ATTLIST %complexType;\n          name      %NCName;                        #IMPLIED\n          id        ID                              #IMPLIED\n          abstract  %boolean;                       #IMPLIED\n          final     %complexDerivationSet;          #IMPLIED\n          block     %complexDerivationSet;          #IMPLIED\n          mixed (true|false) ''false''\n          %complexTypeAttrs;>\n\n<!-- particleAndAttrs is shorthand for a root type -->\n<!-- mixed is disallowed if simpleContent, overriden if complexContent\n     has one too. -->\n\n<!-- If anyAttribute appears in one or more referenced attributeGroups\n     and/or explicitly, the intersection of the permissions is used -->\n\n<!ELEMENT %complexContent; ((%annotation;)?, (%restriction;|%extension;))>\n<!ATTLIST %complexContent;\n          mixed (true|false) #IMPLIED\n          id    ID           #IMPLIED\n          %complexContentAttrs;>\n\n<!-- restriction should use the branch defined above, not the simple\n     one from part2; extension should use the full model  -->\n\n<!ELEMENT %simpleContent; ((%annotation;)?, (%restriction;|%extension;))>\n<!ATTLIST %simpleContent;\n          id    ID           #IMPLIED\n          %simpleContentAttrs;>\n\n<!-- restriction should use the simple branch from part2, not the \n     one defined above; extension should have no particle  -->\n\n<!ELEMENT %extension; ((%annotation;)?, (%particleAndAttrs;))>\n<!ATTLIST %extension;\n          base  %QName;      #REQUIRED\n          id    ID           #IMPLIED\n          %extensionAttrs;>\n\n<!-- an element is declared by either:\n a name and a type (either nested or referenced via the type attribute)\n or a ref to an existing element declaration -->\n\n<!ELEMENT %element; ((%annotation;)?, (%complexType;| %simpleType;)?,\n                     (%unique; | %key; | %keyref;)*)>\n<!-- simpleType or complexType only if no type|ref attribute -->\n<!-- ref not allowed at top level -->\n<!ATTLIST %element;\n            name               %NCName;               #IMPLIED\n            id                 ID                     #IMPLIED\n            ref                %QName;                #IMPLIED\n            type               %QName;                #IMPLIED\n            minOccurs          %nonNegativeInteger;   #IMPLIED\n            maxOccurs          CDATA                  #IMPLIED\n            nillable           %boolean;              #IMPLIED\n            substitutionGroup  %QName;                #IMPLIED\n            abstract           %boolean;              #IMPLIED\n            final              %complexDerivationSet; #IMPLIED\n            block              %blockSet;             #IMPLIED\n            default            CDATA                  #IMPLIED\n            fixed              CDATA                  #IMPLIED\n            form               %formValues;           #IMPLIED\n            %elementAttrs;>\n<!-- type and ref are mutually exclusive.\n     name and ref are mutually exclusive, one is required -->\n<!-- In the absence of type AND ref, type defaults to type of\n     substitutionGroup, if any, else the ur-type, i.e. unconstrained -->\n<!-- default and fixed are mutually exclusive -->\n\n<!ELEMENT %group; ((%annotation;)?,(%mgs;)?)>\n<!ATTLIST %group; \n          name        %NCName;               #IMPLIED\n          ref         %QName;                #IMPLIED\n          minOccurs   %nonNegativeInteger;   #IMPLIED\n          maxOccurs   CDATA                  #IMPLIED\n          id          ID                     #IMPLIED\n          %groupAttrs;>\n\n<!ELEMENT %all; ((%annotation;)?, (%element;)*)>\n<!ATTLIST %all;\n          minOccurs   (1)                    #IMPLIED\n          maxOccurs   (1)                    #IMPLIED\n          id          ID                     #IMPLIED\n          %allAttrs;>\n\n<!ELEMENT %choice; ((%annotation;)?, (%element;| %group;| %cs; | %any;)*)>\n<!ATTLIST %choice;\n          minOccurs   %nonNegativeInteger;   #IMPLIED\n          maxOccurs   CDATA                  #IMPLIED\n          id          ID                     #IMPLIED\n          %choiceAttrs;>\n\n<!ELEMENT %sequence; ((%annotation;)?, (%element;| %group;| %cs; | %any;)*)>\n<!ATTLIST %sequence;\n          minOccurs   %nonNegativeInteger;   #IMPLIED\n          maxOccurs   CDATA                  #IMPLIED\n          id          ID                     #IMPLIED\n          %sequenceAttrs;>\n\n<!-- an anonymous grouping in a model, or\n     a top-level named group definition, or a reference to same -->\n\n<!-- Note that if order is ''all'', group is not allowed inside.\n     If order is ''all'' THIS group must be alone (or referenced alone) at\n     the top level of a content model -->\n<!-- If order is ''all'', minOccurs==maxOccurs==1 on element/any inside -->\n<!-- Should allow minOccurs=0 inside order=''all'' . . . -->\n\n<!ELEMENT %any; (%annotation;)?>\n<!ATTLIST %any;\n            namespace       CDATA                  ''##any''\n            processContents (skip|lax|strict)      ''strict''\n            minOccurs       %nonNegativeInteger;   ''1''\n            maxOccurs       CDATA                  ''1''\n            id              ID                     #IMPLIED\n            %anyAttrs;>\n\n<!-- namespace is interpreted as follows:\n                  ##any      - - any non-conflicting WFXML at all\n\n                  ##other    - - any non-conflicting WFXML from namespace other\n                                  than targetNamespace\n\n                  ##local    - - any unqualified non-conflicting WFXML/attribute\n                  one or     - - any non-conflicting WFXML from\n                  more URI        the listed namespaces\n                  references\n\n                  ##targetNamespace ##local may appear in the above list,\n                    with the obvious meaning -->\n\n<!ELEMENT %anyAttribute; (%annotation;)?>\n<!ATTLIST %anyAttribute;\n            namespace       CDATA              ''##any''\n            processContents (skip|lax|strict)  ''strict''\n            id              ID                 #IMPLIED\n            %anyAttributeAttrs;>\n<!-- namespace is interpreted as for ''any'' above -->\n\n<!-- simpleType only if no type|ref attribute -->\n<!-- ref not allowed at top level, name iff at top level -->\n<!ELEMENT %attribute; ((%annotation;)?, (%simpleType;)?)>\n<!ATTLIST %attribute;\n          name      %NCName;      #IMPLIED\n          id        ID            #IMPLIED\n          ref       %QName;       #IMPLIED\n          type      %QName;       #IMPLIED\n          use       (prohibited|optional|required) #IMPLIED\n          default   CDATA         #IMPLIED\n          fixed     CDATA         #IMPLIED\n          form      %formValues;  #IMPLIED\n          %attributeAttrs;>\n<!-- type and ref are mutually exclusive.\n     name and ref are mutually exclusive, one is required -->\n<!-- default for use is optional when nested, none otherwise -->\n<!-- default and fixed are mutually exclusive -->\n<!-- type attr and simpleType content are mutually exclusive -->\n\n<!-- an attributeGroup is a named collection of attribute decls, or a\n     reference thereto -->\n<!ELEMENT %attributeGroup; ((%annotation;)?,\n                       (%attribute; | %attributeGroup;)*,\n                       (%anyAttribute;)?) >\n<!ATTLIST %attributeGroup;\n                 name       %NCName;       #IMPLIED\n                 id         ID             #IMPLIED\n                 ref        %QName;        #IMPLIED\n                 %attributeGroupAttrs;>\n\n<!-- ref iff no content, no name.  ref iff not top level -->\n\n<!-- better reference mechanisms -->\n<!ELEMENT %unique; ((%annotation;)?, %selector;, (%field;)+)>\n<!ATTLIST %unique;\n          name     %NCName;       #REQUIRED\n	  id       ID             #IMPLIED\n	  %uniqueAttrs;>\n\n<!ELEMENT %key;    ((%annotation;)?, %selector;, (%field;)+)>\n<!ATTLIST %key;\n          name     %NCName;       #REQUIRED\n	  id       ID             #IMPLIED\n	  %keyAttrs;>\n\n<!ELEMENT %keyref; ((%annotation;)?, %selector;, (%field;)+)>\n<!ATTLIST %keyref;\n          name     %NCName;       #REQUIRED\n	  refer    %QName;        #REQUIRED\n	  id       ID             #IMPLIED\n	  %keyrefAttrs;>\n\n<!ELEMENT %selector; ((%annotation;)?)>\n<!ATTLIST %selector;\n          xpath %XPathExpr; #REQUIRED\n          id    ID          #IMPLIED\n          %selectorAttrs;>\n<!ELEMENT %field; ((%annotation;)?)>\n<!ATTLIST %field;\n          xpath %XPathExpr; #REQUIRED\n          id    ID          #IMPLIED\n          %fieldAttrs;>\n\n<!-- Schema combination mechanisms -->\n<!ELEMENT %include; (%annotation;)?>\n<!ATTLIST %include;\n          schemaLocation %URIref; #REQUIRED\n          id             ID       #IMPLIED\n          %includeAttrs;>\n\n<!ELEMENT %import; (%annotation;)?>\n<!ATTLIST %import;\n          namespace      %URIref; #IMPLIED\n          schemaLocation %URIref; #IMPLIED\n          id             ID       #IMPLIED\n          %importAttrs;>\n\n<!ELEMENT %redefine; (%annotation; | %simpleType; | %complexType; |\n                      %attributeGroup; | %group;)*>\n<!ATTLIST %redefine;\n          schemaLocation %URIref; #REQUIRED\n          id             ID       #IMPLIED\n          %redefineAttrs;>\n\n<!ELEMENT %notation; (%annotation;)?>\n<!ATTLIST %notation;\n	  name        %NCName;    #REQUIRED\n	  id          ID          #IMPLIED\n	  public      CDATA       #REQUIRED\n	  system      %URIref;    #IMPLIED\n	  %notationAttrs;>\n\n<!-- Annotation is either application information or documentation -->\n<!-- By having these here they are available for datatypes as well\n     as all the structures elements -->\n\n<!ELEMENT %annotation; (%appinfo; | %documentation;)*>\n<!ATTLIST %annotation; %annotationAttrs;>\n\n<!-- User must define annotation elements in internal subset for this\n     to work -->\n<!ELEMENT %appinfo; ANY>   <!-- too restrictive -->\n<!ATTLIST %appinfo;\n          source     %URIref;      #IMPLIED\n          id         ID         #IMPLIED\n          %appinfoAttrs;>\n<!ELEMENT %documentation; ANY>   <!-- too restrictive -->\n<!ATTLIST %documentation;\n          source     %URIref;   #IMPLIED\n          id         ID         #IMPLIED\n          xml:lang   CDATA      #IMPLIED\n          %documentationAttrs;>\n\n<!NOTATION XMLSchemaStructures PUBLIC\n           ''structures'' ''http://www.w3.org/2001/XMLSchema.xsd'' >\n<!NOTATION XML PUBLIC\n           ''REC-xml-1998-0210'' ''http://www.w3.org/TR/1998/REC-xml-19980210'' >\n','-//W3C//DTD XMLSCHEMA 200102//EN',NULL);

INSERT INTO rbac_role VALUES (-100,0,'Administrator','ADMIN',null,null,null,'Users assigned to the {0} role have full access to the gateway.',0);
INSERT INTO rbac_permission VALUES (-101, 0, -100, 'CREATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-102, 0, -100, 'READ',   null, 'ANY');
INSERT INTO rbac_permission VALUES (-103, 0, -100, 'UPDATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-104, 0, -100, 'DELETE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-105, 0, -100, 'OTHER', 'log-viewer', 'LOG_SINK');
-- Create Operator role
INSERT INTO rbac_role VALUES (-150,0,'Operator',null,null,null,null,'Users assigned to the {0} role have read only access to the gateway.',0);
INSERT INTO rbac_permission VALUES (-151, 0, -150, 'READ', null, 'ANY');
INSERT INTO rbac_permission VALUES (-152, 0, -150, 'OTHER', 'log-viewer', 'LOG_SINK');

-- Create other canned roles
INSERT INTO rbac_role VALUES (-200,0,'Manage Internal Users and Groups', null,null,null,null, 'Users assigned to the {0} role have the ability to create, read, update and delete users and groups in the internal identity provider.',0);
INSERT INTO rbac_permission VALUES (-201,0,-200,'READ',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-202,0,-201);
INSERT INTO rbac_predicate_attribute VALUES (-202,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-203,0,-200,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-204,0,-203);
INSERT INTO rbac_predicate_oid VALUES (-204,'-2');
INSERT INTO rbac_permission VALUES (-205,0,-200,'UPDATE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-206,0,-205);
INSERT INTO rbac_predicate_attribute VALUES (-206,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-207,0,-200,'READ',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-208,0,-207);
INSERT INTO rbac_predicate_attribute VALUES (-208,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-209,0,-200,'DELETE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-210,0,-209);
INSERT INTO rbac_predicate_attribute VALUES (-210,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-211,0,-200,'CREATE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-212,0,-211);
INSERT INTO rbac_predicate_attribute VALUES (-212,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-213,0,-200,'CREATE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-214,0,-213);
INSERT INTO rbac_predicate_attribute VALUES (-214,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-215,0,-200,'DELETE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-216,0,-215);
INSERT INTO rbac_predicate_attribute VALUES (-216,'providerId','-2','eq');
INSERT INTO rbac_permission VALUES (-217,0,-200,'UPDATE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-218,0,-217);
INSERT INTO rbac_predicate_attribute VALUES (-218,'providerId','-2','eq');

INSERT INTO rbac_role VALUES (-250,0,'Publish External Identity Providers', null,null,null,null, 'Users assigned to the {0} role have the ability to create new external identity providers.',0);
INSERT INTO rbac_permission VALUES (-251,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-252,0,-251);
INSERT INTO rbac_predicate_attribute VALUES (-252,'typeVal','2','eq');
INSERT INTO rbac_permission VALUES (-253,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-254,0,-253);
INSERT INTO rbac_predicate_attribute VALUES (-254,'typeVal','3','eq');
INSERT INTO rbac_permission VALUES (-258,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-259,0,-258);
INSERT INTO rbac_predicate_attribute VALUES (-259,'typeVal','4','eq');
INSERT INTO rbac_permission VALUES (-255,0,-250,'READ',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-256,0,-250,'READ',NULL,'SSG_KEYSTORE');
INSERT INTO rbac_permission VALUES (-257,0,-250,'READ',NULL,'SSG_KEY_ENTRY');

INSERT INTO rbac_role VALUES (-300,0,'Search Users and Groups', null,null,null,null, 'Users assigned to the {0} role have permission to search and view users and groups in all identity providers.',0);
INSERT INTO rbac_permission VALUES (-301,0,-300,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-302,0,-300,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-303,0,-300,'READ',NULL,'GROUP');

INSERT INTO rbac_role VALUES (-350,0,'Publish Webservices', null,null,null,null, 'Users assigned to the {0} role have the ability to publish new web services.',0);
INSERT INTO rbac_permission VALUES (-351,0,-350,'READ',NULL,'GROUP');
INSERT INTO rbac_permission VALUES (-352,0,-350,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-353,0,-350,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-354,0,-350,'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-355,0,-350,'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission VALUES (-356,0,-350,'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-357,0,-350,'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-358,0,-350,'READ',NULL,'HTTP_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-359,0,-350,'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');

INSERT INTO rbac_role VALUES (-400,1,'Manage Webservices', null,null,null,null, 'Users assigned to the {0} role have the ability to publish new services and edit existing ones.',0);
INSERT INTO rbac_permission VALUES (-401,0,-400,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-402,0,-400,'READ',NULL,'GROUP');
INSERT INTO rbac_permission VALUES (-403,0,-400,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-404,0,-400,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-405,0,-400,'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-406,0,-400,'UPDATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-407,0,-400,'DELETE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-408,0,-400,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-409,0,-400,'READ',NULL,'SERVICE_USAGE');
INSERT INTO rbac_permission VALUES (-410,0,-400,'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission VALUES (-411,0,-400,'READ',NULL,'AUDIT_MESSAGE');
INSERT INTO rbac_permission VALUES (-412,0,-400,'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-413,0,-400,'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-414,0,-400,'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission VALUES (-415,0,-400,'READ',NULL,'POLICY');
INSERT INTO rbac_permission VALUES (-416,0,-400,'UPDATE',NULL,'POLICY');
INSERT INTO rbac_permission VALUES (-417,0,-400,'CREATE',NULL,'FOLDER');
INSERT INTO rbac_permission VALUES (-418,0,-400,'READ',  NULL,'FOLDER');
INSERT INTO rbac_permission VALUES (-419,0,-400,'UPDATE',NULL,'FOLDER');
INSERT INTO rbac_permission VALUES (-420,0,-400,'DELETE',NULL,'FOLDER');
INSERT INTO rbac_permission VALUES (-421,0,-400,'CREATE',NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission VALUES (-422,0,-400,'READ',  NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission VALUES (-423,0,-400,'UPDATE',NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission VALUES (-424,0,-400,'DELETE',NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission VALUES (-425,0,-400,'CREATE',NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission VALUES (-426,0,-400,'READ',  NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission VALUES (-427,0,-400,'UPDATE',NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission VALUES (-428,0,-400,'DELETE',NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission VALUES (-429,0,-400,'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-430,0,-400,'READ',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-431,0,-400,'UPDATE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-432,0,-400,'DELETE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-433,0,-400,'CREATE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-434,0,-400,'READ',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-435,0,-400,'UPDATE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-436,0,-400,'DELETE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-437,0,-400,'CREATE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-438,0,-400,'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-439,0,-400,'READ',NULL,'HTTP_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-440,0,-400,'READ',NULL,'ENCAPSULATED_ASSERTION');
-- TODO Leaving hole here for fix to FR-473 mess-up (missing CREATE POLICY permission that is in the MySQL ssg.sql)
INSERT INTO rbac_permission VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

INSERT INTO rbac_role VALUES (-450,0,'View Audit Records', null,null,null,null, 'Users assigned to the {0} role have the ability to view audits in the manager.',0);
INSERT INTO rbac_permission VALUES (-451,0,-450,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-452,0,-450,'READ',NULL,'AUDIT_RECORD');

INSERT INTO rbac_role VALUES (-500,0,'View Service Metrics', null,null,null,null, 'Users assigned to the {0} role have the ability to monitor service metrics in the manager.',0);
INSERT INTO rbac_permission VALUES (-501,0,-500,'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission VALUES (-502,0,-500,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-503,0,-500,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-504,0,-500,'READ',NULL,'SERVICE_USAGE');
INSERT INTO rbac_permission VALUES (-505,0,-500,'READ',NULL,'FOLDER');

INSERT INTO rbac_role VALUES (-550,0,'Manage Cluster Status', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster status information.',0);
INSERT INTO rbac_permission VALUES (-551,0,-550,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-552,0,-550,'UPDATE',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-553,0,-550,'DELETE',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-554,0,-550,'READ',NULL,'METRICS_BIN');

INSERT INTO rbac_role VALUES (-600,0,'Manage Certificates (truststore)', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete trusted certificates and policies for revocation checking.',0);
INSERT INTO rbac_permission VALUES (-601,0,-600,'UPDATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-602,0,-600,'READ',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-603,0,-600,'DELETE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-604,0,-600,'CREATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-605,0,-600,'UPDATE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-606,0,-600,'READ',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-607,0,-600,'DELETE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-608,0,-600,'CREATE',NULL,'REVOCATION_CHECK_POLICY');

INSERT INTO rbac_role VALUES (-650,0,'Manage JMS Connections', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JMS connections.',0);
INSERT INTO rbac_permission VALUES (-651,1,-650,'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-652,1,-650,'DELETE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-653,1,-650,'CREATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-654,1,-650,'UPDATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-655,1,-650,'CREATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-656,1,-650,'DELETE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-657,1,-650,'UPDATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-658,1,-650,'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-659,1,-650,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-660,1,-650,'READ',NULL,'SSG_KEYSTORE');
INSERT INTO rbac_permission VALUES (-661,1,-650,'READ',NULL,'SSG_KEY_ENTRY');

INSERT INTO rbac_role VALUES (-700,0,'Manage Cluster Properties', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster properties.',0);
INSERT INTO rbac_permission VALUES (-701,0,-700,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-702,0,-700,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-703,0,-700,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-704,0,-700,'DELETE',NULL,'CLUSTER_PROPERTY');

INSERT INTO rbac_role VALUES (-750,0,'Manage Listen Ports', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete Gateway listen ports (HTTP(S) and FTP(S)) and to list published services.',0);
INSERT INTO rbac_permission VALUES (-751,0,-750,'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-752,0,-750,'CREATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-753,0,-750,'UPDATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-754,0,-750,'DELETE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-755,0,-750,'READ',NULL,'SERVICE');

INSERT INTO rbac_role VALUES (-800,0,'Manage Log Sinks', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete log sinks.',0);
INSERT INTO rbac_permission VALUES (-801,0,-800,'READ',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-802,0,-800,'CREATE',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-803,0,-800,'UPDATE',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-804,0,-800,'DELETE',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-805,0,-800,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-806,0,-800,'OTHER','log-viewer','LOG_SINK');
INSERT INTO rbac_permission VALUES (-807,0,-800,'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-808,0,-800,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-809,0,-800,'READ',NULL,'FOLDER');
INSERT INTO rbac_permission VALUES (-810,0,-800,'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-811,0,-800,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-812,0,-800,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-813,0,-800,'READ',NULL,'POLICY');
INSERT INTO rbac_permission VALUES (-814,0,-800,'READ',NULL,'EMAIL_LISTENER');

INSERT INTO rbac_role VALUES (-850,0,'Gateway Maintenance', null,null,null,null, 'Users assigned to the {0} role have the ability to perform Gateway maintenance tasks.',0);
INSERT INTO rbac_permission VALUES (-851,0,-850,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-852,0,-851);
INSERT INTO rbac_predicate_attribute VALUES (-852,'name','audit.archiver.ftp.config','eq');
INSERT INTO rbac_permission VALUES (-853,0,-850,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-854,0,-853);
INSERT INTO rbac_predicate_attribute VALUES (-854,'name','audit.archiver.ftp.config','eq');
INSERT INTO rbac_permission VALUES (-855,0,-850,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-856,0,-855);
INSERT INTO rbac_predicate_attribute VALUES (-856,'name','audit.archiver.ftp.config','eq');
INSERT INTO rbac_permission VALUES (-857,0,-850,'DELETE',NULL,'AUDIT_RECORD');
-- No predicates implies all entities

INSERT INTO rbac_role VALUES (-900,0,'Manage Email Listeners', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete email listeners.',0);
INSERT INTO rbac_permission VALUES (-901,0,-900,'READ',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission VALUES (-902,0,-900,'CREATE',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission VALUES (-903,0,-900,'UPDATE',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission VALUES (-904,0,-900,'DELETE',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission VALUES (-905,0,-900,'READ',NULL,'SERVICE');

INSERT INTO rbac_role VALUES (-950,0,'Manage JDBC Connections', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JDBC connections.',0);
INSERT INTO rbac_permission VALUES (-951,0,-950,'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-952,0,-950,'CREATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-953,0,-950,'UPDATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-954,0,-950,'DELETE',NULL,'JDBC_CONNECTION');

INSERT INTO rbac_role VALUES (-1000,0,'Manage UDDI Registries', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete UDDI Registry connections.',0);
INSERT INTO rbac_permission VALUES (-1001,0,-1000,'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1002,0,-1000,'CREATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1003,0,-1000,'UPDATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1004,0,-1000,'DELETE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1005,0,-1000,'READ',NULL,'SERVICE');

INSERT INTO rbac_role VALUES (-1050,0,'Manage Secure Passwords', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete any stored password.',0);
INSERT INTO rbac_permission VALUES (-1051,0,-1050,'READ',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission VALUES (-1052,0,-1050,'CREATE',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission VALUES (-1053,0,-1050,'UPDATE',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission VALUES (-1054,0,-1050,'DELETE',NULL,'SECURE_PASSWORD');

INSERT INTO rbac_role VALUES (-1100,1,'Manage Private Keys',NULL,null,NULL,NULL,'Users in this role have the ability to read, create, update, and delete private keys, as well as the ability to change the designated special-purpose keys (eg, the SSL or CA key).',0);
INSERT INTO rbac_permission VALUES
    (-1101,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1102,0,-1100,'DELETE',NULL,'SSG_KEY_ENTRY'),
    (-1103,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),
    (-1104,0,-1100,'READ',NULL,'SSG_KEY_ENTRY'),
    (-1105,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),
    (-1106,0,-1100,'UPDATE',NULL,'SSG_KEY_ENTRY'),
    (-1107,0,-1100,'CREATE',NULL,'SSG_KEY_ENTRY'),
    (-1108,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1109,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1110,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1111,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1112,0,-1100,'UPDATE',NULL,'SSG_KEYSTORE'),
    (-1113,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1114,0,-1100,'READ',NULL,'SSG_KEYSTORE'),
    (-1115,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1116,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1117,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1118,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),
    (-1119,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1120,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1121,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1122,0,-1100,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES
    (-1101,0,-1101),
    (-1103,0,-1103),
    (-1105,0,-1105),
    (-1108,0,-1108),
    (-1109,0,-1109),
    (-1110,0,-1110),
    (-1111,0,-1111),
    (-1113,0,-1113),
    (-1115,0,-1115),
    (-1116,0,-1116),
    (-1117,0,-1117),
    (-1118,0,-1118),
    (-1119,0,-1119),
    (-1120,0,-1120),
    (-1121,0,-1121),
    (-1122,0,-1122);
INSERT INTO rbac_predicate_attribute VALUES
    (-1101,'name','keyStore.defaultSsl.alias','eq'),
    (-1103,'name','keyStore.defaultCa.alias','eq'),
    (-1105,'name','keyStore.defaultSsl.alias','eq'),
    (-1108,'name','keyStore.defaultCa.alias','eq'),
    (-1109,'name','keyStore.defaultSsl.alias','eq'),
    (-1110,'name','keyStore.defaultCa.alias','eq'),
    (-1111,'name','keyStore.defaultSsl.alias','eq'),
    (-1113,'name','keyStore.defaultCa.alias','eq'),
    (-1115,'name','keyStore.auditViewer.alias','eq'),
    (-1116,'name','keyStore.auditViewer.alias','eq'),
    (-1117,'name','keyStore.auditViewer.alias','eq'),
    (-1118,'name','keyStore.auditViewer.alias','eq'),
    (-1119,'name','keyStore.auditSigning.alias','eq'),
    (-1120,'name','keyStore.auditSigning.alias','eq'),
    (-1121,'name','keyStore.auditSigning.alias','eq'),
    (-1122,'name','keyStore.auditSigning.alias','eq');

INSERT INTO rbac_role VALUES (-1150,0,'Manage Password Policies', null,null,null,null, 'Users assigned to the {0} role have the ability to read and update any stored password policy and view the identity providers.',0);
INSERT INTO rbac_permission VALUES (-1151,0,-1150,'READ',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1052,0,-1050,'CREATE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission VALUES (-1153,0,-1150,'UPDATE',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1054,0,-1050,'DELETE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission VALUES (-1155,0,-1150,'READ',NULL,'ID_PROVIDER_CONFIG');

--
-- New role to invoke the audit viewer policy. Requires READ on audits to be able to open the audit viewer.
--
INSERT INTO rbac_role VALUES (-1200,0,'Invoke Audit Viewer Policy', null,null,null,null, 'Allow the INTERNAL audit-viewer policy to be invoked for an audited message (request / response or detail)',0);
INSERT INTO rbac_permission VALUES (-1201,0,-1200,'OTHER','audit-viewer policy', 'AUDIT_RECORD');
INSERT INTO rbac_permission VALUES (-1202,0,-1200,'READ',NULL,'AUDIT_RECORD');
INSERT INTO rbac_permission VALUES (-1203,0,-1200,'READ',NULL,'CLUSTER_INFO');

INSERT INTO rbac_role VALUES (-1250,0,'Manage Administrative Accounts Configuration', null,null,null,null, 'Users assigned to the {0} role have the ability to create/read/update cluster properties applicable to administrative accounts configurations.',0);
INSERT INTO rbac_permission VALUES (-1251,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1252,0,-1251);
INSERT INTO rbac_predicate_attribute VALUES (-1252,'name','logon.maxAllowableAttempts','eq');
INSERT INTO rbac_permission VALUES (-1253,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1254,0,-1253);
INSERT INTO rbac_predicate_attribute VALUES (-1254,'name','logon.lockoutTime','eq');
INSERT INTO rbac_permission VALUES (-1255,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1256,0,-1255);
INSERT INTO rbac_predicate_attribute VALUES (-1256,'name','logon.sessionExpiry','eq');
INSERT INTO rbac_permission VALUES (-1257,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1258,0,-1257);
INSERT INTO rbac_predicate_attribute VALUES (-1258,'name','logon.inactivityPeriod','eq');
INSERT INTO rbac_permission VALUES (-1259,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1260,0,-1259);
INSERT INTO rbac_predicate_attribute VALUES (-1260,'name','logon.maxAllowableAttempts','eq');
INSERT INTO rbac_permission VALUES (-1261,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1262,0,-1261);
INSERT INTO rbac_predicate_attribute VALUES (-1262,'name','logon.lockoutTime','eq');
INSERT INTO rbac_permission VALUES (-1263,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1264,0,-1263);
INSERT INTO rbac_predicate_attribute VALUES (-1264,'name','logon.sessionExpiry','eq');
INSERT INTO rbac_permission VALUES (-1265,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1266,0,-1265);
INSERT INTO rbac_predicate_attribute VALUES (-1266,'name','logon.inactivityPeriod','eq');
INSERT INTO rbac_permission VALUES (-1267,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1268,0,-1267);
INSERT INTO rbac_predicate_attribute VALUES (-1268,'name','logon.maxAllowableAttempts','eq');
INSERT INTO rbac_permission VALUES (-1269,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1270,0,-1269);
INSERT INTO rbac_predicate_attribute VALUES (-1270,'name','logon.lockoutTime','eq');
INSERT INTO rbac_permission VALUES (-1271,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1272,0,-1271);
INSERT INTO rbac_predicate_attribute VALUES (-1272,'name','logon.sessionExpiry','eq');
INSERT INTO rbac_permission VALUES (-1273,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1274,0,-1273);
INSERT INTO rbac_predicate_attribute VALUES (-1274,'name','logon.inactivityPeriod','eq');

--
-- New role for viewing the default log (oid = -810)
--
-- NOTE: This is an entity specific role and will be deleted if the default log
-- sink is removed.
--
INSERT INTO rbac_role VALUES (-1300,0,'View ssg Log Sink (#-1,300)',null,'LOG_SINK',-810,null, 'Users assigned to the {0} role have the ability to read the log sink and any associated log files.',0);
INSERT INTO rbac_permission VALUES (-1301,0,-1300,'READ',NULL,'LOG_SINK');
INSERT INTO rbac_predicate VALUES (-1301,0,-1301);
INSERT INTO rbac_predicate_oid VALUES (-1301,'-810');
INSERT INTO rbac_permission VALUES (-1302,0,-1300,'READ',NULL,'CLUSTER_INFO');

INSERT INTO rbac_permission VALUES (-1303,0,-1300,'OTHER','log-viewer','LOG_SINK');

INSERT INTO rbac_role VALUES (-1350,0,'Manage Encapsulated Assertions', null,'ENCAPSULATED_ASSERTION',null,null, 'Users assigned to the {0} role have the ability to create/read/update/delete encapsulated assertions.',0);
INSERT INTO rbac_permission VALUES (-1351,0,-1350,'CREATE',null,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1352,0,-1350,'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1353,0,-1350,'UPDATE',null, 'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1354,0,-1350,'DELETE',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1355,0,-1350,'READ',NULL,'POLICY');
INSERT INTO rbac_predicate VALUES (-1356,0,-1355);
INSERT INTO rbac_predicate_attribute VALUES (-1356,'type','Included Policy Fragment','eq');

INSERT INTO rbac_role VALUES (-1450,0,'Manage Custom Key Value Store', null,'CUSTOM_KEY_VALUE_STORE',null,null, 'Users assigned to the {0} role have the ability to read, create, update, and delete key values from custom key value store.',0);
INSERT INTO rbac_permission VALUES (-1451,0,-1450,'CREATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1452,0,-1450,'READ',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1453,0,-1450,'UPDATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1454,0,-1450,'DELETE',null,'CUSTOM_KEY_VALUE_STORE');

-- Assign Administrator role to existing admin user
INSERT INTO rbac_assignment VALUES (-105, -2, -100, '3', 'User');

INSERT INTO sink_config VALUES (-810,0,'ssg','Main log','FILE',1,'INFO','AUDIT,LOG','<java version="1.6.0" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>file.maxSize</string><string>20000</string></void><void method="put"><string>file.format</string><string>STANDARD</string></void><void method="put"><string>file.logCount</string><string>10</string></void></object></java>',NULL);
INSERT INTO sink_config VALUES (-811,0,'sspc','Process Controller Log','FILE',0,'FINEST','SSPC','<java version="1.6.0" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>file.maxSize</string><string>20000</string></void><void method="put"><string>file.format</string><string>STANDARD</string></void><void method="put"><string>file.logCount</string><string>10</string></void></object></java>',NULL);


--
-- Table for generic (runtime) entity types
--
create table generic_entity (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  name varchar(255),
  description clob(8388607),
  classname varchar(1024) not null,
  enabled smallint default 1,
  value_xml clob(8388607),
  primary key (goid),
  unique (classname, name)
);

--
-- Change "Manage JMS Connections" role to restrict access for both JMS and MQ native destination management.
-- Add permissions to manage SSG Active Connectors of type MqNative.
-- Add read permission for Secure Password management.
--
UPDATE rbac_role SET name='Manage Message Destinations', description='Users assigned to the {0} role have the ability to read, create, update and delete message destinations.' WHERE objectid=-650;
INSERT INTO rbac_permission VALUES (-662,1,-650,'READ',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-663,0,-662);
INSERT INTO rbac_predicate_attribute VALUES (-663,'type','MqNative','eq');
INSERT INTO rbac_permission VALUES (-664,1,-650,'DELETE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-665,0,-664);
INSERT INTO rbac_predicate_attribute VALUES (-665,'type','MqNative','eq');
INSERT INTO rbac_permission VALUES (-666,1,-650,'CREATE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-667,0,-666);
INSERT INTO rbac_predicate_attribute VALUES (-667,'type','MqNative','eq');
INSERT INTO rbac_permission VALUES (-668,1,-650,'UPDATE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-669,0,-668);
INSERT INTO rbac_predicate_attribute VALUES (-669,'type','MqNative','eq');
INSERT INTO rbac_permission VALUES (-670,0,-650,'READ',NULL,'SECURE_PASSWORD');

-- Reserve -700001 objectid for cluster.hostname and insert default
INSERT INTO cluster_properties VALUES (toGoid(0,-700001),'cluster.hostname',0,'',null);

--
-- Encapsulated Assertions
--
CREATE TABLE encapsulated_assertion (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  name varchar(255),
  guid varchar(255) not null unique,
  policy_oid bigint NOT NULL,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  PRIMARY KEY (goid)
);

alter table encapsulated_assertion
    add constraint FK_ENCASS_POL
    foreign key (policy_oid)
    references policy;

CREATE TABLE encapsulated_assertion_property (
  encapsulated_assertion_goid CHAR(16) FOR BIT DATA NOT NULL,
  name varchar(255) NOT NULL,
  value clob(2147483647) NOT NULL
);

alter table encapsulated_assertion_property
    add constraint FK_ENCASSPROP_ENCASS
    foreign key (encapsulated_assertion_goid)
    references encapsulated_assertion
    on delete cascade;

CREATE TABLE encapsulated_assertion_argument (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  encapsulated_assertion_goid CHAR(16) FOR BIT DATA NOT NULL,
  argument_name varchar(255) NOT NULL,
  argument_type varchar(255) NOT NULL,
  gui_prompt smallint NOT NULL,
  gui_label varchar(255),
  ordinal integer NOT NULL,
  PRIMARY KEY (goid)
);

alter table encapsulated_assertion_argument
    add constraint FK_ENCASSARG_ENCASS
    foreign key (encapsulated_assertion_goid)
    references encapsulated_assertion
    on delete cascade;

CREATE TABLE encapsulated_assertion_result (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  encapsulated_assertion_goid CHAR(16) FOR BIT DATA NOT NULL,
  result_name varchar(255) NOT NULL,
  result_type varchar(255) NOT NULL,
  PRIMARY KEY (goid)
);

alter table encapsulated_assertion_result
    add constraint FK_ENCASSRES_ENCASS
    foreign key (encapsulated_assertion_goid)
    references encapsulated_assertion
    on delete cascade;

CREATE TABLE firewall_rule (
  goid CHAR(16) FOR BIT DATA NOT NULL,
  version integer NOT NULL,
  ordinal integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (goid)
);

CREATE TABLE firewall_rule_property (
  firewall_rule_goid CHAR(16) FOR BIT DATA not null,
  name varchar(128) NOT NULL,
  value clob(2147483647) NOT NULL
);

alter table firewall_rule_property
    add constraint FK_FIREWALL_PROPERTY_GOID
    foreign key (firewall_rule_goid)
    references firewall_rule
    on delete cascade;

-- create new RBAC role for Manage Firewall Rules --
INSERT INTO rbac_role (objectid, version, name, entity_type, description, user_created) VALUES (-1400, 0, 'Manage Firewall Rules', 'FIREWALL_RULE', 'Users assigned to the {0} role have the ability to read, create, update and delete Firewall rules.', 0);
INSERT INTO rbac_permission VALUES (-1275,0,-1400,'CREATE',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission VALUES (-1276,0,-1400,'READ',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission VALUES (-1277,0,-1400,'UPDATE',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission VALUES (-1278,0,-1400,'DELETE',NULL,'FIREWALL_RULE');


-- create new RBAC role for Manage SiteMinder Configuration --
INSERT INTO rbac_role (objectid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (-1500,0,'Manage SiteMinder Configuration', null, 'SITEMINDER_CONFIGURATION', null, null, 'Users assigned to the {0} role have the ability to read, create, update and delete SiteMinder configuration.',0);
INSERT INTO rbac_permission VALUES (-1501,0,-1500,'READ',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1502,0,-1500,'CREATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1503,0,-1500,'UPDATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1504,0,-1500,'DELETE',NULL,'SITEMINDER_CONFIGURATION');

--
-- Custom key value store
--
CREATE TABLE custom_key_value_store (
  goid CHAR(16) FOR BIT DATA NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  value blob(2147483647) NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE (name)
);

