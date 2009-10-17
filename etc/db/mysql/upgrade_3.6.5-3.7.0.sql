--
-- Script to update mysql ssg database from 3.6.5 to 3.7
--
-- Layer 7 Technologies, inc
--

--
-- Table structure for table 'service_documents'
--

DROP TABLE IF EXISTS service_documents;
CREATE TABLE service_documents (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  service_oid bigint(20) NOT NULL,
  uri MEDIUMTEXT,
  type VARCHAR(32) NOT NULL,
  content_type VARCHAR(32) NOT NULL,
  content MEDIUMTEXT,
  INDEX i_sd_service_type (service_oid, type),
  PRIMARY KEY (objectid),
  FOREIGN KEY (service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Foreign key from sample_messages to published_service
--

-- Delete orphaned messages
DELETE FROM sample_messages
    WHERE published_service_oid IS NULL or published_service_oid NOT IN (SELECT objectid FROM published_service);

-- Add foreign key constraint
ALTER TABLE sample_messages
    ADD FOREIGN KEY (published_service_oid)
    REFERENCES published_service (objectid)
    ON DELETE CASCADE;

-- Mark upgrade task
    insert into cluster_properties
        (objectid, version, propkey, propvalue)
        values (-300700, 0, "upgrade.task.300700", "com.l7tech.server.upgrade.Upgrade365To37AddSampleMessagePermissions");


--
-- JMS updates
--
ALTER TABLE jms_connection ADD column properties mediumtext null;

--
-- Add lax_resolution column to published_service
--
ALTER TABLE published_service ADD lax_resolution TINYINT(1) NOT NULL DEFAULT 0;
