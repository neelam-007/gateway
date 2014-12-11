package com.ca.datasources.cassandra;

import com.datastax.driver.core.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraQueryManager {

    private static final Logger logger =  Logger.getLogger(CassandraQueryManager.class.getName());

    /**
     * builds cassandra prepared statement
     * @param session session to be used
     * @param queryString refined query string
     * @return prepared statement
     */
     public PreparedStatement buildPreparedStatement(Session session, String queryString) {
          if(session == null) {
              logger.log(Level.WARNING, "Unable to build PreparedStatement: session is null");
              return null;
          }

          if(queryString == null) {
              logger.log(Level.WARNING, "Unable to build PreparedSatement: query string is null");
              return null;
          }
          return session.prepare(queryString);
     }

    public BoundStatement buildBoundStatement(PreparedStatement preparedStatement,  List<Object> preparedStmtParams) {
        if(preparedStatement == null) {
            logger.log(Level.WARNING, "Unable to build BoundStatement: PreparedStatement is null!");
            return null;
        }
        //construct BoundStatement
        BoundStatement boundStatement = new BoundStatement(preparedStatement);

        List<ColumnDefinitions.Definition> cdlist = boundStatement.preparedStatement().getVariables().asList();
        if(cdlist.size() > 0 ) {
            if(preparedStmtParams == null) {
                logger.log(Level.WARNING, "Missing PreparedStatement parameters");
            }
            else if(preparedStmtParams.size() < cdlist.size()) {
                logger.log(Level.WARNING, "PreparedStatement '" + preparedStatement.getQueryString() + "' expects fewer parameters: expected=" + cdlist.size() + ",  actual=" + preparedStmtParams.size());
            }
            else if(preparedStmtParams.size() > cdlist.size()) {
                logger.log(Level.WARNING, "PreparedStatement '" + preparedStatement.getQueryString() + "' expects more parameters: expected=" + cdlist.size() + ",  actual=" + preparedStmtParams.size());
            }
            else {
                List<Object> convertedStmtParams = new ArrayList<>();
                for(int i = 0; i < cdlist.size(); i++){
                    convertedStmtParams.add(CassandraUtil.javaType2CassandraDataType(cdlist.get(i), preparedStmtParams.get(i)));
                }
                boundStatement.bind(convertedStmtParams.toArray());
            }
        }

        return boundStatement;
    }

    public final int executeStatement(final Session session, final PreparedStatement stmt, final List<Object> preparedStmtParams, final Map<String, List<Object>> resultMap) {
        BoundStatement boundStatement = buildBoundStatement(stmt, preparedStmtParams);
        return executeStatement(session, boundStatement, resultMap);
    }

    public final int executeStatement(final Session session, final BoundStatement boundStatement, final Map<String, List<Object>> resultMap) {
        int rowCount = 0;
        ResultSetFuture result = session.executeAsync(boundStatement);
        ResultSet rows = result.getUninterruptibly();

        Iterator<Row> resultSetIterator = rows.iterator();
        // Get resultSet into map
        while(resultSetIterator.hasNext()){
            Row row = resultSetIterator.next();
            for(ColumnDefinitions.Definition definition: row.getColumnDefinitions()){
                List<Object> col  = resultMap.get(definition.getName());

                if (col == null){
                    col = new ArrayList();
                    resultMap.put(definition.getName(),col);
                }

                Object o = CassandraUtil.cassandraDataType2JavaType(definition, row);
                col.add(o);
            }
            rowCount++;
        }
        return rowCount;
    }


}
