-- CA Technologies
-- OTK schema version otk2.0 for Oracle
--
-- This table holds valid client application values as defined when the application was registered
--
CREATE TABLE oauth_client (
  client_ident varchar(128) primary key,
  name varchar(128) not null,
  type varchar(128) default 'oob' not null,
  description varchar(256) null,
  organization varchar(128) not null,
  registered_by varchar(128) not null,
  created number default 0 not null,
  constraint uk_oc_name_org unique (name, organization),
  constraint uk_oc_name unique (name)
)
/
--
-- This table holds valid client keys
--
CREATE TABLE oauth_client_key (
  client_key varchar(128) primary key,
  secret varchar(128) not null,
  scope varchar(512) DEFAULT 'oob' not null,
  callback varchar(2048) DEFAULT 'oob' not null,
  environment varchar(128) DEFAULT 'ALL' not null,
  expiration number DEFAULT 0 not null ,
  status varchar(128) not null,
  created number DEFAULT 0 not null,
  created_by varchar(128) not null,
  client_ident varchar(128) not null,
  client_name varchar(128) not null,
  constraint ock_fk_clientIdent foreign key (client_ident) references oauth_client (client_ident) ON DELETE CASCADE
)
/
-- This table holds access_tokens and refresh_tokens
-- oauth 1.0 = access_token
-- oauth 2.0 = access_token, refresh_token
--
CREATE TABLE oauth_token (
  otk_token_id varchar(128) primary key,
  token varchar(128) unique,
  secret varchar(128) null,
  expiration number not null,
  scope varchar(512),
  resource_owner varchar(128) not null,
  created number DEFAULT 0,
  rtoken varchar(128) null unique,
  rexpiration number DEFAULT 0,
  status varchar(128) not null,
  client_key varchar(128) not null,
  client_name varchar(128) not null
)
/
-- Adding an index
CREATE INDEX oat_idx_expiration ON oauth_token (expiration)
/
CREATE INDEX oat_idx_rowner_client ON oauth_token(resource_owner,client_key)
/
--
-- This table holds temporary tokens
-- oauth 1.0 = request_token
-- oauth 2.0 = authorization_code
--
CREATE TABLE oauth_initiate (
  token varchar(128) primary key,
  secret varchar(128) null,
  expiration number DEFAULT 0 not null,
  scope varchar(512) null,
  resource_owner varchar(128) null,
  created number DEFAULT 0,
  verifier varchar(128) null,
  callback varchar(256) null,
  client_key varchar(128) not null,
  client_name varchar(128) not null
)
/
--
-- This table holds session info
--
CREATE TABLE oauth_session (
  session_key varchar(128) not null,
  session_group varchar(128) not null,
  expiration number not null,
  value clob not null,
  primary key (session_key, session_group)
)
/
CREATE TABLE otk_version (
   current_version char(10) NOT NULL
)
/
INSERT INTO otk_version (current_version) VALUES ('otk2.0')
/
--
-- Create test clients
--
INSERT INTO oauth_client (client_ident, name, description, organization, registered_by)
VALUES ('TestClient1.0', 'OAuth1Client', 'OAuth 1.0 test client hosted on the ssg', 'Layer7 Technologies Inc.', 'OTK Installer')
/
INSERT INTO oauth_client (client_ident, name, description, organization, registered_by, type)
VALUES ('TestClient2.0', 'OAuth2Client', 'OAuth 2.0 test client hosted on the ssg', 'Layer7 Technologies Inc.', 'OTK Installer', 'confidential')
/
INSERT INTO oauth_client_key (client_key, secret, status, created_by, client_ident, client_name)
VALUES ('acf89db2-994e-427b-ac2c-88e6101f9433', '74d5e0db-cd8b-4d8e-a989-95a0746c3343', 'ENABLED', 'OTK Installer', 'TestClient1.0', 'OAuth1Client')
/
INSERT INTO oauth_client_key (client_key, secret, status, created_by, client_ident, client_name, callback)
VALUES ('54f0c455-4d80-421f-82ca-9194df24859d', 'a0f2742f-31c7-436f-9802-b7015b8fd8e6', 'ENABLED', 'OTK Installer', 'TestClient2.0', 'OAuth2Client', 'YOUR_SSG/oauth/v2/client/authcode,YOUR_SSG/oauth/v2/client/implicit')
/
COMMIT
/