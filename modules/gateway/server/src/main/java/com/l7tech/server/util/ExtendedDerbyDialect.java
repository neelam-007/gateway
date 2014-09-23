package com.l7tech.server.util;

import org.hibernate.dialect.DerbyDialect;

import java.sql.Types;

/**
 * Dialect to fix the following issue:
 *
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-6205
 */
public class ExtendedDerbyDialect extends DerbyDialect {

    @Override
    public String getSelectClauseNullString(int sqlType) {
        final int fixedSqlType;
        if (Types.CLOB == sqlType) {
            fixedSqlType = Types.VARCHAR;
        } else {
            fixedSqlType = sqlType;
        }
        return super.getSelectClauseNullString(fixedSqlType);
    }

}
