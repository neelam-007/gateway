package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.console.JdbcQueryAssertionDialog;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.regex.Matcher;

/**
 * Test the JDBCQueryAssertion.
 */
public class ServerJDBCQueryAssertionTest extends TestCase {

    public ServerJDBCQueryAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerJDBCQueryAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {
        dostuff("SELECT foo, funky(bar), * as all FROM baz");
        dostuff("SELECT foo,bar, * as all FROM baz");
        dostuff("SELECT foo,bar, select (one,two) from other as others FROM baz"); // FAIL
        dostuff("SELECT foo, funky(bar) AS yousuck, * as all FROM baz");
        dostuff("SELECT * FROM baz");
        dostuff("select bunk from table where clause=value");
        dostuff("select bunk\nfrom table where clause=value");
        dostuff("select bunk from table\nwhere clause=value");
    }

    private void dostuff(final String data) {
        System.out.println(data);
        Matcher mat = JdbcQueryAssertionDialog.GREP_COLUMNS_PATTERN.matcher(data);
        if (!mat.find()) throw new IllegalStateException();
        System.out.println("SELECT: " + mat.group(1));
        String[] cols = mat.group(1).split(",\\s*");
        for (String col : cols) {
            Matcher assmat = JdbcQueryAssertionDialog.GREP_ALIAS_PATTERN.matcher(col);
            if (assmat.matches()) {
                System.out.printf("%s aliased to %s\n", assmat.group(1), assmat.group(2));
            } else {
                System.out.println(col + " as-is");
            }
        }
        System.out.println("=========");
    }

//    public void testMoreStuff() throws Exception {
//        doMoreStuff("INSERT INTO tablename WHERE somevalue=blah");
//        doMoreStuff("INseRT INTO tablename WHERE somevalue=blah");
//        doMoreStuff("DELETE from tablename WHERE somevalue=blah");
//        doMoreStuff("delete from tablename WHERE somevalue=blah");
//        doMoreStuff("UPDATE tablename SET column=value WHERE somevalue=blah");
//        doMoreStuff("uPDATe tablename SET column=value WHERE somevalue=blah");
//        doMoreStuff("SELECT from sometable where value=blah");
//    }
//
//    private void doMoreStuff(final String data) {
//        System.out.println(data);
//        Matcher insertMatcher = JDBCQueryAssertion.GREP_INSERT_PATTERN.matcher(data);
//        Matcher updateMatcher = JDBCQueryAssertion.GREP_UPDATE_PATTERN.matcher(data);
//        Matcher deleteMatcher = JDBCQueryAssertion.GREP_DELETE_PATTERN.matcher(data);
//        Matcher selectMatcher = JDBCQueryAssertion.SELECT_PATTERN.matcher(data);
//
//        if(insertMatcher.find()) {
//            System.out.println("INSERT statement");
//        }else if(updateMatcher.find()){
//            System.out.println("UPDATE statement");
//        } else if(deleteMatcher.find()){
//            System.out.println("DELETE statement");
//        }else if(selectMatcher.find()){
//            System.out.println("SELECT statement");
//        }
//        throw new IllegalStateException();
//    }



}
