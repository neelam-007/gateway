--
-- Script to verify if update from 7.1.0 to 7.1.1 was successful
--
-- Layer 7 Technologies, inc
--
-- The last statement must be a select statement


SELECT *
	FROM
		sys.systables t, sys.syscolumns c, sys.sysconstraints con
	WHERE
		c.referenceid = t.tableid
		AND t.tabletype = 'T'
		AND t.tablename = 'AUDIT_MESSAGE'
		AND c.columnname = 'MAPPING_VALUES_OID'
		AND con.tableid = t.tableid
		AND con.constraintname = 'MESSAGE_CONTEXT_MAPPING';
