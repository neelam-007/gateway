package com.datastax.driver.core;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/27/14
 */
public class MockPreparedId extends PreparedId {
    public MockPreparedId(MD5Digest id, ColumnDefinitions metadata, ColumnDefinitions resultSetMetadata, int[] routingKeyIndexes, ProtocolVersion protocolVersion) {
        super(id, metadata, resultSetMetadata, routingKeyIndexes, protocolVersion);
    }
}
