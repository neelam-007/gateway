alter table internal_user add column expiration bigint(20);
update internal_user set expiration=-1;
alter table internal_user modify column expiration bigint(20) not null;
alter table audit_main change ip_address ip_address varchar(32) null;
alter table client_cert change login login varchar(255) null;

DROP TABLE IF EXISTS audit_detail;
CREATE TABLE audit_detail (
   objectid bigint(20) NOT NULL,
   audit_oid bigint(20) NOT NULL,
   time bigint(20) NOT NULL,
   component_id integer,
   ordinal integer,
   message_id integer NOT NULL,
   exception MEDIUMTEXT,
   PRIMARY KEY (objectid),
   KEY idx_component_id (component_id),
   KEY idx_audit_oid (audit_oid)
) TYPE=InnoDB;

DROP TABLE IF EXISTS audit_detail_params;
CREATE TABLE audit_detail_params (
   audit_detail_oid bigint(20) NOT NULL,
   position integer NOT NULL,
   value varchar(255) NOT NULL,
   PRIMARY KEY (audit_detail_oid, position)
) Type=InnoDB;


ALTER TABLE audit_system ADD component_id integer;
UPDATE audit_system SET component_id = 1000000 WHERE component = 'GS';
ALTER TABLE audit_system DROP component;
CREATE INDEX idx_component_id ON audit_system (component_id);
alter table internal_user modify column login varchar(255);
alter table audit_admin  modify column admin_login varchar(255);
alter table fed_user  modify column login varchar(255);

