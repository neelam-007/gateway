package com.l7tech.policy.assertion;

/**
 * this interface to work with any datasources such as JDBC, Cassandra, Mongo, etc.
 * Copyright: CA Technologies, 2014
 * User: ymoiseyenko
 */
public interface Connectionable {
    String getConnectionName();
    void setConnectionName(String connectionName);
}
