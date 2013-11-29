-- Layer7 Technologies Inc.
-- OTK schema version otk2.0
--
-- Upgrade OTK database schema from otk1.0 to otk2.0
--
START TRANSACTION;
--
-- Delete all oldest tokens for multiple existing combinations of resource_owner, client_key
CREATE TEMPORARY TABLE oauth_token_temp (id varchar(128));
INSERT oauth_token_temp(id) SELECT token FROM oauth_token yt WHERE EXISTS (SELECT * FROM oauth_token yt2 WHERE yt.expiration < yt2.expiration AND yt2.resource_owner = yt.resource_owner AND yt2.client_key = yt.client_key);
DELETE FROM oauth_token WHERE token IN (SELECT id FROM oauth_token_temp);
DROP TABLE oauth_token_temp;
--
-- Adding an artificial primary key to oauth_token
--
ALTER TABLE oauth_token ADD UNIQUE (token);
ALTER TABLE oauth_token DROP PRIMARY KEY;
ALTER TABLE oauth_token ADD COLUMN otk_token_id varchar(128);
--
-- Create an initial value for the new primary key
UPDATE oauth_token AS t1,(SELECT token FROM oauth_token) AS t2 SET t1.otk_token_id = t2.token WHERE t1.token = t2.token;
--
-- Create the new primary key oauth_token
ALTER TABLE oauth_token ADD PRIMARY KEY (otk_token_id);
--
-- Change expiration dates to be in seconds rather than milliseconds
UPDATE oauth_client_key SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
UPDATE oauth_initiate SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
UPDATE oauth_token SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
UPDATE oauth_token SET rexpiration = rexpiration/1000 WHERE rexpiration >= 1000000000000;
UPDATE oauth_session SET expiration = expiration/1000 WHERE expiration >= 1000000000000;
--
-- Updating the version to otk1.1
--
UPDATE otk_version SET current_version='otk2.0';
--
-- Adding an index to oauth_token. This will improve the overall performance
CREATE INDEX oat_idx_expiration ON oauth_token (expiration);
--
-- Drop an existing foreign key. This will be handled in polcy when ever the client_name gets updated
ALTER TABLE oauth_client_key DROP FOREIGN KEY ock_fk_clientName;
--
COMMIT;