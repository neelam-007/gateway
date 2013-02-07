package com.l7tech.gateway.common.jdbc;

public class JdbcUtil {
    public final static String CALL = "call"; //standard stored procedure call for MySQL & Oracle
    public final static String EXEC = "exec"; //exec/execute - stored procedure call for MS SQL & T-SQL
    public final static String FUNC = "func"; //keyword to indicate that we are calling a function instead of a stored procedure

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
}
