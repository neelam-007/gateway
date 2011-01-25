--
-- Script to update mysql ssg database from 5.4.0 to 5.4.1
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version
--

UPDATE ssg_version SET current_version = '5.4.1';

ALTER TABLE jms_endpoint ADD COLUMN request_max_size bigint NOT NULL default -1 AFTER use_message_id_for_correlation;

-- bug 9651
UPDATE rbac_role SET description = 'Users assigned to the {0} role have the ability to read, create, update and delete UDDI Registry connections.' where objectid = -1000; 

--
-- Service resolution changes
--

DROP TABLE IF EXISTS service_resolution;

DROP TABLE IF EXISTS resolution_configuration;
CREATE TABLE resolution_configuration (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  path_required tinyint NOT NULL default '0',
  path_case_sensitive tinyint NOT NULL default '0',
  use_url_header tinyint NOT NULL default '0',
  use_service_oid tinyint NOT NULL default '0',
  use_soap_action tinyint NOT NULL default '0',
  use_soap_namespace tinyint NOT NULL default '0',
  PRIMARY KEY (objectid),
  UNIQUE KEY rc_name_idx (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO resolution_configuration (objectid, version, name, path_case_sensitive, use_url_header, use_service_oid, use_soap_action, use_soap_namespace) VALUES (-2, 0, 'Default', 1, 1, 1, 1, 1);

--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

