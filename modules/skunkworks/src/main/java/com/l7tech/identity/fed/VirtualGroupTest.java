package com.l7tech.identity.fed;

import com.l7tech.common.io.CertUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author alex
 * @version $Revision$
 */
public class VirtualGroupTest extends TestCase {
    /**
     * test <code>VirtualGroupTest</code> constructor
     */
    public VirtualGroupTest( String name ) {
        super( name );
    }

    public void testPerformance() throws Exception {
        CertUtils.dnToAttributeMap("dc=layer7-tech,dc=com, uid=acruise");
        long before = System.currentTimeMillis();
        int i = 0;
        for (; i < 250000; i++) {
            CertUtils.dnToAttributeMap("dc=layer7-tech,dc=com, uid=acruise");
        }
        final long t = (System.currentTimeMillis() - before);
        System.out.println( i + " iterations in " + t + "ms.");
        System.out.println((double)i/t *1000 + " iterations per second");
    }

    /**
     * create the <code>TestSuite</code> for the VirtualGroupTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( VirtualGroupTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>VirtualGroupTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }
}