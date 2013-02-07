package com.l7tech.server.jdbc;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author ghuang
 */
public class JdbcQueryingManagerStub implements JdbcQueryingManager {

    private MockJdbcDatabaseManager mockJdbcDatabaseManager;
    private Object mockResults = null;

    public JdbcQueryingManagerStub(MockJdbcDatabaseManager mockJdbcDatabaseManager) {
        this.mockJdbcDatabaseManager = mockJdbcDatabaseManager;
    }

    public void setMockResults(Object object1) {
        this.mockResults = object1;
    }

    /**
     * Get a result set (SqlRowSet object) from the mock JDBC database.
     *
     * @return a SqlRowSet object containing mock data.
     */
    @Override
    public SqlRowSet getMockSqlRowSet() {
        return mockJdbcDatabaseManager.getMockSqlRowSet();
    }

    @Override
    public Object performJdbcQuery(String connectionName, String query, String schema, int maxRecords, List<Object> preparedStmtParams) {
        return mockResults;
    }

    @Override
    public Object performJdbcQuery(DataSource dataSource, String query, String schema, int maxRecords, List<Object> preparedStmtParams) {
        return mockResults;
    }
}
