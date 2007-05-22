---
--- Script to update mysql ssg database from 3.7 to 4.0
---
--- Layer 7 Technologies, inc
---

--
-- Table structure for new table 'keystore_file'
--

CREATE TABLE keystore_file (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  format varchar(128) NOT NULL,
  updatingnodeid varchar(32),
  databytes mediumblob,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

insert into keystore_file values (0, 0, "Software Static", "ss", null, null);     -- placeholder, never loaded or saved
insert into keystore_file values (1, 0, "HSM", "hsm.sca.targz", null, null);      -- tar.gz of items in sca 6000 keydata directory
insert into keystore_file values (2, 0, "Software DB", "sdb.pkcs12", null, null); -- bytes of a PKCS#12 keystore

CREATE TABLE shared_keys (
  encodingid varchar(32) NOT NULL,
  b64edval varchar(256) NOT NULL,
  primary key(encodingid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;