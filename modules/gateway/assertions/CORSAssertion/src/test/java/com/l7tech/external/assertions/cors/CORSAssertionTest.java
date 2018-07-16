package com.l7tech.external.assertions.cors;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the CORSAssertion.
 */
public class CORSAssertionTest {

    private static final Logger log = Logger.getLogger(CORSAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new CORSAssertion());
    }

}
