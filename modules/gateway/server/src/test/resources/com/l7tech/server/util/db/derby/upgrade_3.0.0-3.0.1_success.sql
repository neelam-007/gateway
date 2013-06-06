-- --------------------------------------------------------------------------
-- Test script for upgrading from 2.0.0 to 3.0.0
-- --------------------------------------------------------------------------

-- select * from ssg_version;


SELECT *
	FROM
		sys.systables t, sys.sysschemas s
	WHERE
		t.schemaid = s.schemaid
		AND t.tabletype = 'T'
		AND t.tablename = 'SSG_VERSION';