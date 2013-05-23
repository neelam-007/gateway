--
-- Script to verify if update from 7.1.0 to 7.1.1 was successful
--
-- Layer 7 Technologies, inc
--
-- The last statement must be a select statement
-- Use <db_name> to be replaced by the database name at runtime

--
-- Reenable FK 
--
SET FOREIGN_KEY_CHECKS=1;

SELECT *
FROM
	information_schema.table_constraints con, information_schema.columns c
WHERE
	c.table_schema='<db_name>'
	AND c.table_name='audit_message'
	AND c.column_name = 'mapping_values_oid'
	AND con.table_schema='<db_name>'
	AND con.table_name='audit_message'
	AND con.constraint_name='message_context_mapping';
