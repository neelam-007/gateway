alter table internal_user add column expiration bigint(20);
update internal_user set expiration=-1;
alter table internal_user modify column expiration bigint(20) not null;
alter TABLE audit_main change ip_address ip_address varchar(32) null;
