package com.l7tech.gateway.common.jdbc;

import com.l7tech.common.io.XmlUtil;

import java.util.List;
import java.util.Map;

public class JdbcUtil {
    public final static String CALL = "call"; //standard stored procedure call for MySQL & Oracle
    public final static String EXEC = "exec"; //exec/execute - stored procedure call for MS SQL & T-SQL
    public final static String FUNC = "func"; //keyword to indicate that we are calling a function instead of a stored procedure

    public final static String EMPTY_STRING = "";
    public final static String XML_RESULT_COL_OPEN = "<L7j:col ";
    public final static String XML_RESULT_COL_CLOSE = "</L7j:col>";
    public final static String XML_RESULT_ROW_OPEN = "<L7j:row>";
    public final static String XML_RESULT_ROW_CLOSE = "</L7j:row>";
    public final static String XML_CDATA_TAG_OPEN = "<![CDATA[";
    public final static String XML_CDATA_TAG_CLOSE = "]]>";
    public final static String XML_NULL_VALUE = XML_CDATA_TAG_OPEN + "NULL" + XML_CDATA_TAG_CLOSE;//are special entry to null values


    /**
     * Extract the procedure or function name from the query string
     *
     * @param query
     * @return
     */
    static public String getName(final String query) {
        StringBuffer sb = new StringBuffer();
        boolean keyword=false;
        for(char c : query.toCharArray()){
            if(c!=' ' && c!='(' && c!='\"' && c!='\''){
                if(keyword)
                    sb.append(c);
            } else {
                if(keyword)
                    break;
                else
                    keyword=true;
            }
        }
        return sb.toString().trim();
    }

    public static  boolean isStoredProcedure(String query) {
        return query.toLowerCase().startsWith(CALL)
                        || query.toLowerCase().startsWith(EXEC)
                        || query.toLowerCase().startsWith(FUNC);
    }


    public static void buildXmlResultString(Map<String, List<Object>> resultSet, final StringBuilder xmlResult) {
        int row = 0;
        //try to check how many rows we need
        for (String columnName : resultSet.keySet()) {
            if (resultSet.get(columnName) != null) {
                row = resultSet.get(columnName).toArray().length;
                break;
            }
        }
        StringBuilder records = new StringBuilder();
        for (int i = 0; i < row; i++) {
            records.append(XML_RESULT_ROW_OPEN);
            for (String columnName : resultSet.keySet()) {
                List list = resultSet.get(columnName);
                Object value = null;
                if (list != null && i < list.size()) {
                    value = resultSet.get(columnName).get(i);
                }
                String colType = EMPTY_STRING;
                if (value != null) {
                    if (value instanceof byte[]) {
                        colType = "type=\"java.lang.byte[]\"";
                        StringBuilder sb = new StringBuilder();
                        for (byte b : (byte[]) value) {
                            sb.append(String.format("%02X ", b));
                        }
                        value = sb.toString();
                    } else {
                        colType = "type=\"" + value.getClass().getName() + "\"";
                    }
                }
                records.append(XML_RESULT_COL_OPEN + " name=\"" + columnName + "\" " + colType + ">");
                if (value != null) {
                    records.append(handleSpecialXmlChar(value));
                } else {
                    records.append(XML_NULL_VALUE);
                }
                records.append(XML_RESULT_COL_CLOSE);
            }
            records.append(XML_RESULT_ROW_CLOSE);
        }
        xmlResult.append(records);
    }

    public static Object handleSpecialXmlChar(final Object inputObj) {
        if (inputObj instanceof String) {
            String inputStr = inputObj.toString();
            if (!inputStr.startsWith(XML_CDATA_TAG_OPEN) && (inputStr.indexOf('>') >= 0 || inputStr.indexOf('<') >= 0 || inputStr.indexOf('&') >= 0)) {
                StringBuilder sb = new StringBuilder(XML_CDATA_TAG_OPEN);
                sb.append(inputStr);
                sb.append(XML_CDATA_TAG_CLOSE);
                return sb.toString();
            } else {
                return inputStr;
            }
        } else {
            return inputObj;
        }
    }
}
