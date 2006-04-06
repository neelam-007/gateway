---
--- Script to update mysql ssg database from 3.4(.1) to 4.0
---
--- Layer 7 Technologies, inc
---

--
-- 
--
ALTER TABLE published_service ADD COLUMN http_methods mediumtext;

--
-- Modifications to auditing schema to promote identity to top level
--
-- add new columns
alter table audit_main add column user_name varchar(255);
alter table audit_main add column user_id varchar(255);
alter table audit_main add column provider_oid bigint(20) not null default -1;

-- migrate any existing data
update audit_main,audit_message set audit_main.user_name=audit_message.user_name, audit_main.user_id=audit_message.user_id, audit_main.provider_oid=audit_message.provider_oid where audit_main.objectid=audit_message.objectid;
update audit_main,audit_admin set audit_main.user_name=audit_admin.admin_login, audit_main.user_id=(select objectid from internal_user where login=audit_admin.admin_login) where audit_main.objectid=audit_admin.objectid;

-- drop old user columns
alter table audit_message drop column user_name;
alter table audit_message drop column user_id;
alter table audit_message drop column provider_oid;
alter table audit_admin drop column admin_login;


-- Rename community_schemas.schema to work around reserved word
alter table community_schemas change schema schema_xml mediumtext default '';

--alter table service_resolution drop index `soapaction`;
alter table service_resolution modify column soapaction mediumtext character set latin1 BINARY default '';
alter table service_resolution modify column urn mediumtext character set latin1 BINARY default '';
alter table service_resolution modify column uri mediumtext character set latin1 BINARY default '';
--alter table service_resolution add digested varchar(32) default '';
--update service_resolution set digested=HEX(MD5(CONCAT(soapaction,urn,uri)));
--alter table service_resolution modify column digested varchar(32) NOT NULL;
--CREATE UNIQUE INDEX digested ON service_resolution (digested);

------------------------
-- META-GROUP SUPPORT --
------------------------

-- Get rid of old composite PK
alter table internal_user_group drop primary key;

-- Add/rename columns
alter table internal_user_group change internal_user user_id varchar(255) null;
alter table internal_user_group add provider_oid bigint(20) not null;
alter table internal_user_group add subgroup_id varchar(255);
alter table internal_user_group add version int(11) not null;

-- Populate new column to reflect belonging to IIP
update internal_user_group set provider_oid = -2;

-- Add new PK with auto_increment to generate unique values
-- (hopefully won't collide with hibernate's high/low generator)
alter table internal_user_group add objectid bigint(20) auto_increment primary key;

-- Redefine PK to exclude auto_increment
alter table internal_user_group change objectid objectid bigint(20) not null;

-- Add new indexes
alter table internal_user_group add index (provider_oid);
alter table internal_user_group add index (user_id);
alter table internal_user_group add index (subgroup_id);

