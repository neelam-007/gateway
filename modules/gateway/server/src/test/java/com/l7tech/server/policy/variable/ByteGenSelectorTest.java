package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.util.ByteGen;
import com.l7tech.util.HexUtils;
import com.l7tech.util.RandomByteGen;
import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Tests for ByteGenSelector.
 */
public class ByteGenSelectorTest {
    private static final String TEST_BYTES = "933a1e35d890fe45afa350635f853de4e6f22e6ac37f8f9a441931d7953381a43da9c7080f431da525a72fd39154db228b31599a6cff08b6bf07575c74dd3844bd740b1104c4cdf33926ec81f86f4a25dde3653e59d69e300a57f31fe370de549172a569795dbec0b497de48184892";
    private TestAudit testAudit;
    private byte[] testBytes;
    private ByteGen byteGen = new TestByteGen();
    private boolean strict = true;

    @Before
    public void setUp() throws Exception {
        testAudit = new TestAudit();
        testBytes = HexUtils.unHexDump( TEST_BYTES );
    }

    @After
    public void cleanUp() throws Exception {
        cleanUpAll();
    }

    @AfterClass
    public static void cleanUpAll() throws Exception {
        SyspropUtil.clearProperties(
                ByteGenSelector.PROP_MAX_BYTES
        );
    }


    @Test
    public void testRandom() throws Exception {
        assertEquals( "TestByteGen always returns same value", expand( "${r.hex}" ), expand( "${r.hex}" ) );
        assertEquals( "TestByteGen always returns same value", expand( "${r.44}" ), expand( "${r.44}" ) );
        assertEquals( "TestByteGen always returns same value", expand( "${r.90.base64}" ), expand( "${r.90.base64}" ) );

        byteGen = new RandomByteGen();
        assertFalse( "RandomByteGen always returns different value", expand( "${r.hex}" ).equals( expand( "${r.hex}" ) ) );
        assertFalse( "RandomByteGen always returns different value", expand( "${r.44}" ).equals( expand( "${r.44}" ) ) );
        assertFalse( "RandomByteGen always returns different value", expand( "${r.90.base64}" ).equals( expand( "${r.90.base64}" ) ) );
        byteGen = new TestByteGen();
        assertEquals( "TestByteGen always returns same value", expand( "${r.44}" ), expand( "${r.44}" ) );
    }

    @Test
    public void testFormatting() throws Exception {
        // Make sure lengths look sane
        assertEquals( 64, expand( "${r}" ).length() );
        assertEquals( 64, expand( "${r.hex}" ).length() );
        assertEquals( 64, expand( "${r.32}" ).length() );
        assertEquals( 64, expand( "${r.32.hex}" ).length() );
        assertEquals( 34, expand( "${r.17}" ).length() );
        assertEquals( 22, expand( "${r.11.hex}" ).length() );
        assertEquals( 44, expand( "${r.base64}" ).length() );

        testRand( "${r}",         "933a1e35d890fe45afa350635f853de4e6f22e6ac37f8f9a441931d7953381a4" );
        testRand( "${r.integer}", "-49199414861744211201517162886646227642762792711330976701164483741814136536668" );
        testRand( "${r}",                    "933a1e35d890fe45afa350635f853de4e6f22e6ac37f8f9a441931d7953381a4" );

        testRand( "${r.1}",     "93" );
        testRand( "${r.2.hex}", "933a" );
        testRand( "${r.3.hex}", "933a1e" );
        testRand( "${r.11.hex}", "933a1e35d890fe45afa350" );
        
        testRand( "${r.1.base64}", "kw==" );
        testRand( "${r.2.base64}", "kzo=" );
        testRand( "${r.7.base64}", "kzoeNdiQ/g==" );
        testRand( "${r.10.base64}", "kzoeNdiQ/kWvow==" );
        testRand( "${r.24.base64}", "kzoeNdiQ/kWvo1BjX4U95ObyLmrDf4+a" );
        testRand( "${r.36.base64}", "kzoeNdiQ/kWvo1BjX4U95ObyLmrDf4+aRBkx15UzgaQ9qccI" );

        testRand( "${r.integer}", "-49199414861744211201517162886646227642762792711330976701164483741814136536668" );
        testRand( "${r.1.integer}", "-109" );
        testRand( "${r.2.integer}", "-27846" );
        testRand( "${r.3.integer}", "-7128546" );
        testRand( "${r.4.integer}", "-1824907723" );
        testRand( "${r.8.integer}", "-7837918984869446075" );
        testRand( "${r.16.integer}", "-144584085584356039131934510720965329436" );
        testRand( "${r.32.integer}", "-49199414861744211201517162886646227642762792711330976701164483741814136536668" );

        testRand( "${r.unsigned}", "66592674375571984222053822122041680210507191954309587338293100266098993103268" );
        testRand( "${r.1.unsigned}", "147" );
        testRand( "${r.2.unsigned}", "37690" );
        testRand( "${r.3.unsigned}", "9648670" );
        testRand( "${r.4.unsigned}", "2470059573" );
        testRand( "${r.8.unsigned}", "10608825088840105541" );
        testRand( "${r.16.unsigned}", "195698281336582424331440096710802882020" );
        testRand( "${r.32.unsigned}", "66592674375571984222053822122041680210507191954309587338293100266098993103268" );
    }

    @Test
    public void testLaxMode() throws Exception {
        strict = false;
        assertEquals( "", expand( "${r.blarg}" ) );
        assertTrue( testAudit.isAuditPresentContaining( "Unsupported variable" ) );

        // minimum of 1 byte
        testRand( "${r.0}", "93" );
        testRand( "${r.0.integer}", "-109" );
        testRand( "${r.0.unsigned}", "147" );

        // maximum of configured max bytes
        SyspropUtil.setProperty( ByteGenSelector.PROP_MAX_BYTES, "11" );
        testRand( "${r.10}", "933a1e35d890fe45afa3" );
        testRand( "${r.11}", "933a1e35d890fe45afa350" );
        testRand( "${r.12}", "933a1e35d890fe45afa350" );
        testRand( "${r.13}", "933a1e35d890fe45afa350" );
        testRand( "${r.345346}", "933a1e35d890fe45afa350" );
    }

    @Test
    public void testBinary() throws Exception {
        // Only useful for places that use one of the expandAsObject() methods
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put( "r", byteGen );

        byte[] bytes = (byte[]) ExpandVariables.processSingleVariableAsObject( "${r.4.binary}", vars, testAudit, strict );
        assertFalse( testAudit.isAnyAuditPresent() );

        assertArrayEquals( HexUtils.unHexDump( "933a1e35" ), bytes );
    }

    @Test
    public void testBadSuffix() throws Exception {
        badSuffix( ".blarg" );
        //badSuffix( "." );  // throws ISE - possible bug in ExpandVariables.selectify()?
        badSuffix( ".0" );
        badSuffix( ".0.hex" );
        badSuffix( ".-1" );
        badSuffix( ".-555.hex" );
        badSuffix( ".2345239458762439857.hex" );
        badSuffix( ".345345435.shex" );
    }

    private void badSuffix( final String expr ) {
        testAudit.reset();

        try {
            expand( "${r" + expr + "}" );
            fail( "expected exception not thrown" );
        } catch ( IllegalArgumentException e ) {
            assertTrue( e.getMessage().contains( "Unsupported variable: " ) );
        }
    }

    private void testRand( final String expr, final String expected ) {
        testAudit.reset();

        String got = expand( expr );

        System.out.println( expr + "=" + got );
        for ( String s : testAudit ) {
            System.out.println( s );
        }

        assertEquals( expected, got );
        assertFalse( "No audit detail messages shall have been recorded during expansion", testAudit.isAnyAuditPresent() );
    }

    private String expand( final String expr ) {
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put( "r", byteGen );
        return ExpandVariables.process( expr, vars, testAudit, strict );
    }

    // A ByteGen that always returns the same string of test bytes.
    private class TestByteGen implements ByteGen {
        @Override
        public void generateBytes( @NotNull byte[] array, int offset, int length ) {
            System.arraycopy( testBytes, 0, array, offset, length );
        }
    }
}
