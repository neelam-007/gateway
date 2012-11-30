-- --------------------------------------------------------------------------
-- Test in-memory derby database schema
-- --------------------------------------------------------------------------
--

create table ssg_version (
    current_version varchar(10) not null
);

insert into ssg_version (current_version) VALUES ('1.0.0');