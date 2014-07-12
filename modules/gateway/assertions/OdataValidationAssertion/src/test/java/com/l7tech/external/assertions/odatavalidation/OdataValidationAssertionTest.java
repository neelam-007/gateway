package com.l7tech.external.assertions.odatavalidation;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the OdataValidationAssertion.
 */
public class OdataValidationAssertionTest {

    private static final Logger log = Logger.getLogger(OdataValidationAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new OdataValidationAssertion());
    }

}
