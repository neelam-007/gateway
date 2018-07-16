package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.server.jdbc.JdbcQueryingManager;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcQueryingManagerBuilder {
    private final JdbcQueryingManager jdbcQueryingManager;

    JdbcQueryingManagerBuilder() {
        jdbcQueryingManager = mock(JdbcQueryingManager.class);
    }

    JQMStubber whenPerformJdbcQuery(String connName, String plainQuery, String schema,
                                        int maxRecords, int queryTimeout, List<Object> preparedStmtParams) {
        return new JQMStubber(connName, plainQuery, schema, maxRecords, queryTimeout, preparedStmtParams);
    }

    JdbcQueryingManager build() {
        return jdbcQueryingManager;
    }

    class JQMStubber {
        final private String connName;
        final private String plainQuery;
        final private String schema;
        final private int maxRecords;
        final private int queryTimeout;
        final private List<Object> preparedStmtParams;

        JQMStubber(String connName, String plainQuery, String schema, int maxRecords, int queryTimeout,
                   List<Object> preparedStmtParams) {
            this.connName = connName;
            this.plainQuery = plainQuery;
            this.schema = schema;
            this.maxRecords = maxRecords;
            this.queryTimeout = queryTimeout;
            this.preparedStmtParams = preparedStmtParams;
        }

        @SuppressWarnings("unchecked")
        JdbcQueryingManagerBuilder thenLazyReturn(final ListBuilder<SqlRowSet> result) {
            when((List<SqlRowSet>) jdbcQueryingManager.performJdbcQuery(connName, plainQuery, schema, maxRecords,
                    queryTimeout, preparedStmtParams)).then(new Answer<List<SqlRowSet>>() {
                        @Override
                        public List<SqlRowSet> answer(InvocationOnMock invocationOnMock) throws Throwable {
                            return result.build();
                        }
                    }
            );
            return JdbcQueryingManagerBuilder.this;
        }

        JdbcQueryingManagerBuilder thenReturn(Object object) {
            when(jdbcQueryingManager.performJdbcQuery(connName, plainQuery, schema, maxRecords, queryTimeout,
                    preparedStmtParams)).thenReturn(object);
            return JdbcQueryingManagerBuilder.this;
        }
    }
}
