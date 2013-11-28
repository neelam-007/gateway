-- Layer7 Technologies Inc.
-- OTK schema version otk2.0
--
-- Upgrade OTK database schema from otk1.0 to otk2.0
--
START TRANSACTION;
--
-- Adding an artificial primary key to oauth_token
--
ALTER TABLE oauth_token ADD CONSTRAINT UNIQUE oat_unique_token (token);
ALTER TABLE oauth_token DROP PRIMARY KEY;
ALTER TABLE oauth_token ADD COLUMN otk_token_id varchar(128);
--
-- Create an initial value for the new primary key
UPDATE oauth_token SET otk_token_id = token;
--
-- Create the new primary key oauth_token
ALTER TABLE oauth_token ADD PRIMARY KEY (otk_token_id);
--
-- Change expiration dates to be in seconds rather than milliseconds
UPDATE oauth_client_key SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
UPDATE oauth_initiate SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
UPDATE oauth_token SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
UPDATE oauth_session SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
--
-- Adding an index to oauth_token. This will improve the overall performance
CREATE INDEX oat_idx_expiration ON oauth_token (expiration);
--
-- Drop an existing foreign key. This will be handled in polcy when ever the client_name gets updated
ALTER TABLE oauth_client_key DROP FOREIGN KEY ock_fk_clientName;
--
-- Updating the version to otk1.1
--
UPDATE otk_version SET current_version='otk2.0';
--
COMMIT;