package com.l7tech.util.locator;

import com.l7tech.util.Locator;
import com.l7tech.identity.IdentityProviderConfigManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * Class AbstractLocatorTest.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class AbstractLocatorTest extends TestCase {
    static Logger logger = Logger.getLogger(AbstractLocatorTest.class.getName());
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

    public void testPropertiesLocator() throws Exception {
        LocatorStub.recycle();
        System.setProperty("com.l7tech.util.locator.properties",
                           "/com/l7tech/console/resources/services.properties");
        logger.info(Locator.getDefault().toString());
        Object result =
          Locator.getDefault().lookup(IdentityProviderConfigManager.class);
        if (result == null) {
            fail("Exptected successfull lookup.");
        }

    }

    /**
     * The test locator class
     */
    static class TesLocator extends AbstractLocator {
    }

    static abstract class LocatorStub extends Locator {
        protected static void recycle() {
            Locator.recycle();
        }
    }


    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
