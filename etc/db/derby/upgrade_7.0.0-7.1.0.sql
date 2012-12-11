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
  default_value varchar(32672),
  gui_prompt smallint NOT NULL,
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

ALTER TABLE cluster_properties ADD COLUMN properties clob(2147483647);

ALTER TABLE published_service ALTER COLUMN wsdl_url SET DATA TYPE VARCHAR(4096);
