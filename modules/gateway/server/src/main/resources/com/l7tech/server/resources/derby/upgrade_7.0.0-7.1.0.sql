--
-- Script to update derby ssg database from 7.0.0 to 7.1.0
--
-- Layer 7 Technologies, inc
--

UPDATE ssg_version SET current_version = '7.1.0';

--
-- Encapsulated Assertions
--
CREATE TABLE encapsulated_assertion (
  objectid bigint not null,
  version integer,
  name varchar(255),
  guid varchar(255) not null unique,
  policy_oid bigint NOT NULL,
  PRIMARY KEY (objectid)
);

alter table encapsulated_assertion
    add constraint FK_ENCASS_POL
    foreign key (policy_oid)
    references policy;

CREATE TABLE encapsulated_assertion_property (
  encapsulated_assertion_oid bigint NOT NULL,
  name varchar(255) NOT NULL,
  value clob(2147483647) NOT NULL
);

alter table encapsulated_assertion_property
    add constraint FK_ENCASSPROP_ENCASS
    foreign key (encapsulated_assertion_oid)
    references encapsulated_assertion
    on delete cascade;

CREATE TABLE encapsulated_assertion_argument (
  objectid bigint not null,
  version integer,
  encapsulated_assertion_oid bigint NOT NULL,
  argument_name varchar(255) NOT NULL,
  argument_type varchar(255) NOT NULL,
  gui_prompt smallint NOT NULL,
  gui_label varchar(255),
  ordinal integer NOT NULL,
  PRIMARY KEY (objectid)
);

alter table encapsulated_assertion_argument
    add constraint FK_ENCASSARG_ENCASS
    foreign key (encapsulated_assertion_oid)
    references encapsulated_assertion
    on delete cascade;

CREATE TABLE encapsulated_assertion_result (
  objectid bigint not null,
  version integer,
  encapsulated_assertion_oid bigint NOT NULL,
  result_name varchar(255) NOT NULL,
  result_type varchar(255) NOT NULL,
  FOREIGN KEY (encapsulated_assertion_oid) REFERENCES encapsulated_assertion (objectid) ON DELETE CASCADE,
  PRIMARY KEY (objectid)
);

alter table encapsulated_assertion_result
    add constraint FK_ENCASSRES_ENCASS
    foreign key (encapsulated_assertion_oid)
    references encapsulated_assertion
    on delete cascade;

INSERT INTO rbac_role VALUES (-1350,0,'Manage Encapsulated Assertions', null,'ENCAPSULATED_ASSERTION',null, 'Users assigned to the {0} role have the ability to create/read/update/delete encapsulated assertions.',0);
INSERT INTO rbac_permission VALUES (-1351,0,-1350,'CREATE',null,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1352,0,-1350,'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1353,0,-1350,'UPDATE',null, 'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1354,0,-1350,'DELETE',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1355,0,-1350,'READ',NULL,'POLICY');
INSERT INTO rbac_predicate VALUES (-1356,0,-1355);
INSERT INTO rbac_predicate_attribute VALUES (-1356,'type','Included Policy Fragment','eq');

ALTER TABLE cluster_properties ADD COLUMN properties clob(2147483647);

ALTER TABLE published_service ALTER COLUMN wsdl_url SET DATA TYPE VARCHAR(4096);
