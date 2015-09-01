package com.l7tech.external.assertions.odata.server.producer.jdbc;

public interface JdbcProducerCommandContext {

    Jdbc getJdbc();

    JdbcProducerBackend getBackend();

    <T> T get(Class<T> instanceType);

}
