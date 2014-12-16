package com.ca.datasources.cassandra;

import com.datastax.driver.core.*;

import java.util.List;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 12/15/14
 */
public class CassandraQueryManagerStub extends CassandraQueryManagerImpl implements CassandraQueryManager{

    PreparedStatement preparedStatement;
    BoundStatement boundStatement;

    public CassandraQueryManagerStub() {

    }

    @Override
    public PreparedStatement buildPreparedStatement(Session session, String queryString) {
        return preparedStatement;
    }

    @Override
    public BoundStatement buildBoundStatement(PreparedStatement preparedStatement, List<Object> preparedStmtParams) {
        return boundStatement;
    }


    public void setPreparedStatement(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
    }

    public void setBoundStatement(BoundStatement boundStatement) {
        this.boundStatement = boundStatement;
    }
}
