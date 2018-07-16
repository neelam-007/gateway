package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.server.ServerJdbcQueryAssertion.CaseInsensitiveString;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.util.*;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqlRowSetBuilder {

    private final SqlRowSet rowSet;
    private final SqlRowSetMetaData metaData;
    private List<String> columns;
    private List<Map<CaseInsensitiveString, Object>> rows;
    private int currentRow;

    SqlRowSetBuilder() {
        rowSet = mock(SqlRowSet.class);
        metaData = mock(SqlRowSetMetaData.class);
        columns = new ArrayList<>();
        rows = new ArrayList<>();
        currentRow = -1;
    }

    SqlRowSetBuilder withColumns(String... columns) {
        this.columns = Arrays.asList(columns);
        return this;
    }

    /**
     * Adds rows to be returned by invocations of {@link org.springframework.jdbc.support.rowset.SqlRowSet#getObject(int)}
     * and similar methods.
     * Pass objects IN THE SAME ORDER as you defined your columns.
     * If you don't pass in the exact same number of objects as you defined in your columns (
     * i.e. the number of times you called {@link #withColumns(String...)}, the method will throw an exception.
     * @param row a varargs array containing the objects for a single row of your "query",
     *            in the same order the columns were defined.
     * @return 'this', like any other Builder
     * @throws IllegalArgumentException if the number of objects you pass in doesn't match the number of columns
     */
    SqlRowSetBuilder addRow(Object... row) {
        if (row.length != columns.size()) {
            throw new IllegalArgumentException("Your row has " + row.length + " columns, but you declared " + columns.size());
        }
        Map<CaseInsensitiveString, Object> newRow = new HashMap<>();
        rows.add(newRow);
        for (int i = 0; i < row.length; i++) {
            newRow.put(new CaseInsensitiveString(columns.get(i)), row[i]);
        }
        return this;
    }

    SqlRowSet build() {
        when(metaData.getColumnCount()).thenReturn(columns.size());

        when(metaData.getColumnName(anyInt())).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                Integer i = (Integer) invocationOnMock.getArguments()[0];
                return columns.get(i - 1); // columns are 1-based in this API... go figure
            }
        });

        when(rowSet.getMetaData()).thenReturn(metaData);

        when(rowSet.first()).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                currentRow = -1;
                return true;
            }
        });

        when(rowSet.next()).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (currentRow >= rows.size() - 1) {
                    return false;
                }
                currentRow++;
                return true;
            }
        });

        when(rowSet.getObject(anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String columnName = (String) invocationOnMock.getArguments()[0];
                return rows.get(currentRow).get(new CaseInsensitiveString(columnName.toUpperCase()));
            }
        });

        return rowSet;
    }
}

