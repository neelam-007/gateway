package com.ca.datasources.cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 12/15/14
 */
public interface CassandraQueryManager {
    PreparedStatement buildPreparedStatement(Session session, String queryString);

    BoundStatement buildBoundStatement(PreparedStatement preparedStatement, List<Object> preparedStmtParams);

    int executeStatement(Session session, BoundStatement boundStatement, Map<String, List<Object>> resultMap, int maxRecords, long queryTimeout) throws TimeoutException;

    void testQuery(Session session, String queryString, long timeout) throws Exception;
}
