package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.util.ExceptionUtils;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 */
public class AllAssertionsTest {

    @Test
    public void testAssertionMetadataConsistency() throws Throwable {
        Assertion[] assertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion assertion : assertions) {
            try {
                testAssertionMetadataConsistency(assertion);
            } catch (AssertionError e) {
                throw new AssertionError("Failure for assertion " + assertion.getClass().getName() + ": " + ExceptionUtils.getMessage(e)).initCause(e);
            }
        }
    }

    public static void testAssertionMetadataConsistency(Assertion assertion) {        
        boolean messageTargetable = assertion instanceof MessageTargetable;
        boolean processesRequest = assertion.getClass().isAnnotationPresent(ProcessesRequest.class);
        boolean processesResponse = assertion.getClass().isAnnotationPresent(ProcessesResponse.class);

        assertTrue("MessageTargetable conflicts with @ProcessesRequest", !messageTargetable || messageTargetable != processesRequest);
        assertTrue("MessageTargetable conflicts with @ProcessesResponse", !messageTargetable || messageTargetable != processesResponse);
        assertTrue("@ProcessesRequest conflicts with @ProcessesResponse", !processesRequest || processesRequest != processesResponse);
    }
}
