--
-- Script to update mysql ssg database from 8.2.00 to the pre 8.3.0 version
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.3.pre';

-- Adding missing primary keys
AlTER TABLE active_connector_property ADD CONSTRAINT PRIMARY KEY (connector_goid, name);
AlTER TABLE connector_property ADD CONSTRAINT PRIMARY KEY (connector_goid, name);
AlTER TABLE policy_alias ADD CONSTRAINT PRIMARY KEY (goid);
AlTER TABLE published_service_alias ADD CONSTRAINT PRIMARY KEY (goid);
AlTER TABLE siteminder_configuration_property ADD CONSTRAINT PRIMARY KEY (siteminder_configuration_goid, name);

-- Update audit_admin entity_class column to match larger derby size.
ALTER TABLE audit_admin DROP INDEX idx_class;
AlTER TABLE audit_admin MODIFY entity_class varchar(1024);
ALTER TABLE audit_admin ADD KEY idx_class (entity_class(255));

-- update to match larger derby size
AlTER TABLE client_cert MODIFY serial varchar(255);

-- Update cluster property propkey column to match larger derby size.
AlTER TABLE cluster_properties MODIFY propkey varchar(255) NOT NULL;

-- adding index and foreign key that is in derby db but not mysql
-- need to remove email_listener_state for no longer existing email_listener.
DELETE FROM email_listener_state WHERE email_listener_goid NOT IN (SELECT goid FROM email_listener);
alter table email_listener_state add constraint FOREIGN KEY fk_email_listener_state_email_listener_goid (email_listener_goid) references email_listener(goid) on delete cascade;
alter table email_listener_state add constraint UNIQUE KEY idx_email_listener_state_email_listener_goid (email_listener_goid);

-- Update encapsulated_assertion name column to match larger derby size.
AlTER TABLE encapsulated_assertion MODIFY name varchar(255) NOT NULL;

-- update to match larger derby size
AlTER TABLE encapsulated_assertion_argument MODIFY argument_name varchar(255) NOT NULL;
AlTER TABLE encapsulated_assertion_argument MODIFY argument_type varchar(255) NOT NULL;

-- Update encapsulated_assertion name column to match larger derby size.
AlTER TABLE encapsulated_assertion_property MODIFY name varchar(255) NOT NULL;

-- Update encapsulated_assertion_result result_name and result_type columns to match larger derby size.
AlTER TABLE encapsulated_assertion_result MODIFY result_name varchar(255) NOT NULL;
AlTER TABLE encapsulated_assertion_result MODIFY result_type varchar(255) NOT NULL;

-- update to match larger derby size
ALTER TABLE fed_group_virtual DROP INDEX i_x509_subject_dn_pattern;
AlTER TABLE fed_group_virtual MODIFY x509_subject_dn_pattern varchar(1024);
ALTER TABLE fed_group_virtual ADD KEY i_x509_subject_dn_pattern (x509_subject_dn_pattern(255));

-- Update folder name column to match larger derby size.
AlTER TABLE folder MODIFY name varchar(255) NOT NULL;

-- update to match larger derby size
ALTER TABLE generic_entity DROP INDEX i_classname_name;
AlTER TABLE generic_entity MODIFY classname varchar(1024) NOT NULL;
ALTER TABLE generic_entity ADD UNIQUE KEY i_classname_name (classname(255), name);

-- Update tls_keystore_goid default to be a proper hex value
AlTER TABLE http_configuration MODIFY tls_keystore_goid binary(16) NOT NULL DEFAULT X'00000000000000000000000000000000';
-- update to match larger derby size
AlTER TABLE http_configuration MODIFY protocol varchar(255);
AlTER TABLE http_configuration MODIFY proxy_use varchar(255) DEFAULT 'DEFAULT';
AlTER TABLE http_configuration MODIFY tls_key_use varchar(255) DEFAULT 'DEFAULT';

-- update to match larger derby size
AlTER TABLE jdbc_connection MODIFY driver_class varchar(1024) NOT NULL;
AlTER TABLE jdbc_connection MODIFY password varchar(255) NOT NULL;
AlTER TABLE jdbc_connection MODIFY user_name varchar(255) NOT NULL;

-- update to match larger derby size
AlTER TABLE jms_connection MODIFY factory_classname varchar(1024);
AlTER TABLE jms_connection MODIFY destination_factory_url varchar(4096) default '';

-- update to match larger derby size
AlTER TABLE jms_endpoint MODIFY acknowledgement_type varchar(255);
AlTER TABLE jms_endpoint MODIFY outbound_message_type varchar(255);

-- update to match larger derby size
AlTER TABLE keystore_file MODIFY format varchar(255) NOT NULL;

-- Update logon_info state column to match larger derby size.
AlTER TABLE logon_info MODIFY state varchar(255) NOT NULL DEFAULT 'ACTIVE';

-- Update policy policy_type column to match larger derby size.
AlTER TABLE policy MODIFY policy_type varchar(255) NOT NULL;

-- update to match larger derby size
AlTER TABLE policy_version MODIFY ordinal BIGINT(19) NOT NULL;

-- update to match larger derby size
AlTER TABLE published_service MODIFY routing_uri varchar(255);
AlTER TABLE published_service MODIFY soap_version varchar(255) default 'UNKNOWN';

-- update to match larger derby size
AlTER TABLE resource_entry MODIFY description varchar(2048);
AlTER TABLE resource_entry MODIFY content_type varchar(1024) NOT NULL;

-- update to match larger derby size
AlTER TABLE sample_messages MODIFY operation_name varchar(255);
AlTER TABLE sample_messages MODIFY name varchar(255) NOT NULL;

-- Adding unique key constraint missing from derby table
alter table security_zone add constraint UNIQUE KEY idx_security_zone_name (name);

-- update to match larger derby size
AlTER TABLE service_documents MODIFY type varchar(255) NOT NULL;
AlTER TABLE service_documents MODIFY content_type varchar(1024) NOT NULL;

-- update to match larger derby size
AlTER TABLE service_metrics MODIFY back_sum BIGINT(19) NOT NULL;
AlTER TABLE service_metrics MODIFY front_sum BIGINT(19) NOT NULL;
AlTER TABLE service_metrics MODIFY nodeid varchar(255) NOT NULL;

-- Update service_metrics_details back_sum and front_sum column to match larger derby size.
AlTER TABLE service_metrics_details MODIFY back_sum BIGINT(19) NOT NULL;
AlTER TABLE service_metrics_details MODIFY front_sum BIGINT(19) NOT NULL;

-- update to match larger derby size
AlTER TABLE service_usage MODIFY nodeid varchar(255) NOT NULL;

-- update to match larger derby size
AlTER TABLE ssg_version MODIFY current_version varchar(10) NOT NULL;

-- update to match larger derby size
AlTER TABLE trusted_cert MODIFY serial varchar(1024);

-- update to match larger derby size
AlTER TABLE uddi_business_service_status MODIFY metrics_reference_status varchar(255) NOT NULL;
AlTER TABLE uddi_business_service_status MODIFY policy_status varchar(255) NOT NULL;
AlTER TABLE uddi_business_service_status MODIFY uddi_policy_url varchar(4096);
AlTER TABLE uddi_business_service_status MODIFY uddi_policy_publish_url varchar(4096);

-- Update uddi_proxied_service_info wsdl_hash and publish_type column to match larger derby size.
AlTER TABLE uddi_proxied_service_info MODIFY wsdl_hash varchar(512) NOT NULL;
AlTER TABLE uddi_proxied_service_info MODIFY publish_type varchar(255) NOT NULL;

-- Update uddi_publish_status publish_status column to match larger derby size.
AlTER TABLE uddi_publish_status MODIFY publish_status varchar(255) NOT NULL;

-- update to match larger derby size
AlTER TABLE uddi_registries MODIFY base_url varchar(4096) NOT NULL;
AlTER TABLE uddi_registries MODIFY inquiry_url varchar(4096) NOT NULL;
AlTER TABLE uddi_registries MODIFY metrics_publish_frequency BIGINT(19) NOT NULL DEFAULT 0;
AlTER TABLE uddi_registries MODIFY monitor_frequency BIGINT(19) NOT NULL DEFAULT 0;
AlTER TABLE uddi_registries MODIFY password varchar(255);
AlTER TABLE uddi_registries MODIFY user_name varchar(255);
AlTER TABLE uddi_registries MODIFY security_url varchar(4096) NOT NULL;
AlTER TABLE uddi_registries MODIFY publish_url varchar(4096) NOT NULL;
AlTER TABLE uddi_registries MODIFY subscription_url varchar(4096);
AlTER TABLE uddi_registries MODIFY registry_type varchar(255) NOT NULL;

-- Update wsdm_subscription owner_node_id column to match larger derby size.
AlTER TABLE wsdm_subscription MODIFY owner_node_id varchar(64);
-- Update wsdm_subscription callback_url column to match larger derby size.
AlTER TABLE wsdm_subscription MODIFY callback_url varchar(4096) NOT NULL;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--
