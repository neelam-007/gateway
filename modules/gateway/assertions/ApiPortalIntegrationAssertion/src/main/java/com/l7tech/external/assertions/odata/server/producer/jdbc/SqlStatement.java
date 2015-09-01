package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.odata4j.core.ImmutableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlStatement {
    public static class SqlParameter {
        public final Object value;
        public final Integer sqlType;

        public SqlParameter(Object value, Integer sqlType) {
            this.value = value;
            this.sqlType = sqlType;
        }
    }

    public final String sql;
    public final Integer maxRows;
    public final ImmutableList<SqlParameter> params;

    public SqlStatement(String sql, ImmutableList<SqlParameter> params) {
        this.sql = sql;
        this.params = params;
        this.maxRows = null;
    }

    public SqlStatement(String sql, ImmutableList<SqlParameter> params, Integer maxRows) {
        this.sql = sql;
        this.params = params;
        this.maxRows = maxRows;
    }

    public PreparedStatement asPreparedStatement(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        if (maxRows != null && maxRows.intValue() > 0) {
            stmt.setMaxRows(maxRows.intValue());
        }
        for (int i = 0; i < params.size(); i++) {
            SqlParameter p = params.get(i);
            if (p.sqlType == null) {
                stmt.setObject(i + 1, p.value);
            } else {
                stmt.setObject(i + 1, p.value, p.sqlType);
            }
        }
        return stmt;
    }

}