--
-- Script to update mysql ssg database from 5.4.1 to 5.4.2
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version
--

UPDATE ssg_version SET current_version = '6.0.0';

ALTER TABLE jms_endpoint ADD COLUMN request_max_size bigint NOT NULL default -1 AFTER use_message_id_for_correlation;

-- Per-host outbound TLS cipher suite configuration (enhancement #9939)
ALTER TABLE http_configuration ADD COLUMN tls_cipher_suites varchar(4096) DEFAULT NULL;


--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

