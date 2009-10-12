package com.l7tech.policy.assertion;

/**
 * @author ghuang
 */
public interface JdbcConnectionable {
    String getConnectionName();
    void setConnectionName(String connectionName);
}
