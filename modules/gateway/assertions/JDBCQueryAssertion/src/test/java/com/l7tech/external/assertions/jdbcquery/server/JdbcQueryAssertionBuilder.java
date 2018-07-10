package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcQueryAssertionBuilder {

    private final JdbcQueryAssertion assertion;

    JdbcQueryAssertionBuilder() {
        assertion = mock(JdbcQueryAssertion.class);
    }

    JdbcQueryAssertionBuilder withVariables(String[] variables) {
        when(assertion.getVariablesUsed()).thenReturn(variables);
        return this;
    }

    JdbcQueryAssertionBuilder withConnectionName(String connectionName) {
        when(assertion.getConnectionName()).thenReturn(connectionName);
        return this;
    }

    JdbcQueryAssertionBuilder withSqlQuery(String sqlQuery) {
        when(assertion.getSqlQuery()).thenReturn(sqlQuery);
        return this;
    }

    JdbcQueryAssertionBuilder withSchema(String schema) {
        when(assertion.getSchema()).thenReturn(schema);
        return this;
    }

    JdbcQueryAssertionBuilder withMaxRecords(int maxRecords) {
        when(assertion.getMaxRecords()).thenReturn(maxRecords);
        return this;
    }

    JdbcQueryAssertionBuilder withQueryTimeout(String timeout) {
        when(assertion.getQueryTimeout()).thenReturn(timeout);
        return this;
    }

    JdbcQueryAssertionBuilder withSaveResultsAsContextVariables(boolean save) {
        when(assertion.isSaveResultsAsContextVariables()).thenReturn(save);
        return this;
    }

    JdbcQueryAssertionBuilder withNamingMap(Map<String, String> map) {
        when(assertion.getNamingMap()).thenReturn(map);
        return this;
    }

    JdbcQueryAssertionBuilder withVariablePrefix(String prefix) {
        when(assertion.getVariablePrefix()).thenReturn(prefix);
        return this;
    }

    JdbcQueryAssertionBuilder withGenerateXmlResult(boolean generate) {
        when(assertion.isGenerateXmlResult()).thenReturn(generate);
        return this;
    }

    JdbcQueryAssertionBuilder withAssertionFailureEnabled(boolean enabled) {
        when(assertion.isAssertionFailureEnabled()).thenReturn(enabled);
        return this;
    }

    JdbcQueryAssertion build() {
        return assertion;
    }
}
