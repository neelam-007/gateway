--
-- Script to update mysql ssg database from 4.3 to 4.4
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

-- New columns for JMS Endpoints
ALTER TABLE jms_endpoint ADD reply_to_queue_name varchar(128);
ALTER TABLE jms_endpoint ADD outbound_message_type varchar(128);
ALTER TABLE jms_endpoint ADD disabled tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE jms_endpoint ADD use_message_id_for_correlation tinyint(1) NOT NULL DEFAULT 0;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;
