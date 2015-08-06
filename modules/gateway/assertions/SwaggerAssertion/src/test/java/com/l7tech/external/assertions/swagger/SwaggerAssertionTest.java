package com.l7tech.external.assertions.swagger;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the SwaggerAssertion.
 */
public class SwaggerAssertionTest {

    private static final Logger log = Logger.getLogger(SwaggerAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SwaggerAssertion());
    }

}
