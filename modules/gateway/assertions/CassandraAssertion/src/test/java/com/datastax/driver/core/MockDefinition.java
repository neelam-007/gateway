package com.datastax.driver.core;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 26/03/14
 * Time: 12:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class MockDefinition extends ColumnDefinitions.Definition {

    public MockDefinition(String keyspace, String table, String name, DataType dataType) {
        super(keyspace, table, name, dataType);
    }

}
