package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.StubDataStore;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
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
        final TestSuite suite = new TestSuite(AbstractLocatorTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * test setup that deletes the stub data store; will trigger
             * store recreate
             * sets the environment
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                File f = new File(StubDataStore.DEFAULT_STORE_PATH);
                if (f.exists()) {
                    f.delete();
                }

                System.setProperty("com.l7tech.common.locator.properties",
                        "/com/l7tech/common/locator/test.properties");

            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testSimpleLookup() throws Exception {
        TesLocator locator = new TesLocator();
        locator.addPair(String.class, Object.class);
        Object result = locator.lookup(String.class);
        if (result == null) {
            fail("Exptected successfull lookup.");
        }

        Object result2 = locator.lookup(String.class);
        assertTrue(result == result2);
    }

    public void testFailLookup() throws Exception {
        TesLocator locator = new TesLocator();
        locator.addPair(Object.class, Object.class);
        Object result = locator.lookup(String.class);
        if (result != null) {
            fail("Lookup should have returned null.");
        }
    }


    public void testPropertiesLocator() throws Exception {
        LocatorStub.recycle();
        logger.info(Locator.getDefault().toString());
        Object result =
          Locator.getDefault().lookup(IdentityAdmin.class);
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
        public static void recycle() {
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
