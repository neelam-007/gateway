--
-- Script to update derby ssg database from 8.1.02 to 8.2.00
--
-- Layer 7 Technologies, inc
--

-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.3.pre';

-- Update active_connector table
ALTER TABLE active_connector ALTER COLUMN version NOT NULL;
ALTER TABLE active_connector ALTER COLUMN enabled NOT NULL;
ALTER TABLE active_connector ALTER COLUMN type SET DATA TYPE varchar(128);

-- Update active_connector_property table
ALTER TABLE active_connector_property ADD COLUMN value_backup varchar(32672);
UPDATE active_connector_property SET value_backup = value;
ALTER TABLE active_connector_property DROP COLUMN value;
ALTER TABLE active_connector_property ADD COLUMN value clob(2147483647);
UPDATE active_connector_property SET value = value_backup;
ALTER TABLE active_connector_property ALTER COLUMN value NOT NULL;
ALTER TABLE active_connector_property DROP COLUMN value_backup;

-- Update assertion_access table
ALTER TABLE assertion_access ALTER COLUMN version NOT NULL;

-- Update audit_admin table
CREATE INDEX idx_class ON audit_admin(entity_class);
CREATE INDEX idx_oid ON audit_admin(entity_id);

-- Update audit_detail table
CREATE INDEX idx_component_id ON audit_detail(component_id);
ALTER TABLE audit_detail DROP FOREIGN KEY FK97797D35810D4766;
CREATE INDEX idx_audit_goid ON audit_detail(audit_goid);
ALTER TABLE audit_detail ADD CONSTRAINT FK97797D35810D4766 FOREIGN KEY (audit_goid) REFERENCES audit_main ON DELETE CASCADE;

-- Update audit_detail_params table
ALTER TABLE audit_detail_params ALTER COLUMN value NOT NULL;

-- Update audit_main table
ALTER TABLE audit_main ALTER COLUMN ip_address SET DATA TYPE varchar(39);
CREATE INDEX idx_audit_main_time ON audit_main(time);
CREATE INDEX idx_audit_main_ip_address ON audit_main(ip_address);
CREATE INDEX idx_audit_main_provider_goid_user_id ON audit_main(provider_goid, user_id);

-- Update audit_message table
ALTER TABLE audit_message ADD COLUMN status_backup integer;
UPDATE audit_message SET status_backup = status;
ALTER TABLE audit_message DROP COLUMN status;
ALTER TABLE audit_message ADD COLUMN status varchar(32);
UPDATE audit_message SET status = CAST(status_backup as char);
ALTER TABLE audit_message ALTER COLUMN status NOT NULL;
ALTER TABLE audit_message DROP COLUMN status_backup;

CREATE INDEX idx_status ON audit_message(status);
CREATE INDEX idx_request_id ON audit_message(request_id);
CREATE INDEX idx_service_oid ON audit_message(service_goid);

-- Update audit_system table
CREATE INDEX idx_audit_system_component_id ON audit_system(component_id);
CREATE INDEX idx_audit_system_action ON audit_system(action);

-- Update client_cert table
ALTER TABLE client_cert ADD CONSTRAINT fk_client_cert_provider FOREIGN KEY (provider) REFERENCES identity_provider ON DELETE CASCADE;

CREATE INDEX i_subject_dn ON client_cert(subject_dn);
CREATE INDEX i_issuer_dn ON client_cert(issuer_dn);
CREATE INDEX i_thumb ON client_cert(thumbprint_sha1);
CREATE INDEX i_ski ON client_cert(ski);
CREATE UNIQUE INDEX i_identity ON client_cert(provider, user_id);

-- Update cluster_properties table
ALTER TABLE cluster_properties ALTER COLUMN version NOT NULL;

-- Update connector table
ALTER TABLE connector ALTER COLUMN scheme DEFAULT 'http';
ALTER TABLE connector ALTER COLUMN enabled DEFAULT 0;
ALTER TABLE connector ALTER COLUMN secure DEFAULT 0;
ALTER TABLE connector ALTER COLUMN version NOT NULL;
ALTER TABLE connector ALTER COLUMN enabled NOT NULL;
ALTER TABLE connector ALTER COLUMN port NOT NULL;
ALTER TABLE connector ALTER COLUMN secure NOT NULL;

-- changing the client auth column from integer to smallint
ALTER TABLE connector ADD COLUMN client_auth_backup integer;
UPDATE connector SET client_auth_backup = client_auth;
ALTER TABLE connector DROP COLUMN client_auth;
ALTER TABLE connector ADD COLUMN client_auth smallint;
UPDATE connector SET client_auth = client_auth_backup;
ALTER TABLE connector ALTER COLUMN client_auth DEFAULT 0;
ALTER TABLE connector ALTER COLUMN client_auth NOT NULL;
ALTER TABLE connector DROP COLUMN client_auth_backup;

-- increasing size of endpoints column to match larger mysql size
ALTER TABLE connector ALTER COLUMN endpoints SET DATA TYPE varchar(256);

-- Update connector_property table
ALTER TABLE connector_property ADD COLUMN value_backup varchar(32672);
UPDATE connector_property SET value_backup = value;
ALTER TABLE connector_property DROP COLUMN value;
ALTER TABLE connector_property ADD COLUMN value clob(2147483647);
UPDATE connector_property SET value = value_backup;
ALTER TABLE connector_property ALTER COLUMN value NOT NULL;
ALTER TABLE connector_property DROP COLUMN value_backup;

-- Update email_listener table
ALTER TABLE email_listener ALTER COLUMN active DEFAULT 1;
ALTER TABLE email_listener ALTER COLUMN version NOT NULL;

-- Update email_listener_state table
ALTER TABLE email_listener_state ALTER COLUMN version NOT NULL;

-- Update encapsulated_assertion table
ALTER TABLE encapsulated_assertion ALTER COLUMN name NOT NULL;
ALTER TABLE encapsulated_assertion ALTER COLUMN version NOT NULL;

-- Update encapsulated_assertion_argument table
ALTER TABLE encapsulated_assertion_argument ALTER COLUMN version NOT NULL;

-- Update encapsulated_assertion_result table
ALTER TABLE encapsulated_assertion_result ALTER COLUMN version NOT NULL;

-- Update fed_group table
ALTER TABLE fed_group ALTER COLUMN version NOT NULL;

ALTER TABLE fed_group ADD COLUMN description_backup varchar(4096);
UPDATE fed_group SET description_backup = description;
ALTER TABLE fed_group DROP COLUMN description;
ALTER TABLE fed_group ADD COLUMN description CLOB(2147483647);
UPDATE fed_group SET description = description_backup;
ALTER TABLE fed_group DROP COLUMN description_backup;

CREATE UNIQUE INDEX idx_fed_group_name ON fed_group(provider_goid, name);
CREATE INDEX idx_fed_group_provider_goid ON fed_group(provider_goid);

-- Update fed_group_virtual table
ALTER TABLE fed_group_virtual ALTER COLUMN version NOT NULL;

ALTER TABLE fed_group_virtual ADD COLUMN description_backup varchar(4096);
UPDATE fed_group_virtual SET description_backup = description;
ALTER TABLE fed_group_virtual DROP COLUMN description;
ALTER TABLE fed_group_virtual ADD COLUMN description CLOB(2147483647);
UPDATE fed_group_virtual SET description = description_backup;
ALTER TABLE fed_group_virtual DROP COLUMN description_backup;

CREATE UNIQUE INDEX idx_fed_group_virtual_name ON fed_group_virtual(provider_goid, name);
CREATE INDEX idx_fed_group_virtual_provider_goid ON fed_group_virtual(provider_goid);
CREATE INDEX idx_fed_group_virtual_x509_subject_dn_pattern ON fed_group_virtual(x509_subject_dn_pattern);
CREATE INDEX idx_fed_group_virtual_saml_email_pattern ON fed_group_virtual(saml_email_pattern);

-- Update fed_user table
ALTER TABLE fed_user ALTER COLUMN version NOT NULL;
ALTER TABLE fed_user ALTER COLUMN login NULL;
ALTER TABLE fed_user ALTER COLUMN provider_goid NOT NULL;

CREATE UNIQUE INDEX idx_fed_user_name ON fed_user(provider_goid, name);
CREATE INDEX idx_fed_user_provider_goid ON fed_user(provider_goid);
CREATE INDEX idx_fed_user_email ON fed_user(email);
CREATE INDEX idx_fed_user_login ON fed_user(login);
CREATE INDEX idx_fed_user_subject_dn ON fed_user(subject_dn);

-- Update fed_user_group table
-- need to change the order of the primary keys to match mysql
ALTER TABLE fed_user_group DROP PRIMARY KEY;
ALTER TABLE fed_user_group ADD PRIMARY KEY (provider_goid,fed_user_goid,fed_group_goid);

-- Update firewall_rule_property table
-- Increase the size of the firewall rule value column to match the other clob columns
ALTER TABLE firewall_rule_property ADD COLUMN value_backup CLOB(16777215);
UPDATE firewall_rule_property SET value_backup = value;
ALTER TABLE firewall_rule_property DROP COLUMN value;
ALTER TABLE firewall_rule_property ADD COLUMN value CLOB(2147483647);
UPDATE firewall_rule_property SET value = value_backup;
ALTER TABLE firewall_rule_property ALTER COLUMN value NOT NULL;
ALTER TABLE firewall_rule_property DROP COLUMN value_backup;

-- Update folder table
-- match the mysql folder table not null for name
ALTER TABLE folder ALTER COLUMN name NOT NULL;
CREATE UNIQUE INDEX i_name_parent ON folder(name, parent_folder_goid);

-- Update generic_entity table
ALTER TABLE generic_entity ALTER COLUMN version NOT NULL;

ALTER TABLE generic_entity ADD COLUMN description_backup clob(8388607);
UPDATE generic_entity SET description_backup = description;
ALTER TABLE generic_entity DROP COLUMN description;
ALTER TABLE generic_entity ADD COLUMN description clob(2147483647);
UPDATE generic_entity SET description = description_backup;
ALTER TABLE generic_entity DROP COLUMN description_backup;

ALTER TABLE generic_entity ADD COLUMN value_xml_backup clob(8388607);
UPDATE generic_entity SET value_xml_backup = value_xml;
ALTER TABLE generic_entity DROP COLUMN value_xml;
ALTER TABLE generic_entity ADD COLUMN value_xml clob(2147483647);
UPDATE generic_entity SET value_xml = value_xml_backup;
ALTER TABLE generic_entity DROP COLUMN value_xml_backup;

-- force generic_entity classname, name index to be unique
ALTER TABLE generic_entity ADD COLUMN name_backup varchar(255);
UPDATE generic_entity SET name_backup = name;
ALTER TABLE generic_entity DROP COLUMN name;
ALTER TABLE generic_entity ADD COLUMN name varchar(255);
UPDATE generic_entity SET name = name_backup;
ALTER TABLE generic_entity DROP COLUMN name_backup;
CREATE UNIQUE INDEX idx_generic_entity_classname_name ON generic_entity(classname, name);

-- Update http_configuration table
ALTER TABLE http_configuration ALTER COLUMN tls_keystore_goid DEFAULT X'0000000000000000FFFFFFFFFFFFFFFF';
ALTER TABLE http_configuration ALTER COLUMN timeout_connect DEFAULT -1;
ALTER TABLE http_configuration ALTER COLUMN follow_redirects DEFAULT 0;
ALTER TABLE http_configuration ALTER COLUMN port DEFAULT 0;
ALTER TABLE http_configuration ALTER COLUMN proxy_port DEFAULT 0;
ALTER TABLE http_configuration ALTER COLUMN proxy_use DEFAULT 'DEFAULT';
ALTER TABLE http_configuration ALTER COLUMN timeout_read DEFAULT -1;
ALTER TABLE http_configuration ALTER COLUMN tls_key_use DEFAULT 'DEFAULT';

ALTER TABLE http_configuration ALTER COLUMN timeout_connect NOT NULL;
ALTER TABLE http_configuration ALTER COLUMN follow_redirects NOT NULL;
ALTER TABLE http_configuration ALTER COLUMN port NOT NULL;
ALTER TABLE http_configuration ALTER COLUMN proxy_port NOT NULL;
ALTER TABLE http_configuration ALTER COLUMN timeout_read NOT NULL;
ALTER TABLE http_configuration ALTER COLUMN version NOT NULL;

-- Update identity_provider table
ALTER TABLE identity_provider ADD COLUMN type_backup integer;
UPDATE identity_provider SET type_backup = type;
ALTER TABLE identity_provider DROP COLUMN type;
ALTER TABLE identity_provider ADD COLUMN type bigint;
UPDATE identity_provider SET type = type_backup;
ALTER TABLE identity_provider ALTER COLUMN type NOT NULL;
ALTER TABLE identity_provider DROP COLUMN type_backup;

CREATE UNIQUE INDEX ipnm_idx ON identity_provider(name);

-- Update internal_group table
ALTER TABLE internal_group ALTER COLUMN version NOT NULL;

ALTER TABLE internal_group ADD COLUMN description_backup varchar(4096);
UPDATE internal_group SET description_backup = description;
ALTER TABLE internal_group DROP COLUMN description;
ALTER TABLE internal_group ADD COLUMN description CLOB(2147483647);
UPDATE internal_group SET description = description_backup;
ALTER TABLE internal_group DROP COLUMN description_backup;

CREATE UNIQUE INDEX g_idx ON internal_group(name);

-- Update internal_user table
ALTER TABLE internal_user ALTER COLUMN version NOT NULL;
-- mysql has name nullable and defaulting to null
ALTER TABLE internal_user ALTER COLUMN name NULL;
ALTER TABLE internal_user ALTER COLUMN name DEFAULT NULL;
ALTER TABLE internal_user ALTER COLUMN password_expiry DEFAULT 0;

CREATE UNIQUE INDEX l_idx ON internal_user(login);

-- Update internal_user_group table
ALTER TABLE internal_user_group ALTER COLUMN version NOT NULL;
CREATE INDEX idx_internal_user_group_internal_group ON internal_user_group(internal_group);
CREATE INDEX idx_internal_user_group_provider_goid ON internal_user_group(provider_goid);
CREATE INDEX idx_internal_user_group_user_goid ON internal_user_group(user_goid);
CREATE INDEX idx_internal_user_group_subgroup_id ON internal_user_group(subgroup_id);

-- Update jdbc_connection table
ALTER TABLE jdbc_connection ALTER COLUMN enabled DEFAULT 1;
ALTER TABLE jdbc_connection ALTER COLUMN max_pool_size DEFAULT 15;
ALTER TABLE jdbc_connection ALTER COLUMN min_pool_size DEFAULT 3;

ALTER TABLE jdbc_connection ALTER COLUMN version NOT NULL;
ALTER TABLE jdbc_connection ALTER COLUMN enabled NOT NULL;
ALTER TABLE jdbc_connection ALTER COLUMN max_pool_size NOT NULL;
ALTER TABLE jdbc_connection ALTER COLUMN min_pool_size NOT NULL;

CREATE UNIQUE INDEX idx_jdbc_connection_name ON jdbc_connection(name);

-- Update jms_connection table
ALTER TABLE jms_connection ALTER COLUMN destination_factory_url DEFAULT '';
ALTER TABLE jms_connection ALTER COLUMN password DEFAULT '';
ALTER TABLE jms_connection ALTER COLUMN queue_factory_url DEFAULT '';
ALTER TABLE jms_connection ALTER COLUMN topic_factory_url DEFAULT '';
ALTER TABLE jms_connection ALTER COLUMN username DEFAULT '';

ALTER TABLE jms_connection ALTER COLUMN version NOT NULL;
ALTER TABLE jms_connection ALTER COLUMN is_template NOT NULL;

-- Update jms_endpoint table
ALTER TABLE jms_endpoint ALTER COLUMN disabled DEFAULT 0;
ALTER TABLE jms_endpoint ALTER COLUMN max_concurrent_requests DEFAULT 1;
ALTER TABLE jms_endpoint ALTER COLUMN password DEFAULT '';
ALTER TABLE jms_endpoint ALTER COLUMN destination_type DEFAULT 1;
ALTER TABLE jms_endpoint ALTER COLUMN request_max_size DEFAULT -1;
ALTER TABLE jms_endpoint ALTER COLUMN use_message_id_for_correlation DEFAULT 0;
ALTER TABLE jms_endpoint ALTER COLUMN username DEFAULT '';

ALTER TABLE jms_endpoint ALTER COLUMN version NOT NULL;
ALTER TABLE jms_endpoint ALTER COLUMN disabled NOT NULL;
ALTER TABLE jms_endpoint ALTER COLUMN destination_type NOT NULL;
ALTER TABLE jms_endpoint ALTER COLUMN is_template NOT NULL;
ALTER TABLE jms_endpoint ALTER COLUMN use_message_id_for_correlation NOT NULL;

-- Update keystore_file table
ALTER TABLE keystore_file ALTER COLUMN version NOT NULL;
ALTER TABLE keystore_file ALTER COLUMN format NOT NULL;

CREATE UNIQUE INDEX idx_keystore_file_name ON keystore_file(name);

-- Update keystore_key_metadata table
ALTER TABLE keystore_key_metadata ALTER COLUMN version NOT NULL;
-- mysql has a default of 0 for this column
ALTER TABLE keystore_key_metadata ALTER COLUMN version DEFAULT 0;

-- Update logon_info table
ALTER TABLE logon_info ALTER COLUMN fail_count DEFAULT 0;
ALTER TABLE logon_info ALTER COLUMN state DEFAULT 'ACTIVE';
ALTER TABLE logon_info ALTER COLUMN fail_count NOT NULL;
ALTER TABLE logon_info ALTER COLUMN last_activity NOT NULL;
ALTER TABLE logon_info ALTER COLUMN last_attempted NOT NULL;
ALTER TABLE logon_info ALTER COLUMN login NOT NULL;
ALTER TABLE logon_info ALTER COLUMN provider_goid NOT NULL;
ALTER TABLE logon_info ALTER COLUMN state NOT NULL;
ALTER TABLE logon_info ALTER COLUMN version NOT NULL;

ALTER TABLE logon_info ADD CONSTRAINT fk_logon_info_provider_goid FOREIGN KEY (provider_goid) REFERENCES identity_provider ON DELETE CASCADE;

CREATE UNIQUE INDEX unique_provider_login ON logon_info(provider_goid, login);

-- Update message_context_mapping_keys table
ALTER TABLE message_context_mapping_keys ALTER COLUMN version NOT NULL;

ALTER TABLE message_context_mapping_keys ADD COLUMN digested_backup varchar(36);
UPDATE message_context_mapping_keys SET digested_backup = digested;
ALTER TABLE message_context_mapping_keys DROP COLUMN digested;
ALTER TABLE message_context_mapping_keys ADD COLUMN digested char(36);
UPDATE message_context_mapping_keys SET digested = digested_backup;
ALTER TABLE message_context_mapping_keys ALTER COLUMN digested NOT NULL;
ALTER TABLE message_context_mapping_keys DROP COLUMN digested_backup;

CREATE INDEX idx_message_context_mapping_keys_digested ON message_context_mapping_keys(digested);

-- Update message_context_mapping_values table
ALTER TABLE message_context_mapping_values ALTER COLUMN mapping_keys_goid NOT NULL;

ALTER TABLE message_context_mapping_values ADD COLUMN digested_backup varchar(36);
UPDATE message_context_mapping_values SET digested_backup = digested;
ALTER TABLE message_context_mapping_values DROP COLUMN digested;
ALTER TABLE message_context_mapping_values ADD COLUMN digested char(36);
UPDATE message_context_mapping_values SET digested = digested_backup;
ALTER TABLE message_context_mapping_values ALTER COLUMN digested NOT NULL;
ALTER TABLE message_context_mapping_values DROP COLUMN digested_backup;

ALTER TABLE service_metrics_details ADD CONSTRAINT fk_service_metrics_details_mapping_values_goid FOREIGN KEY (mapping_values_goid) REFERENCES message_context_mapping_values;

CREATE INDEX idx_message_context_mapping_values_digested ON message_context_mapping_values(digested);

-- Update password_history table
ALTER TABLE password_history ALTER COLUMN prev_password SET DATA TYPE varchar(256);
ALTER TABLE password_history ALTER COLUMN last_changed NOT NULL;
ALTER TABLE password_history ALTER COLUMN prev_password NULL;

-- Update password_policy table
ALTER TABLE password_policy ALTER COLUMN version NOT NULL;
ALTER TABLE password_policy ADD CONSTRAINT fk_password_policy_internal_identity_provider_goid FOREIGN KEY (internal_identity_provider_goid) REFERENCES identity_provider ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_password_policy_internal_identity_provider_goid ON password_policy(internal_identity_provider_goid);

-- Update policy table
-- change size and type of policy guid column to match the mysql one
ALTER TABLE policy ADD COLUMN guid_backup varchar(255);
UPDATE policy SET guid_backup = guid;
ALTER TABLE policy DROP COLUMN guid;
ALTER TABLE policy ADD COLUMN guid CHAR(36);
UPDATE policy SET guid = guid_backup;
ALTER TABLE policy ALTER COLUMN guid NOT NULL;
ALTER TABLE policy DROP COLUMN guid_backup;

ALTER TABLE policy ALTER COLUMN name NOT NULL;
ALTER TABLE policy ALTER COLUMN policy_type NOT NULL;
ALTER TABLE policy ALTER COLUMN "xml" NOT NULL;
ALTER TABLE policy ALTER COLUMN soap DEFAULT 0;
ALTER TABLE policy ALTER COLUMN soap NOT NULL;

ALTER TABLE policy ADD CONSTRAINT i_guid UNIQUE (guid);

CREATE UNIQUE INDEX idx_policy_name ON policy(name);
CREATE UNIQUE INDEX idx_policy_guid ON policy(guid);
CREATE INDEX idx_policy_policy_type ON policy(policy_type);

-- Update policy_alias table
ALTER TABLE policy_alias ALTER COLUMN policy_goid NOT NULL;
ALTER TABLE policy_alias ALTER COLUMN folder_goid NOT NULL;
CREATE UNIQUE INDEX idx_policy_alias_folder_goid_policy_goid ON policy_alias(folder_goid, policy_goid);

-- Update policy_version table
ALTER TABLE policy_version ALTER COLUMN policy_goid NOT NULL;
ALTER TABLE policy_version ALTER COLUMN ordinal NOT NULL;
ALTER TABLE policy_version ALTER COLUMN time NOT NULL;

CREATE INDEX idx_policy_version_policy_goid ON policy_version(policy_goid);
CREATE UNIQUE INDEX idx_policy_version_ordinal ON policy_version(policy_goid, ordinal);
ALTER TABLE policy_version ADD CONSTRAINT fk_policy_version_policy_goid FOREIGN KEY (policy_goid) REFERENCES policy ON DELETE CASCADE;

-- Update published_service table
ALTER TABLE published_service ADD COLUMN policy_xml clob(2147483647);
ALTER TABLE published_service ALTER COLUMN disabled DEFAULT 0;
ALTER TABLE published_service ALTER COLUMN soap DEFAULT 1;
ALTER TABLE published_service ALTER COLUMN internal DEFAULT 0;
ALTER TABLE published_service ALTER COLUMN lax_resolution DEFAULT 0;
ALTER TABLE published_service ALTER COLUMN wss_processing DEFAULT 1;
ALTER TABLE published_service ALTER COLUMN tracing DEFAULT 0;
ALTER TABLE published_service ALTER COLUMN soap_version DEFAULT 'UNKNOWN';

ALTER TABLE published_service ALTER COLUMN name NOT NULL;
ALTER TABLE published_service ALTER COLUMN disabled NOT NULL;
ALTER TABLE published_service ALTER COLUMN soap NOT NULL;
ALTER TABLE published_service ALTER COLUMN internal NOT NULL;
ALTER TABLE published_service ALTER COLUMN lax_resolution NOT NULL;
ALTER TABLE published_service ALTER COLUMN wss_processing NOT NULL;
ALTER TABLE published_service ALTER COLUMN tracing NOT NULL;

ALTER TABLE published_service ALTER COLUMN default_routing_url SET DATA TYPE varchar(4096);

ALTER TABLE published_service ADD COLUMN http_methods_backup varchar(255);
UPDATE published_service SET http_methods_backup = http_methods;
ALTER TABLE published_service DROP COLUMN http_methods;
ALTER TABLE published_service ADD COLUMN http_methods clob(2147483647);
UPDATE published_service SET http_methods = http_methods_backup;
ALTER TABLE published_service DROP COLUMN http_methods_backup;

-- Update published_service_alias table
ALTER TABLE published_service_alias ALTER COLUMN folder_goid NOT NULL;
ALTER TABLE published_service_alias ALTER COLUMN published_service_goid NOT NULL;

ALTER TABLE published_service_alias ADD CONSTRAINT fk_published_service_alias_published_service_goid FOREIGN KEY (published_service_goid) REFERENCES published_service ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_published_service_alias_folder_goid_published_service_goid ON published_service_alias(folder_goid, published_service_goid);

-- Update rbac_assignment table
CREATE UNIQUE INDEX idx_rbac_assignment_unique_assignment ON rbac_assignment(provider_goid,role_goid,identity_id, entity_type);
CREATE INDEX i_rbacassign_uid ON rbac_assignment(identity_id);
CREATE INDEX i_rbacassign_pid ON rbac_assignment(provider_goid);

--add missing on delete cascade on rbac_role foreign key
ALTER TABLE rbac_assignment DROP FOREIGN KEY FK51FEC6DACCD6DF3E;
ALTER TABLE rbac_assignment ADD CONSTRAINT fk_rbac_assignment_role_goid FOREIGN KEY (role_goid) REFERENCES rbac_role ON DELETE CASCADE;

ALTER TABLE rbac_assignment ADD CONSTRAINT fk_rbac_assignment_provider_goid FOREIGN KEY (provider_goid) REFERENCES identity_provider ON DELETE CASCADE;

-- Update rbac_permission table
ALTER TABLE rbac_permission ALTER COLUMN version NOT NULL;
ALTER TABLE rbac_permission ALTER COLUMN role_goid NULL;
ALTER TABLE rbac_permission ALTER COLUMN operation_type NULL;
ALTER TABLE rbac_permission ALTER COLUMN entity_type NULL;

-- Update rbac_predicate table
ALTER TABLE rbac_predicate ALTER COLUMN version NOT NULL;
ALTER TABLE rbac_predicate ALTER COLUMN permission_goid NULL;

-- Update rbac_predicate_attribute table
ALTER TABLE rbac_predicate_attribute ALTER COLUMN attribute NULL;

-- Update rbac_predicate_entityfolder table
ALTER TABLE rbac_predicate_entityfolder ALTER COLUMN entity_type NOT NULL;
ALTER TABLE rbac_predicate_entityfolder ALTER COLUMN entity_id NOT NULL;

-- Update rbac_predicate_oid table
-- setting the entity_id to be null to match the mysql column
ALTER TABLE rbac_predicate_oid ALTER COLUMN entity_id NULL;

-- Update rbac_role table
ALTER TABLE rbac_role ALTER COLUMN name NULL;
ALTER TABLE rbac_role ALTER COLUMN version NOT NULL;

ALTER TABLE rbac_role ADD COLUMN description_backup varchar(255);
UPDATE rbac_role SET description_backup = description;
ALTER TABLE rbac_role DROP COLUMN description;
ALTER TABLE rbac_role ADD COLUMN description CLOB(2147483647);
UPDATE rbac_role SET description = description_backup;
ALTER TABLE rbac_role DROP COLUMN description_backup;

CREATE UNIQUE INDEX rbac_role_name ON rbac_role(name);
CREATE INDEX i_rbacrole_etype ON rbac_role(entity_type);
CREATE INDEX i_rbacrole_egoid ON rbac_role(entity_goid);

-- Update resolution_configuration table
ALTER TABLE resolution_configuration ALTER COLUMN version NOT NULL;
ALTER TABLE resolution_configuration ALTER COLUMN path_required NOT NULL;
ALTER TABLE resolution_configuration ALTER COLUMN path_case_sensitive NOT NULL;
ALTER TABLE resolution_configuration ALTER COLUMN use_url_header NOT NULL;
ALTER TABLE resolution_configuration ALTER COLUMN use_service_oid NOT NULL;
ALTER TABLE resolution_configuration ALTER COLUMN use_soap_action NOT NULL;
ALTER TABLE resolution_configuration ALTER COLUMN use_soap_namespace NOT NULL;

CREATE UNIQUE INDEX rc_name_idx ON resolution_configuration(name);

-- Update resource_entry table
ALTER TABLE resource_entry ALTER COLUMN version NOT NULL;
ALTER TABLE resource_entry ALTER COLUMN uri NOT NULL;
ALTER TABLE resource_entry ALTER COLUMN uri_hash NOT NULL;
ALTER TABLE resource_entry ALTER COLUMN type NOT NULL;
ALTER TABLE resource_entry ALTER COLUMN content_type NOT NULL;
ALTER TABLE resource_entry ALTER COLUMN content NOT NULL;
ALTER TABLE resource_entry ALTER COLUMN version NOT NULL;

CREATE UNIQUE INDEX idx_resource_entry_uri_hash ON resource_entry(uri_hash);

-- Update revocation_check_policy table
CREATE unique INDEX rcp_name_idx ON revocation_check_policy(name);

-- Update sample_messages table
ALTER TABLE sample_messages ALTER COLUMN name NOT NULL;
ALTER TABLE sample_messages ALTER COLUMN "xml" NOT NULL;

CREATE INDEX i_ps_goid ON sample_messages(published_service_goid);
CREATE INDEX i_operation_name ON sample_messages(operation_name);

-- Update secure_password table
ALTER TABLE secure_password ALTER COLUMN last_update DEFAULT 0;
ALTER TABLE secure_password ALTER COLUMN type DEFAULT 'PASSWORD';
ALTER TABLE secure_password ALTER COLUMN usage_from_variable DEFAULT 0;
ALTER TABLE secure_password ALTER COLUMN version NOT NULL;
ALTER TABLE secure_password ALTER COLUMN last_update NOT NULL;
ALTER TABLE secure_password ALTER COLUMN usage_from_variable NOT NULL;

ALTER TABLE secure_password ADD COLUMN encoded_password_backup clob(65535);
UPDATE secure_password SET encoded_password_backup = encoded_password;
ALTER TABLE secure_password DROP COLUMN encoded_password;
ALTER TABLE secure_password ADD COLUMN encoded_password clob(2147483647);
UPDATE secure_password SET encoded_password = encoded_password_backup;
ALTER TABLE secure_password ALTER COLUMN encoded_password NOT NULL;
ALTER TABLE secure_password DROP COLUMN encoded_password_backup;

CREATE UNIQUE INDEX idx_secure_password_name ON secure_password(name);

-- Update service_documents table
ALTER TABLE service_documents ALTER COLUMN service_goid NOT NULL;
ALTER TABLE service_documents ALTER COLUMN type NOT NULL;
ALTER TABLE service_documents ALTER COLUMN content_type NOT NULL;

ALTER TABLE service_documents ADD COLUMN uri_backup varchar(4096);
UPDATE service_documents SET uri_backup = uri;
ALTER TABLE service_documents DROP COLUMN uri;
ALTER TABLE service_documents ADD COLUMN uri clob(2147483647);
UPDATE service_documents SET uri = uri_backup;
ALTER TABLE service_documents DROP COLUMN uri_backup;

ALTER TABLE service_documents ADD CONSTRAINT fk_service_documents_service_goid FOREIGN KEY (service_goid) REFERENCES published_service ON DELETE CASCADE;

CREATE INDEX i_sd_service_type ON service_documents(service_goid, type);

-- Update service_metrics table
ALTER TABLE service_metrics ALTER COLUMN nodeid NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN published_service_goid NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN resolution NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN period_start NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN start_time NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN end_time NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN attempted NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN authorized NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN completed NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN back_sum NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN front_sum NOT NULL;
ALTER TABLE service_metrics ALTER COLUMN interval_size NOT NULL;

CREATE INDEX i_sm_nodeid ON service_metrics(nodeid);
CREATE INDEX i_sm_servicegoid ON service_metrics(published_service_goid);
CREATE INDEX i_sm_pstart ON service_metrics(period_start);
CREATE UNIQUE INDEX idx_internal_user_group_nodeid_published_service_goid_resolution_period_start ON service_metrics(nodeid, published_service_goid, resolution, period_start);

-- Update service_metrics_details table
ALTER TABLE service_metrics_details ALTER COLUMN attempted NOT NULL;
ALTER TABLE service_metrics_details ALTER COLUMN authorized NOT NULL;
ALTER TABLE service_metrics_details ALTER COLUMN completed NOT NULL;
ALTER TABLE service_metrics_details ALTER COLUMN back_sum NOT NULL;
ALTER TABLE service_metrics_details ALTER COLUMN front_sum NOT NULL;

ALTER TABLE service_metrics_details ADD CONSTRAINT fk_service_metrics_details_service_metrics_goid FOREIGN KEY (service_metrics_goid) REFERENCES service_metrics ON DELETE CASCADE;

-- Update service_usage table
ALTER TABLE service_usage ALTER COLUMN requestnr NOT NULL;
ALTER TABLE service_usage ALTER COLUMN authorizedreqnr NOT NULL;
ALTER TABLE service_usage ALTER COLUMN completedreqnr NOT NULL;

-- Update sink_config table
ALTER TABLE sink_config ALTER COLUMN type DEFAULT 'FILE';
ALTER TABLE sink_config ALTER COLUMN enabled DEFAULT 0;
ALTER TABLE sink_config ALTER COLUMN severity DEFAULT 'INFO';

ALTER TABLE sink_config ALTER COLUMN version NOT NULL;
ALTER TABLE sink_config ALTER COLUMN enabled NOT NULL;
ALTER TABLE sink_config ALTER COLUMN severity NOT NULL;
ALTER TABLE sink_config ALTER COLUMN type NOT NULL;

-- Update siteminder_configuration table
ALTER TABLE siteminder_configuration ALTER COLUMN update_sso_token NOT NULL;
ALTER TABLE siteminder_configuration ALTER COLUMN noncluster_failover NOT NULL;
ALTER TABLE siteminder_configuration ALTER COLUMN ipcheck NOT NULL;
ALTER TABLE siteminder_configuration ALTER COLUMN enabled DEFAULT 1;
ALTER TABLE siteminder_configuration ALTER COLUMN enabled NOT NULL;
ALTER TABLE siteminder_configuration ALTER COLUMN version NOT NULL;

CREATE UNIQUE INDEX i_name ON siteminder_configuration(name);

-- Update siteminder_configuration_property table
ALTER TABLE siteminder_configuration_property ADD COLUMN value_backup varchar(32672);
UPDATE siteminder_configuration_property SET value_backup = value;
ALTER TABLE siteminder_configuration_property DROP COLUMN value;
ALTER TABLE siteminder_configuration_property ADD COLUMN value clob(2147483647);
UPDATE siteminder_configuration_property SET value = value_backup;
ALTER TABLE siteminder_configuration_property ALTER COLUMN value NOT NULL;
ALTER TABLE siteminder_configuration_property DROP COLUMN value_backup;

-- Update trusted_cert table
ALTER TABLE trusted_cert ALTER COLUMN revocation_type DEFAULT 'USE_DEFAULT';
ALTER TABLE trusted_cert ALTER COLUMN trust_anchor DEFAULT 1;
ALTER TABLE trusted_cert ALTER COLUMN cert_base64 NOT NULL;
ALTER TABLE trusted_cert ALTER COLUMN version NOT NULL;

CREATE UNIQUE INDEX idx_trusted_cert_thumbprint_sha1 ON trusted_cert(thumbprint_sha1);
CREATE INDEX idx_trusted_cert_ski ON trusted_cert(ski);
CREATE INDEX idx_trusted_cert_subject_dn ON trusted_cert(subject_dn);
CREATE INDEX idx_trusted_cert_issuer_dn ON trusted_cert(issuer_dn);

-- Update trusted_esm table
ALTER TABLE trusted_esm ALTER COLUMN version NOT NULL;

-- Update trusted_esm_user table
ALTER TABLE trusted_esm_user ALTER COLUMN version NOT NULL;
ALTER TABLE trusted_esm_user ALTER COLUMN user_id NOT NULL;
ALTER TABLE trusted_esm_user ALTER COLUMN esm_user_id NOT NULL;

ALTER TABLE trusted_esm_user ADD CONSTRAINT fk_trusted_esm_user_provider_goid FOREIGN KEY (provider_goid) REFERENCES identity_provider ON DELETE CASCADE;

-- Update uddi_business_service_status table
ALTER TABLE uddi_business_service_status ALTER COLUMN published_service_goid NOT NULL;
ALTER TABLE uddi_business_service_status ALTER COLUMN metrics_reference_status NOT NULL;
ALTER TABLE uddi_business_service_status ALTER COLUMN policy_status NOT NULL;
ALTER TABLE uddi_business_service_status ALTER COLUMN uddi_registry_goid NOT NULL;
ALTER TABLE uddi_business_service_status ALTER COLUMN uddi_service_key NOT NULL;
ALTER TABLE uddi_business_service_status ALTER COLUMN uddi_service_name NOT NULL;
ALTER TABLE uddi_business_service_status ALTER COLUMN version NOT NULL;

ALTER TABLE uddi_business_service_status ADD CONSTRAINT fk_uddi_business_service_status_published_service_goid FOREIGN KEY (published_service_goid) REFERENCES published_service ON DELETE CASCADE;
ALTER TABLE uddi_business_service_status ADD CONSTRAINT fk_uddi_business_service_status_uddi_registry_goid FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries ON DELETE CASCADE;

-- Update uddi_proxied_service table
ALTER TABLE uddi_proxied_service ALTER COLUMN version NOT NULL;
ALTER TABLE uddi_proxied_service ALTER COLUMN uddi_service_key NOT NULL;
ALTER TABLE uddi_proxied_service ALTER COLUMN uddi_service_name NOT NULL;
ALTER TABLE uddi_proxied_service ALTER COLUMN wsdl_service_name NOT NULL;
ALTER TABLE uddi_proxied_service ALTER COLUMN wsdl_service_namespace NOT NULL;

CREATE unique INDEX idx_uddi_proxied_service_uddi_proxied_service_info_goid_wsdl_service_name_wsdl_service_namespace ON uddi_proxied_service(uddi_proxied_service_info_goid, wsdl_service_name, wsdl_service_namespace);
CREATE unique INDEX idx_uddi_proxied_service_uddi_service_key ON uddi_proxied_service(uddi_service_key);

-- Update uddi_proxied_service_info table
ALTER TABLE uddi_proxied_service_info ALTER COLUMN created_from_existing DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN metrics_enabled DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_wspolicy_enabled DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_wspolicy_full DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_wspolicy_inlined DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN remove_other_bindings DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN update_proxy_on_local_change DEFAULT 0;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN created_from_existing NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN metrics_enabled NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_type NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_wspolicy_enabled NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_wspolicy_full NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN publish_wspolicy_inlined NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN published_service_goid NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN remove_other_bindings NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN uddi_business_key NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN uddi_business_name NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN uddi_registry_goid NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN update_proxy_on_local_change NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN version NOT NULL;
ALTER TABLE uddi_proxied_service_info ALTER COLUMN wsdl_hash NOT NULL;

ALTER TABLE uddi_proxied_service_info ADD CONSTRAINT fk_uddi_proxied_service_info_published_service_goid FOREIGN KEY (published_service_goid) REFERENCES published_service ON DELETE CASCADE;
ALTER TABLE uddi_proxied_service_info ADD CONSTRAINT fk_uddi_proxied_service_info_uddi_registry_goid FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_uddi_proxied_service_info_published_service_goid ON uddi_proxied_service_info(published_service_goid);

-- Update uddi_publish_status table
ALTER TABLE uddi_publish_status ALTER COLUMN version NOT NULL;
ALTER TABLE uddi_publish_status ALTER COLUMN publish_status NOT NULL;
ALTER TABLE uddi_publish_status ALTER COLUMN fail_count DEFAULT 0;
ALTER TABLE uddi_publish_status ALTER COLUMN fail_count NOT NULL;

ALTER TABLE uddi_publish_status ADD CONSTRAINT fk_uddi_publish_status_uddi_proxied_service_info_goid FOREIGN KEY (uddi_proxied_service_info_goid) REFERENCES uddi_proxied_service_info ON DELETE CASCADE;
CREATE UNIQUE INDEX idx_uddi_publish_status_uddi_proxied_service_info_goid ON uddi_publish_status(uddi_proxied_service_info_goid);

-- Update uddi_registries table
ALTER TABLE uddi_registries ALTER COLUMN client_auth DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN enabled DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN metrics_publish_frequency DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN monitoring_enabled DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN metrics_enabled DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN subscribe_for_notifications DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN monitor_frequency DEFAULT 0;
ALTER TABLE uddi_registries ALTER COLUMN version NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN base_url NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN client_auth NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN enabled NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN inquiry_url NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN metrics_publish_frequency NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN monitoring_enabled NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN monitor_frequency NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN publish_url NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN security_url NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN subscribe_for_notifications NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN registry_type NOT NULL;
ALTER TABLE uddi_registries ALTER COLUMN metrics_enabled NOT NULL;

CREATE UNIQUE INDEX idx_uddi_registries_name ON uddi_registries(name);

-- Update uddi_registry_subscription table
ALTER TABLE uddi_registry_subscription ALTER COLUMN uddi_subscription_check_time NOT NULL;
ALTER TABLE uddi_registry_subscription ALTER COLUMN uddi_subscription_expiry_time NOT NULL;
ALTER TABLE uddi_registry_subscription ALTER COLUMN uddi_subscription_notified_time NOT NULL;
ALTER TABLE uddi_registry_subscription ALTER COLUMN uddi_registry_goid NOT NULL;
ALTER TABLE uddi_registry_subscription ALTER COLUMN version NOT NULL;

ALTER TABLE uddi_registry_subscription ADD CONSTRAINT fk_uddi_registry_subscription_uddi_registry_goid FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_uddi_registry_subscription_uddi_registry_goid ON uddi_registry_subscription(uddi_registry_goid);

-- Update uddi_service_control table
ALTER TABLE uddi_service_control ALTER COLUMN disable_service_on_change DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN has_been_overwritten DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN has_had_endpoints_removed DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN metrics_enabled DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN monitoring_enabled DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN publish_wspolicy_enabled DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN publish_wspolicy_full DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN publish_wspolicy_inlined DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN under_uddi_control DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN update_wsdl_on_change DEFAULT 0;
ALTER TABLE uddi_service_control ALTER COLUMN disable_service_on_change NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN has_been_overwritten NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN has_had_endpoints_removed NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN metrics_enabled NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN monitoring_enabled NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN publish_wspolicy_enabled NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN publish_wspolicy_full NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN publish_wspolicy_inlined NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN published_service_goid NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN uddi_business_key NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN uddi_business_name NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN uddi_registry_goid NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN uddi_service_key NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN uddi_service_name NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN under_uddi_control NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN update_wsdl_on_change NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN version NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN wsdl_port_binding NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN wsdl_port_binding_namespace NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN wsdl_port_name NOT NULL;
ALTER TABLE uddi_service_control ALTER COLUMN wsdl_service_name NOT NULL;

ALTER TABLE uddi_service_control ADD CONSTRAINT fk_uddi_service_control_published_service_goid FOREIGN KEY (published_service_goid) REFERENCES published_service ON DELETE CASCADE;
ALTER TABLE uddi_service_control ADD CONSTRAINT fk_uddi_service_control_uddi_registry_goid FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_uddi_service_control_published_service_goid ON uddi_service_control(published_service_goid);

-- Update uddi_service_control_monitor_runtime table
ALTER TABLE uddi_service_control_monitor_runtime ALTER COLUMN last_uddi_modified_timestamp NOT NULL;
ALTER TABLE uddi_service_control_monitor_runtime ALTER COLUMN access_point_url NOT NULL;
ALTER TABLE uddi_service_control_monitor_runtime ALTER COLUMN version NOT NULL;
ALTER TABLE uddi_service_control_monitor_runtime ALTER COLUMN uddi_service_control_goid NOT NULL;

ALTER TABLE uddi_service_control_monitor_runtime ADD CONSTRAINT fk_uddi_service_control_monitor_runtime_uddi_service_control_goid FOREIGN KEY (uddi_service_control_goid) REFERENCES uddi_service_control ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_uddi_service_control_monitor_runtime_uddi_service_control_goid ON uddi_service_control_monitor_runtime(uddi_service_control_goid);

-- Update wsdm_subscription table
ALTER TABLE wsdm_subscription ALTER COLUMN version NOT NULL;
-- Increase the size and change type of the wsdm_subscription reference_parameters column to match the other clob columns
ALTER TABLE wsdm_subscription ADD COLUMN reference_parameters_backup CLOB(16777215);
UPDATE wsdm_subscription SET reference_parameters_backup = reference_parameters;
ALTER TABLE wsdm_subscription DROP COLUMN reference_parameters;
ALTER TABLE wsdm_subscription ADD COLUMN reference_parameters CLOB(2147483647);
UPDATE wsdm_subscription SET reference_parameters = reference_parameters_backup;
ALTER TABLE wsdm_subscription DROP COLUMN reference_parameters_backup;

-- change size and type of wsdm_subscription notification_policy_guid column to match the mysql one
ALTER TABLE wsdm_subscription ADD COLUMN notification_policy_guid_backup varchar(36);
UPDATE wsdm_subscription SET notification_policy_guid_backup = notification_policy_guid;
ALTER TABLE wsdm_subscription DROP COLUMN notification_policy_guid;
ALTER TABLE wsdm_subscription ADD COLUMN notification_policy_guid CHAR(36);
UPDATE wsdm_subscription SET notification_policy_guid = notification_policy_guid_backup;
ALTER TABLE wsdm_subscription DROP COLUMN notification_policy_guid_backup;

ALTER TABLE wsdm_subscription ALTER COLUMN esm_service_goid DEFAULT X'0000000000000000FFFFFFFFFFFFFFFF';

ALTER TABLE wsdm_subscription ADD CONSTRAINT fk_wsdm_subscription_notification_policy_guid FOREIGN KEY (notification_policy_guid) REFERENCES policy(guid);

-- Update wssc_session table
ALTER TABLE wssc_session ALTER COLUMN inbound DEFAULT 0;
ALTER TABLE wssc_session ALTER COLUMN created NOT NULL;
ALTER TABLE wssc_session ALTER COLUMN expires NOT NULL;
ALTER TABLE wssc_session ALTER COLUMN inbound NOT NULL;
ALTER TABLE wssc_session ALTER COLUMN provider_goid NOT NULL;
ALTER TABLE wssc_session ALTER COLUMN user_id NOT NULL;
ALTER TABLE wssc_session ALTER COLUMN user_login NOT NULL;
ALTER TABLE wssc_session ALTER COLUMN session_key_hash NULL;
ALTER TABLE wssc_session ALTER COLUMN encrypted_key SET DATA TYPE varchar(4096);

-- Increase the size and change type of the wssc_session token column to match the other clob columns
ALTER TABLE wssc_session ADD COLUMN token_backup varchar(32672);
UPDATE wssc_session SET token_backup = token;
ALTER TABLE wssc_session DROP COLUMN token;
ALTER TABLE wssc_session ADD COLUMN token CLOB(2147483647);
UPDATE wssc_session SET token = token_backup;
ALTER TABLE wssc_session DROP COLUMN token_backup;

-- force session_key_hash index to be unique
ALTER TABLE wssc_session ADD COLUMN session_key_hash_backup varchar(128);
UPDATE wssc_session SET session_key_hash_backup = session_key_hash;
ALTER TABLE wssc_session DROP COLUMN session_key_hash;
ALTER TABLE wssc_session ADD COLUMN session_key_hash varchar(128);
UPDATE wssc_session SET session_key_hash = session_key_hash_backup;
ALTER TABLE wssc_session DROP COLUMN session_key_hash_backup;
CREATE UNIQUE INDEX idx_wssc_session_session_key_hash ON wssc_session(session_key_hash);
