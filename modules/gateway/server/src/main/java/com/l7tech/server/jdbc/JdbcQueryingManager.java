package com.l7tech.server.jdbc;

import java.util.List;

/**
 * @author ghuang
 */
public interface JdbcQueryingManager {

    Object performJdbcQuery(String connectionName, String query, int maxRecords, List<Object> preparedStmtParams);
}
