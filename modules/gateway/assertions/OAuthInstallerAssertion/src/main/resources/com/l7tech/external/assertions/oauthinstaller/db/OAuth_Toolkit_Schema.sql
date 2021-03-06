-- CA Technologies
-- OTK schema version otk2.0
--
-- Difference between otk1.0 and otk2.0:
-- - removed foreign_key ock_fk_clientName from oauth_client_key
-- - removed ON UPDATE CASCADE, this got implemented in policy at /clientstore endpoint using multiple sql statements
-- - added 'otk_token_id' at 'oauth_token as primary key
-- - added an index oat_idx_expiration on oauth_token.expiration
-- - added constraint uk_oc_name to oauth_client.name
--
-- This table holds valid client application values as defined when the application was registered
--
CREATE TABLE oauth_client (
  client_ident varchar(128) primary key,
  name varchar(128) not null COMMENT 'The associated name of the application using this client_id',
  type varchar(128) not null DEFAULT 'oob' COMMENT 'used with oauth 2.0',
  description varchar(256) null,
  organization varchar(128) not null,
  registered_by varchar(128) not null,
  created bigint DEFAULT 0 not null,
  constraint uk_oc_name_org unique (name, organization),
  constraint uk_oc_name unique (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
--
-- This table holds valid client keys
--
CREATE TABLE oauth_client_key (
  client_key varchar(128) primary key COMMENT 'oauth_consumer_key or client_id',
  secret varchar(128) not null COMMENT 'oauth_consumer_key_secret or client_secret',
  scope varchar(512) not null DEFAULT 'oob' COMMENT 'for oauth2, to be defined by the customer and handled accordingly within the policy',
  callback varchar(2048) not null DEFAULT 'oob' COMMENT 'in oauth2 = redirect_uri, contains one URI',
  environment varchar(128) not null DEFAULT 'ALL' COMMENT 'COULD BE SOMETHING LIKE Test, Prod, Integration',
  expiration bigint not null DEFAULT 0 COMMENT 'Date until this key is valid',
  status varchar(128) not null COMMENT 'for validation purposes, ENABLED or DISABLED',
  created bigint DEFAULT 0 not null,
  created_by varchar(128) not null,
  client_ident varchar(128) not null COMMENT 'The client that owns this key',
  client_name varchar(128) not null COMMENT 'The name of the client that owns this key. Not normalized for performance.',
  constraint ock_fk_clientIdent foreign key (client_ident) references oauth_client (client_ident) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
--
-- This table holds access_tokens and refresh_tokens
-- oauth 1.0 = access_token
-- oauth 2.0 = access_token, refresh_token
--
CREATE TABLE oauth_token (
  otk_token_id varchar(128) primary key,
  token varchar(128) unique,
  secret varchar(128) null COMMENT 'null for oauth 2.0, it does not provide a secret',
  expiration bigint not null,
  scope varchar(512) null COMMENT 'for 0auth 2.0, the scope granted by the resource owner',
  resource_owner varchar(128) not null COMMENT 'the authenticated user that granted the token',
  created bigint DEFAULT 0 COMMENT 'the date this token (or these tokens in oauth 2.0) were created',
  rtoken varchar(128) null unique key,
  rexpiration bigint DEFAULT 0 COMMENT 'DEFAULT 0 because otherwise timestamp will be set to now() on an update',
  status varchar(128) not null COMMENT 'for validation purposes, ENABLED or DISABLED',
  client_key varchar(128) not null COMMENT 'the client_key this token was issued for',
  client_name varchar(128) not null COMMENT 'The name of the client that owns this key. Not normalized for performance.'
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
-- Adding an index to oauth_token. This will improve the overall performance
CREATE INDEX oat_idx_expiration ON oauth_token (expiration);
CREATE INDEX oat_idx_rowner_client ON oauth_token(resource_owner,client_key);
--
-- This table holds temporary tokens
-- oauth 1.0 = request_token
-- oauth 2.0 = authorization_code
--
CREATE TABLE oauth_initiate (
  token varchar(128) primary key COMMENT 'for oauth 1.0, 2.0',
  secret varchar(128) null COMMENT 'null for oauth 2.0, it does not provide a secret',
  expiration bigint not null DEFAULT 0 COMMENT 'for oauth 1.0, 2.0, DEFAULT 0 because otherwise timestamp will be set to now() on an update',
  scope varchar(4096) null COMMENT 'for oauth 2.0, the scope granted by the resource owner',
  resource_owner varchar(128) null COMMENT 'the authenticated user that granted the token',
  created bigint DEFAULT 0 COMMENT 'the date this token (or these tokens in oauth 2.0) were created',
  verifier varchar(128) null COMMENT 'for oauth 1.0',
  callback varchar(256) null COMMENT 'for oauth 1.0, 2.0',
  client_key varchar(128) not null COMMENT 'the client that received this token',
  client_name varchar(128) not null COMMENT 'The name of the client that owns this key. Not normalized for performance.'
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
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
CREATE TABLE otk_version (
  current_version char(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
INSERT INTO otk_version (current_version) VALUES ('otk2.0');
--
-- Create test clients
--
INSERT INTO oauth_client (client_ident, name, description, organization, registered_by)
  VALUES ('TestClient1.0', 'OAuth1Client', 'OAuth 1.0 test client hosted on the ssg', 'Layer7 Technologies Inc.', 'OTK Installer');

INSERT INTO oauth_client (client_ident, name, description, organization, registered_by, type)
  VALUES ('TestClient2.0', 'OAuth2Client', 'OAuth 2.0 test client hosted on the ssg', 'Layer7 Technologies Inc.', 'OTK Installer', 'confidential');

INSERT INTO oauth_client_key (client_key, secret, status, created_by, client_ident, client_name)
  VALUES ('acf89db2-994e-427b-ac2c-88e6101f9433', '74d5e0db-cd8b-4d8e-a989-95a0746c3343', 'ENABLED', 'OTK Installer', 'TestClient1.0', 'OAuth1Client');

INSERT INTO oauth_client_key (client_key, secret, status, created_by, client_ident, client_name, callback)
  VALUES ('54f0c455-4d80-421f-82ca-9194df24859d', 'a0f2742f-31c7-436f-9802-b7015b8fd8e6', 'ENABLED', 'OTK Installer', 'TestClient2.0', 'OAuth2Client', 'YOUR_SSG/oauth/v2/client/authcode,YOUR_SSG/oauth/v2/client/implicit');

COMMIT;