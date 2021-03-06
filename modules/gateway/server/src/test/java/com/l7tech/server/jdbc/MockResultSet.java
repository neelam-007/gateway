package com.l7tech.server.jdbc;

import com.l7tech.util.CollectionUtils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is used to mock a result set returned from a database.
 *
 * @author Victor Kazakov
 */
public class MockResultSet implements ResultSet {

    private List<Map<String, Object>> results;
    private List<String> columns;
    private int index = -1;
    private boolean throwExceptionOnNull;

    public MockResultSet(List<Map<String, Object>> results) {
        this(results, false);
    }

    /**
     * @param results              The rows to return
     * @param throwExceptionOnNull If true it will throw an exception for non existent values. Otherwise it will just return null.
     */
    public MockResultSet(List<Map<String, Object>> results, boolean throwExceptionOnNull) {
        this.results = results;
        this.columns = results.isEmpty()? Collections.<String>emptyList(): CollectionUtils.toList(results.get(0).keySet());
        this.throwExceptionOnNull = throwExceptionOnNull;
    }

    @Override
    public boolean next() throws SQLException {
        if (results.size() > index + 1) {
            index++;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws SQLException {
        index = -1;
    }

    @Override
    public boolean wasNull() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof String) {
            return (String) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be String is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            if (!throwExceptionOnNull && value == null) return false;
            throw new SQLException(columnLabel + " expected to be Boolean is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Byte) {
            return (Byte) value;
        } else {
            if (!throwExceptionOnNull && value == null) return 0;
            throw new SQLException(columnLabel + " expected to be Byte is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Short) {
            return (Short) value;
        } else {
            if (!throwExceptionOnNull && value == null) return 0;
            throw new SQLException(columnLabel + " expected to be Short is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            if (!throwExceptionOnNull && value == null) return 0;
            throw new SQLException(columnLabel + " expected to be Integer is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Long) {
            return (Long) value;
        } else {
            if (!throwExceptionOnNull && value == null) return 0;
            throw new SQLException(columnLabel + " expected to be Long is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Float) {
            return (Float) value;
        } else {
            if (!throwExceptionOnNull && value == null) return 0;
            throw new SQLException(columnLabel + " expected to be Float is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Double) {
            return (Double) value;
        } else {
            if (!throwExceptionOnNull && value == null) return 0;
            throw new SQLException(columnLabel + " expected to be Double is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be BigDecimal is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof byte[]) {
            return (byte[]) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be byte[] is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Date) {
            return (Date) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be Date is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Time) {
            return (Time) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be Time is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be Timestamp is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof InputStream) {
            return (InputStream) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be InputStream is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof InputStream) {
            return (InputStream) value;
        } else {
            throw new SQLException(columnLabel + " expected to be InputStream is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof InputStream) {
            return (InputStream) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be InputStream is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCursorName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new MockResultSetMetaData();
    }

    private String getColumnName(int columnIndex){
        return columns.get(columnIndex-1);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return results.get(index).get(getColumnName(columnIndex));
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return results.get(index).get(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof Reader) {
            return (Reader) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be Reader is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        Object value = results.get(index).get(columnLabel);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else {
            if (!throwExceptionOnNull && value == null) return null;
            throw new SQLException(columnLabel + " expected to be BigDecimal is incorrect type: " + (value != null ? value.getClass() : "null"));
        }
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean first() throws SQLException {
        index = -1;
        return true;
    }

    @Override
    public boolean last() throws SQLException {
        index = results.size()-1;
        return true;
    }

    @Override
    public int getRow() throws SQLException {
        return index +1;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean previous() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFetchSize() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getConcurrency() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Statement getStatement() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }


    public class MockResultSetMetaData implements ResultSetMetaData{

        @Override
        public int getColumnCount() throws SQLException {
            return columns.size();
        }

        @Override
        public boolean isAutoIncrement(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCaseSensitive(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSearchable(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCurrency(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int isNullable(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSigned(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getColumnDisplaySize(int column) throws SQLException {
            return columns.get(column-1).length();
        }

        @Override
        public String getColumnLabel(int column) throws SQLException {
            return columns.get(column-1);
        }

        @Override
        public String getColumnName(int column) throws SQLException {
            return columns.get(column-1);
        }

        @Override
        public String getSchemaName(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getPrecision(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getScale(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getTableName(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCatalogName(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getColumnType(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getColumnTypeName(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadOnly(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWritable(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDefinitelyWritable(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getColumnClassName(int column) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
