package com.l7tech.proxy.datamodel;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.util.ThreadPoolTest;

import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;
import java.io.IOException;

/**
 * Work the Client Proxy's SSG Manager a little bit.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 3:39:14 PM
 */
public class SsgManagerTest extends TestCase {
    static Logger log = Logger.getLogger(ThreadPoolTest.class.getName());

    private static final SsgManagerImpl sm = SsgManagerImpl.getInstance();

    private static final Ssg SSG1 =
            new Ssg(sm.nextId(), "Test SSG1", "testssg1", "http://localhost:4444/soap");
    private static final Ssg SSG2 =
            new Ssg(sm.nextId(), "Test SSG2", "testssg2", "http://localhost:4445/soap");
    private static final Ssg SSG3 =
            new Ssg(sm.nextId(), "Test SSG3", "testssg3", "http://localhost:4446/soap");
    private static final Ssg SSG3_CLONE = SSG3.getCopy();

    private static final Ssg[] TEST_SSGS = { SSG1, SSG2, SSG3, SSG3_CLONE };

    public SsgManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SsgManagerTest.class);
    }

    public void eraseAllSsgs() {
        // Erase the current store
        try {
            sm.clear();
            sm.save();
            sm.load();
        } catch (IOException e) {
        }
    }

    /** Make sure the slate is clean before starting a test. */
    public void removeBunkySsgsByName(SsgManagerImpl smi) {
        for (int i = 0; i < TEST_SSGS.length; ++i) {
                boolean found;
                do {
                    found = true;
                    try {
                        Ssg ssg = smi.getSsgByName(TEST_SSGS[i].getName());
                        smi.remove(ssg);
                    } catch (SsgNotFoundException e) {
                        found = false;
                    }
                } while (found);
        }
    }

    /**
     * Find how many SSGs are registered under the specified name.
     * @param name
     * @return The number of SSGs this.sm with the given name
     */
    private int countNames(String name) {
        int count = 0;
        for (Iterator i = sm.getSsgList().iterator(); i.hasNext(); ) {
            Ssg ssg = (Ssg)i.next();
            if (name.equals(ssg.getName()))
                ++count;
        }
        return count;
    }

    private interface Testable {
        public void run() throws Exception;
    }

    private void mustThrow(Class c, Testable r) throws Exception {
        try {
            r.run();
        } catch (Exception e) {
            if (c.isInstance(e))
                return;
            throw new Exception("Testable threw the wrong exception", e);
        }

        throw new Exception("Testable failed to throw the expected exception");
    }

    /** Make sure none of our test records are present in the given SMI. */
    public void assertNoBunkysIn(final SsgManagerImpl smi) throws Exception {
        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                smi.getSsgByName(SSG1.getName());
            }
        });

        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                smi.getSsgByName(SSG2.getName());
            }
        });

        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                smi.getSsgByName(SSG3.getName());
            }
        });

        Collection unwantedNames = Arrays.asList(new String[] {
            SSG1.getName(), SSG2.getName(), SSG3.getName()
        });
        Collection unwantedEndpoints = Arrays.asList(new String[] {
            SSG1.getLocalEndpoint(), SSG2.getLocalEndpoint(), SSG3.getLocalEndpoint()
        });
        for (Iterator i = smi.getSsgList().iterator(); i.hasNext(); ) {
            Ssg ssg = (Ssg)i.next();
            if (unwantedNames.contains(ssg.getName()) || unwantedEndpoints.contains(ssg.getLocalEndpoint()))
                throw new Exception("Failed to remove all test SSG records");;
        }
    }

    public void testPersistence() throws Exception {
        eraseAllSsgs();
        assertTrue(sm.getSsgList().size() == 0);

        // Add a couple of SSGs
        sm.add(SSG1);
        sm.add(SSG2);
        assertTrue(sm.getSsgList().contains(SSG1));
        assertTrue(sm.getSsgList().contains(SSG2));

        // Find SSGs by name.
        assertTrue(sm.getSsgByName(SSG1.getName()) == SSG1);
        assertTrue(sm.getSsgByName(SSG2.getName()) == SSG2);

        // Find SSGs by local endpoint.
        assertTrue(sm.getSsgByEndpoint(SSG1.getLocalEndpoint()) == SSG1);
        assertTrue(sm.getSsgByEndpoint(SSG2.getLocalEndpoint()) == SSG2);

        // Unable to find nonexistent SSGs
        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                sm.getSsgByName("Bloof Blaz");
            }
        });
        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                sm.getSsgByEndpoint("zasdfasdf");
            }
        });

        // Ssg manager won't store multiple Ssg's that equals() the same
        assertFalse(sm.add(SSG1));

        // Persistence works
        sm.save();
        sm.clear();
        assertNoBunkysIn(sm);
        sm.load();
        Ssg loaded1 = sm.getSsgByName(SSG1.getName());
        assertTrue(loaded1 != null);
        assertTrue(loaded1.getName() != null);
        assertTrue(loaded1.getLocalEndpoint().equals(SSG1.getLocalEndpoint()));
        Ssg loaded2 = sm.getSsgByEndpoint(SSG2.getLocalEndpoint());
        assertTrue(loaded2 != null);
        assertTrue(loaded2.getName() != null);
        assertTrue(loaded2.getName().equals(SSG2.getName()));

        // May not add two Ssgs with the same Id
        // (Actually you shouldn't even be instantiating two Ssgs with the same Id)
        sm.add(SSG3);
        SSG3_CLONE.setId(SSG3.getId());
        assertFalse(sm.add(SSG3_CLONE));
        assertTrue(countNames(SSG3.getName()) == 1);

        // May add two Ssgs with the same fields as long as the Ids differ.
        SSG3_CLONE.setId(sm.nextId());
        sm.add(SSG3_CLONE);
        assertTrue(countNames(SSG3.getName()) == 2);

        eraseAllSsgs();
        assertTrue(sm.getSsgList().size() == 0);
    }

    public void testFailures() {
        assertTrue(false);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}

