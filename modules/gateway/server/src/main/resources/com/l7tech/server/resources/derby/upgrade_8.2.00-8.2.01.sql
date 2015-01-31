--
-- Script to update derby ssg database from 8.2.00 to 8.2.01
--
-- Layer 7 Technologies, inc
--

-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.2.01';

