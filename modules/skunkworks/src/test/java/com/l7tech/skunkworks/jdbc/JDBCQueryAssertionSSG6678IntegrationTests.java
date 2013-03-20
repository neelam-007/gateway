package com.l7tech.skunkworks.jdbc;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.external.assertions.jdbcquery.server.ServerJdbcQueryAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This was created: 3/18/13 as 1:55 PM
 *
 * @author Victor Kazakov
 */
public class JDBCQueryAssertionSSG6678IntegrationTests extends ServerJdbcQueryAssertionIntegrationAbstractTestClass {
    private static final Logger logger = Logger.getLogger(JDBCQueryAssertionSSG6678IntegrationTests.class.getName());

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerJdbcQueryAssertionIntegrationAbstractTestClass.beforeClass(null);
    }

    @Test(timeout = 30 * 1000)
    public void testRetrieveTimeStampFromProcedure() throws PolicyAssertionException, IOException {
        final int sleepTime = 5;

        try {
            createDropItem(CreateSleepFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            assertion.setSqlQuery("FUNC " + SleepFunctionName + " " + sleepTime);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, getContext());

            long startTime = new Date().getTime();
            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(getPolicyEnforcementContext());
            long endTime = new Date().getTime();

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertTrue("Query returned too soon", endTime - startTime > sleepTime * 1000);
            Assert.assertTrue("Query took too long to return", endTime - startTime < (sleepTime + 2) * 1000);

        } finally {
            createDropItem(DropSleepFunction);
        }
    }

    @Test(timeout = 60 * 1000)
    public void testRetrieveTimeStampFromProcedureTimeoutExpired() throws PolicyAssertionException, IOException {
        final int queryTimeout = 10;
        final int querySleepTime = queryTimeout * 2;

        try {
            createDropItem(CreateSleepFunction);

            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            assertion.setQueryTimeout(String.valueOf(queryTimeout));
            assertion.setSqlQuery("FUNC " + SleepFunctionName + " " + querySleepTime);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, getContext());

            logger.log(Level.INFO, "Before executing assertion");
            long startTime = new Date().getTime();
            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(getPolicyEnforcementContext());
            long endTime = new Date().getTime();
            logger.log(Level.INFO, "After executing assertion");
            logger.log(Level.INFO, "Time to execute query: " + (endTime - startTime));
            Assert.assertEquals(AssertionStatus.FAILED, assertionStatus);

            Assert.assertTrue("Query returned too soon", endTime - startTime > queryTimeout * 1000);
            Assert.assertTrue("Query took too long to return", endTime - startTime < querySleepTime * 1000);
            Assert.assertTrue("Query took too long to return", endTime - startTime < (queryTimeout + queryTimeout / 2) * 1000);

        } finally {
            createDropItem(DropSleepFunction);
        }

    }

    private static final String SleepFunctionName = "TESTSLEEP";
    private static final String CreateSleepFunction =
            "create or replace\n" +
                    "FUNCTION " + SleepFunctionName + "\n" +
                    "(\n" +
                    "TIME_  IN  NUMBER\n" +
                    ")\n" +
                    "RETURN INTEGER IS\n" +
                    " BEGIN\n" +
                    "   DBMS_LOCK.sleep(seconds => TIME_);\n" +
                    "RETURN 1;\n" +
                    " EXCEPTION\n" +
                    "   WHEN OTHERS THEN\n" +
                    "   RAISE;\n" +
                    "   RETURN 1;\n" +
                    "END ;";
    public static final String DropSleepFunction = "DROP FUNCTION " + SleepFunctionName;
}
