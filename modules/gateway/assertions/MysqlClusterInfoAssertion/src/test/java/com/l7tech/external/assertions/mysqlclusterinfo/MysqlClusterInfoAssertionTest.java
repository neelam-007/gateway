package com.l7tech.external.assertions.mysqlclusterinfo;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the MysqlClusterInfoAssertion.
 */
public class MysqlClusterInfoAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new MysqlClusterInfoAssertion());
    }

}
