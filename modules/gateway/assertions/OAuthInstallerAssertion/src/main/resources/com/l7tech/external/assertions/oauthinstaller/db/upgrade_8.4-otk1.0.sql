--
-- Script to update mysql otk database from 8.4 (tactical version) to otk1.0
--
-- CA Technologies
--
ALTER TABLE oauth_client_key MODIFY COLUMN callback varchar(2048) NOT NULL default 'oob';

--
-- This table holds session info
--
CREATE TABLE oauth_session (
  session_key varchar(128) not null,
  session_group varchar(128) not null,
  expiration bigint not null,
  value mediumtext not null,
  primary key (session_key, session_group)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Create version table
--
CREATE TABLE otk_version (
   current_version char(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO otk_version (current_version) VALUES ('otk1.0');