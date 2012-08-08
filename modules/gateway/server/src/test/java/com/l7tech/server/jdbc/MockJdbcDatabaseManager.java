package com.l7tech.server.jdbc;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSetMetaData;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * The manager class builds a mock database containing an employee info table with four column labels ("name", "department", "sex", and "age") and twelve records.
 *
 * @author ghuang
 */
public class MockJdbcDatabaseManager {
    public final static int MOCK_MAX_ROWS = 12;
    public final static int MOCK_MAX_COLS = 4;

    public final static String[] MOCK_COLUMN_NAMES = new String[] {"name", "department", "sex", "age"};
    public final static String[] MOCK_NAMES = new String[] {"John", "Darcy", "Alice", "Mary", "Oliver", "Bob", "Ahmad", "Amy", "Carmen", "David", "Carlo", "Kan"};
    public final static String[] MOCK_DEPTS = new String[] {"Production", "Sales", "Admin", "Production", "Marketing", "Development", "Development", "Admin", "Sales", "Sales", "Marketing", "Production"};
    public final static String[] MOCK_SEXES = new String[] {"M", "M", "F", "F", "M", "M", "M", "F", "F", "M", "M", "M"};
    public final static String[] MOCK_AGES = new String[] {"45", "26", "22", "32", "50", "40", "38", "19", "25", "28", "37", "29"};

    public final static List<Map<String, String>> MOCK_DATA = new ArrayList<Map<String, String>>(MOCK_MAX_ROWS);

    static {
        for (int i = 0; i < MOCK_MAX_ROWS; i++) {
            Map<String, String> oneRowData = new HashMap<String, String>(MOCK_MAX_COLS);
            for (int j = 0; j < MOCK_MAX_COLS; j++) {
                switch (j) {
                    case 0:
                        oneRowData.put(MOCK_COLUMN_NAMES[j], MOCK_NAMES[i]);
                        break;
                    case 1:
                        oneRowData.put(MOCK_COLUMN_NAMES[j], MOCK_DEPTS[i]);
                        break;
                    case 2:
                        oneRowData.put(MOCK_COLUMN_NAMES[j], MOCK_SEXES[i]);
                        break;
                    case 3:
                        oneRowData.put(MOCK_COLUMN_NAMES[j], MOCK_AGES[i]);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid column index");
                }
            }

            MOCK_DATA.add(oneRowData);
        }
    }

    public SqlRowSet getMockSqlRowSet() {
        return new MockSqlRowSet();
    }

    public static class MockMetaData extends ResultSetWrappingSqlRowSetMetaData {
        public MockMetaData(ResultSetMetaData resultSetMetaData) {
            super(resultSetMetaData);
        }

        @Override
        public int getColumnCount() throws InvalidResultSetAccessException {
            return MOCK_COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) throws InvalidResultSetAccessException {
            return MOCK_COLUMN_NAMES[column-1];
        }
    }

    public static class MockSqlRowSet implements SqlRowSet {
        private int cursor = 0;

        @Override
        public SqlRowSetMetaData getMetaData() {
            return new MockMetaData(null);
        }

        @Override
        public boolean next() throws InvalidResultSetAccessException {
            return cursor++ < MOCK_MAX_ROWS;
        }

        @Override
        public Object getObject(String s) throws InvalidResultSetAccessException {
            if (cursor > MOCK_MAX_ROWS) throw new IllegalStateException("No data can be accessed.");

            return MOCK_DATA.get(cursor-1).get(s);
        }

        @Override
        public int findColumn(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("findColumn not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public BigDecimal getBigDecimal(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBigDecimal not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public BigDecimal getBigDecimal(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBigDecimal not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean getBoolean(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBoolean not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean getBoolean(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBoolean not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public byte getByte(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getByte not implemented in MockJdbcDatabaseManager.");

        }

        @Override
        public byte getByte(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getByte not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Date getDate(int i, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Date getDate(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Date getDate(String s, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Date getDate(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public double getDouble(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDouble not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public double getDouble(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDouble not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public float getFloat(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getFloat not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public float getFloat(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getFloat not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public int getInt(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getInt not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public int getInt(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getInt not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public long getLong(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getLong not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public long getLong(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getLong not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Object getObject(int i, Map map) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getObject not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Object getObject(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getObject not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Object getObject(String s, Map map) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getObject not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public short getShort(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getShort not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public short getShort(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getShort not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public String getString(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getString not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public String getString(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getString not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Time getTime(int i, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Time getTime(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Time getTime(String s, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Time getTime(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Timestamp getTimestamp(int i, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Timestamp getTimestamp(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Timestamp getTimestamp(String s, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public Timestamp getTimestamp(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean absolute(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("absolute not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public void afterLast() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("afterLast not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public void beforeFirst() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("beforeFirst not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean first() throws InvalidResultSetAccessException {
            cursor = 0;
            return true;
        }

        @Override
        public int getRow() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getRow not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean isAfterLast() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isAfterLast not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean isBeforeFirst() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isBeforeFirst not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean isFirst() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isFirst not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean isLast() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isLast not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean last() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("last not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean previous() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("previous not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean relative(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("relative not implemented in MockJdbcDatabaseManager.");
        }

        @Override
        public boolean wasNull() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("wasNull not implemented in MockJdbcDatabaseManager.");
        }
    }
}
