package com.l7tech.server.jdbc;

import java.util.List;

/**
 * @author ghuang
 */
public class JdbcQueryingManagerStub implements JdbcQueryingManager {

    @Override
    public Object performJdbcQuery(String connectionName, String query, int maxRecords, List<Object> preparedStmtParams) {
        return null;
    }
}
