package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.metadata.CallMetaDataProvider;
import org.springframework.jdbc.core.metadata.CallMetaDataProviderFactory;
import org.springframework.jdbc.core.metadata.CallParameterMetaData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSetMetaData;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util for stored procedure calls and result processing using @{link SimpleJdbcCall}
 *
 * @author rraquepo
 */
public class JdbcCallHelper {

    //copied from JdbcTemplate since it is private there and we need the same string
    private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";
    private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";

    private static final String SQL_FUNCTION = "func";//keyword to indicate that we are calling a function instead of a stored procedure

    private final SimpleJdbcCall simpleJdbcCall;//made simpleJdbcCall a class variable so we can mock it out during test

    protected final Log logger = LogFactory.getLog(getClass());

    //using pattern instead of naked split, for splitting comma delimited parameters
    private final Pattern p = Pattern.compile(
            "(?x)          # enable comments                                      \n" +
                    "(\"[^\"]*\")  # quoted data, and store in group #1                   \n" +
                    "|             # OR                                                   \n" +
                    "([^,]+)       # one or more chars other than ',', and store it in #2 \n" +
                    "|             # OR                                                   \n" +
                    "\\s*,\\s*     # a ',' optionally surrounded by space-chars           \n"
    );


    public JdbcCallHelper(SimpleJdbcCall simpleJdbcCall1) {
        this.simpleJdbcCall = simpleJdbcCall1;
    }

    /**
     * Execute stored procedure (or function) and retrieves the result(s) as list of SqlRowSet
     *
     * @param query
     * @param args
     * @param schemaName explicit name of the schema. Applies only to Oracle
     * @return
     * @throws DataAccessException
     */
    public List<SqlRowSet> queryForRowSet(final String query, @Nullable final String schemaName ,Object... args) throws DataAccessException {
        Assert.notNull(query, "query must not be null");
        final List<SqlRowSet> results = new ArrayList<SqlRowSet>();
        final String procName = JdbcUtil.getName(query);
        if (query.toLowerCase().startsWith(SQL_FUNCTION)) {
            simpleJdbcCall.setProcedureName(procName);
            simpleJdbcCall.setFunction(true);
        } else {
            simpleJdbcCall.setProcedureName(procName);
            simpleJdbcCall.setFunction(false);
        }

        final boolean hasSchemaName = schemaName != null & !schemaName.trim().isEmpty();
        if(hasSchemaName){
            simpleJdbcCall.setSchemaName(schemaName);
        }

        //SimpleJdbcCall will require a list of named parameters, let's build it dynamically
        final MapSqlParameterSource inParameters = new MapSqlParameterSource();
        List<String> queryParameters = getParametersFromQuery(query);
        List<String> queryParametersRaw = getParametersFromQuery(query, false);
        List<String> parametersNames = getInParametersName(procName, hasSchemaName ? schemaName : null);

        if ((parametersNames.size() != queryParameters.size())) {
            throw new BadSqlGrammarException("", query, new SQLException("Incorrect number of arguments for " + procName + "; expected " + parametersNames.size() + ", got " + queryParameters.size() + "; query generated was " + query));
        }
        if (parametersNames.size() > 0) {//input parameters needed
            int paramIndex = 0;
            int varArgIndex = 0;//index of the parameter values from context
            for (final String paramName : parametersNames) {//get all IN parameter names of the procedure
                boolean definedPossibleNull=false;
                Object paramValue = null;
                if (paramIndex < queryParameters.size()){
                    paramValue = queryParameters.get(paramIndex++);
                    definedPossibleNull = true;
                }
                if (paramValue != null && paramValue.toString().equals("?") && !queryParametersRaw.get(paramIndex-1).equals("'?'") && !queryParametersRaw.get(paramIndex-1).equals("\"?\"")) {
                    if (varArgIndex >= args.length || args.length==0) {
                        throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query : @" + (varArgIndex + 1) + " - " + query));//Bug 12575 - user explicitly trying to use ? w/c is really an invalid query
                    }
                    paramValue = args[varArgIndex++];
                } else if (paramValue != null && ((String) paramValue).startsWith("@")){//special case if we want to verbose define INOUT param in MS SQL, see known issue
                    paramValue = "";
                } else if (paramValue != null && queryParametersRaw.get(paramIndex-1).equalsIgnoreCase("null") && queryParameters.get(paramIndex-1).equals(queryParametersRaw.get(paramIndex-1))){  // if null query parameter is not a string (no ' )
                    // handle literal null - e.g. not from a variable value
                    paramValue = null;
                    definedPossibleNull = true;
                }


                if (paramValue != null){
                    inParameters.addValue(paramName, paramValue);
                }  else if(definedPossibleNull){
                    inParameters.addValue(paramName, null, Types.NULL);
                }

                //there's an issue in MS SQL where OUT param behaves as an INOUT and therefore has the same columnType code
                //therefore those parameters will be included in the result
                //workaround in MS SQL is to make sure all IN parameters are declared first before any OUT parameter or introduce a @param or re-use a variable(dummy)
                //to be safe we will check if we reached the argument length, meaning we can't add parameters because we don't have anymore args to use
                //if (varArgIndex+paramIndex >= queryParameters.size()) break;
                if (varArgIndex > args.length) break;
                if (paramIndex > queryParameters.size()) break;
            }
        }
        final Map<String, Object> resultSets = simpleJdbcCall.execute(inParameters);
        //out parameters data
        final List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        final Map<String, Object> parameterMap = new HashMap<String, Object>();
        final List<String> outParameterNames = new ArrayList();
        for (final Map.Entry e : resultSets.entrySet()) {
            String key = e.getKey().toString();
            Object obj = e.getValue();
            if (key.startsWith(RETURN_RESULT_SET_PREFIX)) {
                if (obj instanceof List) {
                    final List<Map<String, Object>> recordRows = (List<Map<String, Object>>) obj;
                    if (recordRows.size() > 0) {
                        final Set recordSet = recordRows.get(0).keySet();//we can use the first row to get the column names of the results
                        final String[] columnNames = (String[]) recordSet.toArray(new String[recordSet.size()]);
                        results.add(new OutParameterResult(new OutParametersMetaData(null, columnNames), recordRows));
                    } else
                        logger.info("empty result set - " + key);
                } else
                    logger.warn("unable to process non-list result set - " + key);//should not really happen
            } else if (key.startsWith(RETURN_UPDATE_COUNT_PREFIX)) {
                //we don't need to process this query count?
                continue;
            } else {//this section will process OUT parameters, get all the results and process it later
                outParameterNames.add(key);
                parameterMap.put(key, obj);
            }
        }
        if (parameterMap.size() > 0) {
            data.add(parameterMap);
            results.add(new OutParameterResult(new OutParametersMetaData(null, outParameterNames.toArray(new String[outParameterNames.size()])), data));
        }
        return results;//this will return a List<SqlRowSet>
    }

    /**
     * Get the input parameter names of the procedure
     *
     * @param procName
     * @return
     */
    private List<String> getInParametersName(final String procName, @Nullable final String schemaName) {
        final CallMetaDataContext callMetaDataContext = new CallMetaDataContext();
        callMetaDataContext.setProcedureName(procName);
        if(schemaName!=null){
            callMetaDataContext.setSchemaName(schemaName);
        }
        final CallMetaDataProvider metaProvider = CallMetaDataProviderFactory.createMetaDataProvider(simpleJdbcCall.getJdbcTemplate().getDataSource(), callMetaDataContext);
        List<CallParameterMetaData> parameterMetaData = metaProvider.getCallParameterMetaData();
        List<String> parameterNames = new ArrayList<String>();
        for (final CallParameterMetaData meta : parameterMetaData) {                        // 1=procedureColumnIn, 2=procedureColumnInOut, 4=procedureColumnOut
            if (meta.getParameterType() == DatabaseMetaData.procedureColumnIn               // 1-IN parameter, 2-INOUT param in Mysql & Oracle , 4-OUT param in Mysql & Oracle,
                    || meta.getParameterType() == DatabaseMetaData.procedureColumnInOut) {  // but 2 is OUT param in MS SQL as well since it behaves as IN & OUT
                parameterNames.add(meta.getParameterName().replaceAll("@", ""));
            }
        }
        return parameterNames;
    }

    /**
     * Allows us to extract the parameters from the given query
     *
     * @param query
     * @return
     */
    public List<String> getParametersFromQuery(final String query) {
        return getParametersFromQuery(query, true);
    }

    /**
     * Allows us to extract the parameters from the given query
     *
     * @param query
     * @return
     */
    public List<String> getParametersFromQuery(final String query, boolean stripQuote) {
        List<String> values = new ArrayList<String>();
        final String name = JdbcUtil.getName(query);
        int marker = query.indexOf(name, 1) + name.length();
        //if (marker < query.length()) marker++;
        String query2 = query.substring(marker).trim();
        if(query2.startsWith("(")){
            if(!query2.endsWith(")")){
                throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query " + query));//non matching parenthesis
            } else {
                query2 = query2.substring(1,query2.length()-1);
            }
        } else if(query2.endsWith(")") || query2.endsWith(",")){
            throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query " + query));//non matching parenthesis
        }
        if (query2 != null && (query2.trim().equals("") || query2.trim().equals(")"))) {
            return values;
        }
        final Matcher m = p.matcher(query2);
        while (m.find()) {
            // get the match
            final String paramCopy = m.group();
            String param = paramCopy.trim();
            // it's a param if it's group #1 or #2, otherwise proceed to next match
            if(m.group(1) == null && m.group(2) == null) {
                continue;
            }            
            if(param.equals("")){
                throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query - missing param value ;" + query));
            } else if (param.startsWith("(") || param.endsWith(")")) {
                throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query - param:" + paramCopy + ";" + query));
            }
            if (stripQuote) {
                if (param.startsWith("'")) {
                    //trim string quote
                    param = param.substring(1);
                    if(!param.trim().endsWith("'")){
                        throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query - param:" + paramCopy + ";" + query));
                    }
                    param = param.substring(0, param.length() - 1);
                } else if (param.startsWith("\"")) {
                    //trim string quote
                    param = param.substring(1);
                    if(!param.trim().endsWith("\"")){
                        throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query - param:" + paramCopy + ";" + query));
                    }
                    param = param.substring(0, param.length() - 1);
                } else if(param.endsWith("\'") || param.endsWith("\"")){
                    throw new BadSqlGrammarException("", query, new SQLException("invalid/bad query - param:" + paramCopy + ";" + query));
                }
            }
            values.add(param);
        }
        return values;
    }

    /**
     * Our own implementation of metadata to represent our results
     */
    private class OutParametersMetaData extends ResultSetWrappingSqlRowSetMetaData {
        private String[] columnNames = null;

        public OutParametersMetaData(ResultSetMetaData resultSetMetaData, String[] columnNames) {
            super(resultSetMetaData);
            this.columnNames = columnNames;
        }

        @Override
        public int getColumnCount() throws InvalidResultSetAccessException {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) throws InvalidResultSetAccessException {
            return columnNames[column - 1];
        }

        @Override
        public int getColumnType(int column) throws InvalidResultSetAccessException {
            return Types.OTHER;
        }
    }

    /**
     * Our own implementation of SqlRowSet, allows us to re-use existing logic that already processes SqlRowSet
     */
    private class OutParameterResult implements SqlRowSet {

        private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        private SqlRowSetMetaData metaData = null;
        private int MAX_ROWS = -1;
        private int cursor = 0;

        public OutParameterResult(SqlRowSetMetaData metaData, List<Map<String, Object>> data) {
            this.metaData = metaData;
            this.data = data;
            this.MAX_ROWS = data.size();
        }

        @Override
        public SqlRowSetMetaData getMetaData() {
            return metaData;
        }

        @Override
        public boolean next() throws InvalidResultSetAccessException {
            return cursor++ < MAX_ROWS;
        }

        @Override
        public Object getObject(String s) throws InvalidResultSetAccessException {
            if (cursor > MAX_ROWS) throw new IllegalStateException("No data can be accessed.");

            Object value = null;
            for(Map.Entry entry : data.get(cursor - 1).entrySet()){
                String key = entry.getKey().toString();
                if(s!=null && s.equalsIgnoreCase(key)){
                    value=entry.getValue();
                    break;
                }
            }
            return value;
        }

        @Override
        public int findColumn(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("findColumn not implemented in OutParameterResult.");
        }

        @Override
        public BigDecimal getBigDecimal(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBigDecimal not implemented in OutParameterResult.");
        }

        @Override
        public BigDecimal getBigDecimal(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBigDecimal not implemented in OutParameterResult.");
        }

        @Override
        public boolean getBoolean(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBoolean not implemented in OutParameterResult.");
        }

        @Override
        public boolean getBoolean(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getBoolean not implemented in OutParameterResult.");
        }

        @Override
        public byte getByte(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getByte not implemented in OutParameterResult.");

        }

        @Override
        public byte getByte(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getByte not implemented in OutParameterResult.");
        }

        @Override
        public Date getDate(int i, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in OutParameterResult.");
        }

        @Override
        public Date getDate(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in OutParameterResult.");
        }

        @Override
        public Date getDate(String s, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in OutParameterResult.");
        }

        @Override
        public Date getDate(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDate not implemented in OutParameterResult.");
        }

        @Override
        public double getDouble(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDouble not implemented in OutParameterResult.");
        }

        @Override
        public double getDouble(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getDouble not implemented in OutParameterResult.");
        }

        @Override
        public float getFloat(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getFloat not implemented in OutParameterResult.");
        }

        @Override
        public float getFloat(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getFloat not implemented in OutParameterResult.");
        }

        @Override
        public int getInt(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getInt not implemented in OutParameterResult.");
        }

        @Override
        public int getInt(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getInt not implemented in OutParameterResult.");
        }

        @Override
        public long getLong(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getLong not implemented in OutParameterResult.");
        }

        @Override
        public long getLong(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getLong not implemented in OutParameterResult.");
        }

        @Override
        public Object getObject(int i, Map map) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getObject not implemented in OutParameterResult.");
        }

        @Override
        public Object getObject(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getObject not implemented in OutParameterResult.");
        }

        @Override
        public Object getObject(String s, Map map) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getObject not implemented in OutParameterResult.");
        }

        @Override
        public short getShort(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getShort not implemented in OutParameterResult.");
        }

        @Override
        public short getShort(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getShort not implemented in OutParameterResult.");
        }

        @Override
        public String getString(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getString not implemented in OutParameterResult.");
        }

        @Override
        public String getString(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getString not implemented in OutParameterResult.");
        }

        @Override
        public Time getTime(int i, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in OutParameterResult.");
        }

        @Override
        public Time getTime(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in OutParameterResult.");
        }

        @Override
        public Time getTime(String s, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in OutParameterResult.");
        }

        @Override
        public Time getTime(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTime not implemented in OutParameterResult.");
        }

        @Override
        public Timestamp getTimestamp(int i, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in OutParameterResult.");
        }

        @Override
        public Timestamp getTimestamp(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in OutParameterResult.");
        }

        @Override
        public Timestamp getTimestamp(String s, Calendar calendar) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in OutParameterResult.");
        }

        @Override
        public Timestamp getTimestamp(String s) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getTimestamp not implemented in OutParameterResult.");
        }

        @Override
        public boolean absolute(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("absolute not implemented in OutParameterResult.");
        }

        @Override
        public void afterLast() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("afterLast not implemented in OutParameterResult.");
        }

        @Override
        public void beforeFirst() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("beforeFirst not implemented in OutParameterResult.");
        }

        @Override
        public boolean first() throws InvalidResultSetAccessException {
            cursor = 0;
            return true;
        }

        @Override
        public int getRow() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("getRow not implemented in OutParameterResult.");
        }

        @Override
        public boolean isAfterLast() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isAfterLast not implemented in OutParameterResult.");
        }

        @Override
        public boolean isBeforeFirst() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isBeforeFirst not implemented in OutParameterResult.");
        }

        @Override
        public boolean isFirst() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isFirst not implemented in OutParameterResult.");
        }

        @Override
        public boolean isLast() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("isLast not implemented in OutParameterResult.");
        }

        @Override
        public boolean last() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("last not implemented in OutParameterResult.");
        }

        @Override
        public boolean previous() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("previous not implemented in OutParameterResult.");
        }

        @Override
        public boolean relative(int i) throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("relative not implemented in OutParameterResult.");
        }

        @Override
        public boolean wasNull() throws InvalidResultSetAccessException {
            throw new UnsupportedOperationException("wasNull not implemented in OutParameterResult.");
        }
    }
}
