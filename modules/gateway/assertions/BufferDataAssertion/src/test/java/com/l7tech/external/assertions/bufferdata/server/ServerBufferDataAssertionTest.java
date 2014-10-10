package com.l7tech.external.assertions.bufferdata.server;

import static org.junit.Assert.*;

import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.TestTimeSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the BufferDataAssertion.
 */
public class ServerBufferDataAssertionTest {

    private static final Logger log = Logger.getLogger(ServerBufferDataAssertionTest.class.getName());

    private static final TestTimeSource timeSource = new TestTimeSource();

    @BeforeClass
    public static void setUp() {
        OrderedMemoryBuffer.timeSource = timeSource;
        NonBlockingMemoryBuffer.timeSource = timeSource;
    }

    @Before
    public void before() {
        timeSource.sync();
    }

    @Test
    public void testSimpleAppendData() throws Exception {
        BufferDataAssertion ass = new BufferDataAssertion();
        ass.setBufferName( "mybuff" );
        ass.setNewDataVarName( "csv" );
        ass.setMaxSizeBytes( 20 );
        ass.setMaxAgeMillis( 1000 );
        ass.setExtractIfFull( true );
        ass.setVariablePrefix( "buffer" );
        ServerBufferDataAssertion sass = new ServerBufferDataAssertion( ass );

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );

        // Initial data
        context.setVariable( "csv", "a|0|\n" );
        AssertionStatus result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // 5 more bytes
        context.setVariable( "csv", "b|1|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // 5 more bytes
        context.setVariable( "csv", "c|2|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // 5 more bytes -- buffer now exactly perfectly full, but extract not triggered until
        // next write of more than zero bytes (or until oldest buffered data exceeds maximum age)
        context.setVariable( "csv", "d|3|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // we now overflow the buffer.  existing 20 bytes extracted, new write becomes start of new buffer
        context.setVariable( "csv", "e|4|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.FALSIFIED, result );
        Message extract = (Message) context.getVariable( "buffer.extractedMessage" );
        assertNotNull( extract );

        String extractedStr = new String( IOUtils.slurpStream( extract.getMimeKnob().getEntireMessageBodyAsInputStream() ), Charsets.UTF8 );
        assertEquals( "a|0|\nb|1|\nc|2|\nd|3|\n", extractedStr );

        // Now do a zero-length write after simulating passage of an entire day
        // oldest data in buffer is now too old, so extract is triggered even though buffer only 25% full
        // and new write was zero-length
        context.setVariable( "csv", "" );
        timeSource.advanceByMillis( 8640000L );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.FALSIFIED, result );
        extract = (Message) context.getVariable( "buffer.extractedMessage" );
        assertNotNull( extract );

        extractedStr = new String( IOUtils.slurpStream( extract.getMimeKnob().getEntireMessageBodyAsInputStream() ), Charsets.UTF8 );
        assertEquals( "e|4|\n", extractedStr );
    }

    @Test
    public void testWriteOverLargeDataChunk() throws Exception {
        BufferDataAssertion ass = new BufferDataAssertion();
        ass.setBufferName( "mybuff2" );
        ass.setNewDataVarName( "csv" );
        ass.setMaxSizeBytes( 5 );
        ass.setMaxAgeMillis( 1000 );
        ass.setExtractIfFull( true );
        ass.setVariablePrefix( "buffer" );
        ServerBufferDataAssertion sass = new ServerBufferDataAssertion( ass );

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );

        // Initial data is larger than the entire buffer.
        // As there is no safe correct reaction to this, the insert operation fails
        context.setVariable( "csv", "1234567890" );
        AssertionStatus result = sass.checkRequest( context );
        assertEquals( AssertionStatus.SERVER_ERROR, result );

    }


    private void assertNullOrNotSet( PolicyEnforcementContext context, String var ) {
        try {
            Object value = context.getVariable( var );
            assertNull( "Variable not supposed to have a non-null value: " + var, value );
        } catch ( NoSuchVariableException e ) {
            // Ok
        }
    }
}
