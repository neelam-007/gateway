package com.l7tech.common.audit;

import com.l7tech.common.Component;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author alex
 * @version $Revision$
 */
public class TestComponent extends TestCase {
    /**
     * test <code>TestComponent</code> constructor
     */
    public TestComponent( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the TestComponent <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( TestComponent.class );
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testStuff() throws Exception {
        System.out.println(Component.GATEWAY);
        System.out.println(Component.GW_POLICY);
        System.out.println(Component.HTTP_BASIC);
        System.out.println(Component.GW_CLUSTER);
    }

    /**
     * Test <code>TestComponent</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }
}