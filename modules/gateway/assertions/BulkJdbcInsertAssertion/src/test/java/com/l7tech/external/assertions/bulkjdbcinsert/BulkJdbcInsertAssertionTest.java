package com.l7tech.external.assertions.bulkjdbcinsert;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the BulkJdbcInsertAssertion.
 */
public class BulkJdbcInsertAssertionTest {

    private static final Logger log = Logger.getLogger(BulkJdbcInsertAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new BulkJdbcInsertAssertion());
    }

}
