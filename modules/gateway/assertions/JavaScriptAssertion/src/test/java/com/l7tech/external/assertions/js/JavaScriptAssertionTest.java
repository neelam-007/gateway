package com.l7tech.external.assertions.js;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the JavaScriptAssertion.
 */
public class JavaScriptAssertionTest {

    private static final Logger log = Logger.getLogger(JavaScriptAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new JavaScriptAssertion() );
    }

}
