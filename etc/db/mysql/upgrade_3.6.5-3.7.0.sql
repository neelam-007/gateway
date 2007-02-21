---
--- Script to update mysql ssg database from 3.6.5 to 3.7
---
--- Layer 7 Technologies, inc
---

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
