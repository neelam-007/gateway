package com.l7tech.proxy.util;

import com.l7tech.common.io.CertUtilsTest;
import com.l7tech.common.io.CertUtils;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.lang.reflect.Field;

/**
 * Test the BC implmenetation of the DnParser
 *
 * @author steve
 */
public class BCCertUtilsTest extends TestCase {

    public BCCertUtilsTest( String name ) {
        super( name );
    }

    public static Test suite() {
        return new TestSuite( BCCertUtilsTest.class );
    }

    public static void main( String[] args ) {
        junit.textui.TestRunner.run( suite() );
    }

    public void testDnParserBc() throws Exception {
        setDnParser( new DnParserBc() );
        new BCCertUtilsTestRunner( getName() ).runTests();
    }

    private void setDnParser( final CertUtils.DnParser parser ) throws Exception {
        Field parserField = CertUtils.class.getDeclaredField( "DN_PARSER" );
        parserField.setAccessible( true );
        parserField.set( null, parser );
    }

    /**
     * Create subclass here since we don't want to run all the tests.
     */
    private static final class BCCertUtilsTestRunner extends CertUtilsTest {
        public BCCertUtilsTestRunner( String name ) {
            super( name );
        }

        public void runTests() throws Exception {
            doTestDnParse();
            doTestDnPatterns();
        }
    }
}
