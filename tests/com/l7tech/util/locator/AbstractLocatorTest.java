package com.l7tech.util.locator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Class AbstractLocatorTest.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class AbstractLocatorTest extends TestCase {
    /**
     * test <code>AbstractLocatorTest</code> constructor
     */
    public AbstractLocatorTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * AbstractLocatorTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(AbstractLocatorTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testSimpleLookup() throws Exception {
        TesLocator locator = new TesLocator();
        locator.addPair("object", String.class);
        Object result = locator.lookup(String.class);
        if (result == null) {
            fail("Exptected successfull lookup.");
        }

        Object result2 = locator.lookup(String.class);
        assertTrue(result == result2);

    }

    static class TesLocator extends AbstractLocator {
    }

    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
