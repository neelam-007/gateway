package com.l7tech.cluster;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.InetAddress;

/**
 * @author alex
 * @version $Revision$
 */
public class TestMulticastAddress extends TestCase {
    /**
     * test <code>TestMulticastAddress</code> constructor
     */
    public TestMulticastAddress( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the TestMulticastAddress <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( TestMulticastAddress.class );
        return suite;
    }

    public void testAddressGeneration() throws Exception {
        boolean[] found = new boolean[256];
        for ( int i = 0; i < 10000; i++ ) {
            String addr = ClusterInfoManager.generateMulticastAddress();
            InetAddress ia = InetAddress.getByName(addr.toString());
            found[(ia.getAddress()[3] & 0xff)] = true;
        }
        assertTrue(found[0]);
        assertTrue(found[255]);
    }


    /**
     * Test <code>TestMulticastAddress</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }
}