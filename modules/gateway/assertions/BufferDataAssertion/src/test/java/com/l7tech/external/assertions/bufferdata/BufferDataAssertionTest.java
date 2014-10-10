package com.l7tech.external.assertions.bufferdata;

import static org.junit.Assert.*;

import com.l7tech.external.assertions.bufferdata.server.ServerBufferDataAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.TestTimeSource;
import com.l7tech.util.TimeSource;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the BufferDataAssertion.
 */
public class BufferDataAssertionTest {

    private static final Logger log = Logger.getLogger(BufferDataAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new BufferDataAssertion() );
    }


}
