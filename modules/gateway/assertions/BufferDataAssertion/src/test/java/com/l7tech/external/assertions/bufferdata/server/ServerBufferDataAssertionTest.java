package com.l7tech.external.assertions.bufferdata.server;

import static org.junit.Assert.*;

import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.test.BugId;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.TestTimeSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Test the BufferDataAssertion.
 */
public class ServerBufferDataAssertionTest {

    private static final Logger log = Logger.getLogger(ServerBufferDataAssertionTest.class.getName());

    private static final TestTimeSource timeSource = new TestTimeSource();
    private static final String TEST_RECORD = "1,3,4,asdf,55,adsfhiauhrg,55,aghuiahsiuha,63336\r\n";

    @BeforeClass
    public static void setUp() {
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
        ass.setVariablePrefix( "buffer" );
        ServerBufferDataAssertion sass = new ServerBufferDataAssertion( ass );

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );

        // Initial data
        context.setVariable( "csv", "a|0|\n" );
        AssertionStatus result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertFalse( (Boolean) context.getVariable( "buffer.wasExtracted" ) );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // 5 more bytes
        context.setVariable( "csv", "b|1|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertFalse( (Boolean) context.getVariable( "buffer.wasExtracted" ) );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // 5 more bytes
        context.setVariable( "csv", "c|2|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertFalse( (Boolean) context.getVariable( "buffer.wasExtracted" ) );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // 5 more bytes -- buffer now exactly perfectly full, but extract not triggered until
        // next write of more than zero bytes (or until oldest buffered data exceeds maximum age)
        context.setVariable( "csv", "d|3|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertFalse( (Boolean) context.getVariable( "buffer.wasExtracted" ) );
        assertNullOrNotSet( context, "buffer.extractedMessage" );

        // we now overflow the buffer.  existing 20 bytes extracted, new write becomes start of new buffer
        context.setVariable( "csv", "e|4|\n" );
        result = sass.checkRequest( context );
        assertEquals( AssertionStatus.NONE, result );
        assertTrue( (Boolean) context.getVariable( "buffer.wasExtracted" ) );
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
        assertEquals( AssertionStatus.NONE, result );
        assertTrue( (Boolean) context.getVariable( "buffer.wasExtracted" ) );
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

    @Test
    @BugId( "DE254507" )
    @Ignore
    public void testParallelBuffering() throws Exception {
        final BufferDataAssertion ass1 = new BufferDataAssertion();
        ass1.setBufferName( "concurrent1" );
        ass1.setNewDataVarName( "csv" );
        ass1.setMaxSizeBytes( 90000000 );
        ass1.setMaxAgeMillis( 86400 * 1000L );
        ass1.setVariablePrefix( "capture" );
        final ServerBufferDataAssertion sass1 = new ServerBufferDataAssertion( ass1 );

        final BufferDataAssertion ass2 = new BufferDataAssertion();
        ass2.setBufferName( "concurrent1" );
        ass2.setNewDataVarName( "empty" );
        ass2.setMaxSizeBytes( 0L );
        ass2.setMaxAgeMillis( 0L );
        ass2.setVariablePrefix( "extract" );
        final ServerBufferDataAssertion sass2 = new ServerBufferDataAssertion( ass2 );

        Runnable r = new Runnable() {
            private void extract() {
                try {
                    PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
                    pec.setVariable( "empty", "" );
                    assertEquals( AssertionStatus.NONE, sass2.checkRequest( pec ) );
                    boolean wasExtracted = Boolean.valueOf( String.valueOf( pec.getVariable( "extract.wasExtracted" ) ) );
                    if ( wasExtracted ) {
                        Message mess = (Message) pec.getVariable( "extract.extractedMessage" );
                        byte[] messBytes = IOUtils.slurpStream( mess.getMimeKnob().getEntireMessageBodyAsInputStream() );
                        String messStr = new String( messBytes, Charsets.UTF8 ) ;
                        assertTrue( messStr.startsWith( TEST_RECORD ) );
                        assertTrue( messStr.endsWith( TEST_RECORD ) );
                    }
                } catch ( Exception e ) {
                    e.printStackTrace();
                    fail( "test failed: " + e );
                }
            }

            private void store() {
                try {
                    PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
                    pec.setVariable( "csv", TEST_RECORD );
                    assertEquals( AssertionStatus.NONE, sass1.checkRequest( pec ) );
                    assertFalse( Boolean.valueOf( String.valueOf( pec.getVariable( "capture.wasExtracted" ) ) ) );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    fail( "test failed: " + e );
                }
            }

            final Random s = new Random( 359835L );

            @Override
            public void run() {
                final Random r = new Random( s.nextLong() );
                store();
                store();
                store();
                store();
                store();
                store();
                if ( r.nextBoolean() )
                    extract();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
                if ( r.nextBoolean() )
                    extract();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
                store();
            }
        };
        new BenchmarkRunner( r, 1000000, 200, "buffer data workload" ).run();
    }
}
