package com.l7tech.external.assertions.mysqlcounter;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the MysqlCounterAssertion.
 */
public class MysqlCounterAssertionTest {

    @Test
    public void isCloneDeepyCopy() {
        AllAssertionsTest.checkCloneIsDeepCopy(new MysqlCounterAssertion());
    }
}
