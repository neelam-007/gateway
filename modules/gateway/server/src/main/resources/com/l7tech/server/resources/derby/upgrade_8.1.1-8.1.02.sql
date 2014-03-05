--
-- Script to update derby ssg database from 8.0.0 to 8.1.0
--
-- Layer 7 Technologies, inc
--

-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.1.02';

