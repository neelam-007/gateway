/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 20, 2009
 * Time: 12:49:59 PM
 */
package com.l7tech.gateway.standardreports;

import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The use of this object is not very natural as it's a minimum change injection into current implementation of
 * reports. Currently each report manages and creates all the parameters it requires in it's parameters section.
 * As a result this object will be created outside of the report, passed into the report, when it's configuration
 * will be completed by calling configure()
 */
public class PreparedStatementDataSource implements JRDataSource {

    public PreparedStatementDataSource(final Connection conn) {
        this.conn = conn;
        initFieldNameToColumnMap();
        this.subReportInstances = new ArrayList<PreparedStatementDataSource>();
        isMasterDataSource = true;
    }

    /**
     * Constructor for subreport instances
     *
     * @param parent the parent data source from which the new instance will get it's java.sql.Connection
     */
    private PreparedStatementDataSource(final PreparedStatementDataSource parent) {
        this.conn = parent.getConn();
        initFieldNameToColumnMap();
        this.subReportInstances = null;
        isMasterDataSource = false;
    }

    /**
     * Get an instance of PreparedStatementDataSource for use in a sub report
     *
     * @return a data source which still needs to have configure() called
     */
    public PreparedStatementDataSource getSubReportInstance() {
        // Note: if you want to find usages of this method, you need to search based on text as its used from within
        // jrxml files

        final PreparedStatementDataSource psds = new PreparedStatementDataSource(this);
        subReportInstances.add(psds);
        return psds;
    }

    /**
     * Convenience method for reports, which will have a Pair&lt;String, List&lt;Object&gt;&gt;
     *
     * @param sqlAndParamsPair the pair containing the sql and the list of parameters
     * @return true, the value does not indiciate anything other than the method completed successfully. It never
     *         returns false
     * @throws SQLException
     */
    public Boolean configure(final Pair<String, List<Object>> sqlAndParamsPair) throws SQLException {
        return configure(sqlAndParamsPair.getKey(), sqlAndParamsPair.getValue());
    }

    /**
     * configure is called exclusively from within reports. Each report is passed an instance of this class via
     * a parameter. The report has all the parameters required to create the parameters required for configure(). The
     * report should call this method as the last parameter in it's configuration. This method returns a boolean so
     * that an assignment can be made, making it easy to call from within reports.
     * <p/>
     * After configure has been called, next() can be called to start retrieveing data
     *
     * @param sql    the paramaterized sql string
     * @param params the list of parameters to supply to the prepared statement
     * @return true, the value does not indiciate anything other than the method completed successfully. It never
     *         returns false
     * @throws SQLException
     * @throws IllegalStateException    if this data source has already been executed
     * @throws IllegalArgumentException if either, sql is null or empty, or colummNames is null or empty
     * @throws IllegalStateException    if the number of parameters does not match the number of ?'s in the sql string
     * @throws NullPointerException     if params is null
     */
    public Boolean configure(final String sql, final List<Object> params) throws SQLException {
        if (hasExecuted) throw new IllegalStateException("DataSource cannot be configured after it has been executed");
        if (sql == null || sql.equals("")) throw new IllegalArgumentException("Sql cannot be null or the emtpy String");
        if (params == null) throw new NullPointerException("params must be specified. It can be empty");
        int numQuestionMarks = getNumQuestionMarks(sql);
        if (numQuestionMarks != params.size())
            throw new IllegalStateException("The number of replaceable parameters in " +
                    "the String sql must match the length of params");

        preparedStatement = conn.prepareStatement(sql);
        //replace all sql ? with their param
        for (int i = 0; i < params.size(); i++) {
            final Object o = params.get(i);
            final String className = o.getClass().getName();
            SupportedType st;
            try {
                st = SupportedType.getType(className);
            } catch (UnsupportedTypeExcetpion unsupportedTypeExcetpion) {
                close();
                throw new IllegalStateException(unsupportedTypeExcetpion.getMessage());
            }
            //this switch has not accidently left out double. We dont need it yet
            switch (st) {
                case JAVA_LANG_INTEGER:
                    preparedStatement.setInt(i + 1, (Integer) o);
                    break;
                case JAVA_LANG_LONG:
                    preparedStatement.setLong(i + 1, (Long) o);
                    break;
                case JAVA_LANG_STRING:
                    preparedStatement.setString(i + 1, (String) o);
                    break;
                default:
                    close();
                    throw new IllegalArgumentException("Params contains a parameter of type '" + st.getName() + "' " +
                            "which is currently not supported");
            }
        }
        return true;//this is only to avoid hacking jrxml file, need a return value for assignment
    }

    public Object getFieldValue(final JRField jrField) throws JRException {
        //only testing for rs here because its used below
        if (rs == null) {
            close();
            throw new IllegalStateException("Resultset is closed");
        }

        final String name = jrField.getName();
        if (!fieldNameToColumnName.containsKey(name)) {
            close();
            throw new IllegalStateException("Column '" + name + "' unknown");
        }

        ColumnName fc = fieldNameToColumnName.get(name);
        final SupportedType st = fc.getType();

        try {
            switch (st) {
                case JAVA_LANG_INTEGER:
                    return rs.getInt(fc.getColumnName());
                case JAVA_LANG_DOUBLE:
                    return rs.getDouble(fc.getColumnName());
                case JAVA_LANG_LONG:
                    return rs.getLong(fc.getColumnName());
                case JAVA_LANG_STRING:
                    return rs.getString(fc.getColumnName());
                default:
                    //can't happen due to previous checks
                    close();
                    throw new IllegalStateException("Unsupported type: " + st.getName());
            }
        } catch (SQLException e) {
            close();
            throw new JRException(e.getMessage());
        }
    }

    /**
     * The first call to next() will execute the underlying query and will automatically move onto the first row.
     * Client code does not need to worry about this, once configure() has been called, next() can be called to
     * start retrieving data.
     * <p/>
     * next() calls next on the underlying ResultSet. When rs.next() returns false, we close both the result set
     * and the PreparedStatement, so client code does not need to explicitly close
     *
     * @return true if this data source has another row of data
     * @throws JRException
     */
    public boolean next() throws JRException {
        if (!hasExecuted) {
            try {
                //execute the prepared statement
                rs = preparedStatement.executeQuery();
                hasExecuted = true;
            } catch (SQLException e) {
                close();
                throw new JRException(e.getMessage());
            }
        }
        boolean isNext = false;
        try {
            isNext = rs.next();
            return isNext;
        } catch (SQLException e) {
            throw new JRException(e.getMessage());
        } finally {
            //isNext here captures 2 cases 1) rs.next throws an exception 2) rs.next returns false
            if (!isNext) {
                //close all resources
                close();
            }
        }
    }

    /**
     * next() will close all resources when the resultset is exhausted, however this only happens when the result
     * set is exhausted. Client code if possible should still call close to ensure resources are closed immediately,
     * instead of waiting for the underlying Connection to be closed
     */
    public void close() {
        //close may be called more than once, so no point proceeding if already closed
        if (closed) return;

        ResourceUtils.closeQuietly(rs);
        ResourceUtils.closeQuietly(preparedStatement);
        rs = null;
        preparedStatement = null;
        closed = true;

        if (isMasterDataSource) {
            //Make sure resources from all subreports are closed
            //this is only in case sub reports have any undiscovered strange behaviour as their data sources
            //should be closed automatically either via an exception or as a result of their result set exhausting
            for (PreparedStatementDataSource psds : subReportInstances) {
                psds.close();
            }
        }
    }

    private int getNumQuestionMarks(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '?') count++;
        }
        return count;
    }

    /**
     * Convenience to know if close() has been previously closed
     *
     * @return true if close() has been called, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    private Connection getConn() {
        return conn;
    }

    private void initFieldNameToColumnMap() {
        ColumnName[] allNames = ColumnName.values();
        for (ColumnName cn : allNames) {
            fieldNameToColumnName.put(cn.getColumnName(), cn);
        }
    }

    /**
     * ColumnName represents every string value which can be returned from any of the standard reports
     */
    public enum ColumnName {
        AUTHORIZED("AUTHORIZED", SupportedType.JAVA_LANG_DOUBLE),//todo [Donal] should be long
        ATTEMPTED("ATTEMPTED", SupportedType.JAVA_LANG_DOUBLE),//todo [Donal] should be long
        FRONT_SUM("FRONT_SUM", SupportedType.JAVA_LANG_DOUBLE),//todo [Donal] should be long
        BACK_SUM("BACK_SUM", SupportedType.JAVA_LANG_DOUBLE),//todo [Donal] should be long
        COMPLETED("COMPLETED", SupportedType.JAVA_LANG_DOUBLE),//todo [Donal] should be long
        THROUGHPUT("THROUGHPUT", SupportedType.JAVA_LANG_DOUBLE),//todo [Donal] should be long
        POLICY_VIOLATIONS("POLICY_VIOLATIONS", SupportedType.JAVA_LANG_LONG),
        ROUTING_FAILURES("ROUTING_FAILURES", SupportedType.JAVA_LANG_LONG),
        USAGE_SUM("USAGE_SUM", SupportedType.JAVA_LANG_LONG),
        SERVICE_ID("SERVICE_ID", SupportedType.JAVA_LANG_STRING),
        SERVICE_NAME("SERVICE_NAME", SupportedType.JAVA_LANG_STRING),
        ROUTING_URI("ROUTING_URI", SupportedType.JAVA_LANG_STRING),
        FRTM("FRTM", SupportedType.JAVA_LANG_INTEGER),
        FRTMX("FRTMX", SupportedType.JAVA_LANG_INTEGER),
        FRTA("FRTA", SupportedType.JAVA_LANG_DOUBLE),
        BRTM("BRTM", SupportedType.JAVA_LANG_INTEGER),
        BRTMX("BRTMX", SupportedType.JAVA_LANG_INTEGER),
        BRTA("BRTA", SupportedType.JAVA_LANG_DOUBLE),
        AP("AP", SupportedType.JAVA_LANG_DOUBLE),
        SERVICE_OPERATION_VALUE("SERVICE_OPERATION_VALUE", SupportedType.JAVA_LANG_STRING),
        MAPPING_VALUE_1("MAPPING_VALUE_1", SupportedType.JAVA_LANG_STRING),
        MAPPING_VALUE_2("MAPPING_VALUE_2", SupportedType.JAVA_LANG_STRING),
        MAPPING_VALUE_3("MAPPING_VALUE_3", SupportedType.JAVA_LANG_STRING),
        MAPPING_VALUE_4("MAPPING_VALUE_4", SupportedType.JAVA_LANG_STRING),
        MAPPING_VALUE_5("MAPPING_VALUE_5", SupportedType.JAVA_LANG_STRING),
        AUTHENTICATED_USER("AUTHENTICATED_USER", SupportedType.JAVA_LANG_STRING),
        CONSTANT_GROUP("CONSTANT_GROUP", SupportedType.JAVA_LANG_STRING);

        ColumnName(String columnName, SupportedType type) {
            this.columnName = columnName;
            this.type = type;
        }

        public String getColumnName() {
            return columnName;
        }

        public SupportedType getType() {
            return type;
        }

        public static ColumnName getColumnName(String name) {
            ColumnName[] allColumns = values();
            for (ColumnName cn : allColumns) {
                if (cn.getColumnName().equals(name)) return cn;
            }
            throw new IllegalArgumentException("Requested column name '" + name + "' not found");
        }

        private final String columnName;
        private final SupportedType type;
    }

    enum SupportedType {
        JAVA_LANG_INTEGER("java.lang.Integer"),
        JAVA_LANG_LONG("java.lang.Long"),
        JAVA_LANG_STRING("java.lang.String"),
        JAVA_LANG_DOUBLE("java.lang.Double");

        SupportedType(String name) {
            this.name = name;
        }

        public static SupportedType getType(String type) throws UnsupportedTypeExcetpion {
            SupportedType[] allTypes = values();
            for (SupportedType st : allTypes) {
                if (st.getName().equals(type)) return st;
            }
            throw new UnsupportedTypeExcetpion("Requested type '" + type + "' not supported");
        }

        public String getName() {
            return name;
        }

        private final String name;
    }

    private boolean hasExecuted;
    private boolean closed;
    private final Connection conn;
    private PreparedStatement preparedStatement;
    private ResultSet rs;
    private final Map<String, ColumnName> fieldNameToColumnName = new HashMap<String, ColumnName>();
    /**
     * A master report will track all data sources created for subreports to ensure their resources are
     * always closed correctly
     */
    private final List<PreparedStatementDataSource> subReportInstances;
    private final boolean isMasterDataSource;
}

/**
 * Exception defined to ensure close can be called when a type is requested which is not supported
 */
class UnsupportedTypeExcetpion extends Exception {
    UnsupportedTypeExcetpion(String message) {
        super(message);
    }
}
