package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Work the Client Proxy's SSG Manager a little bit.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 3:39:14 PM
 */
public class SsgManagerTest {

    private static final SsgManagerImpl sm = SsgManagerImpl.getSsgManagerImpl();

    private static final Ssg SSG1 =
      new Ssg(1, "bunky1.foo.bar");
    private static final Ssg SSG2 =
      new Ssg(2, "bunky2.foo.bar");
    private static final Ssg SSG3 =
      new Ssg(3, "bunky3.foo.bar");

    static {
        SSG2.setTrustedGateway(SSG1);
    }

    @Before
    public void tearDown() throws Exception {
        SsgFinderImpl.exceptionListener = new SsgFinderTest.FatalExceptionListener();
        sm.clear();
        sm.save();
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

    /**
     * Find how many SSGs are registered under the specified hostname.
     *
     * @param name
     * @return The number of SSGs this.sm with the given name
     */
    private int countNames(String name) {
        int count = 0;
        for (Iterator i = sm.getSsgList().iterator(); i.hasNext();) {
            Ssg ssg = (Ssg)i.next();
            if (name.equals(ssg.getSsgAddress()))
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

    /**
     * Make sure none of our test records are present in the given SMI.
     */
    public void assertNoBunkysIn(final SsgManagerImpl smi) throws Exception {
        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                smi.getSsgByHostname(SSG1.getSsgAddress());
            }
        });

        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                smi.getSsgByHostname(SSG2.getSsgAddress());
            }
        });

        mustThrow(SsgNotFoundException.class, new Testable() {
            public void run() throws Exception {
                smi.getSsgByHostname(SSG3.getSsgAddress());
            }
        });

        Collection unwantedNames = Arrays.asList(new String[]{
            SSG1.getSsgAddress(), SSG2.getSsgAddress(), SSG3.getSsgAddress()
        });
        Collection unwantedEndpoints = Arrays.asList(new String[]{
            SSG1.getLocalEndpoint(), SSG2.getLocalEndpoint(), SSG3.getLocalEndpoint()
        });
        for (Iterator i = smi.getSsgList().iterator(); i.hasNext();) {
            Ssg ssg = (Ssg)i.next();
            if (unwantedNames.contains(ssg.getSsgAddress()) || unwantedEndpoints.contains(ssg.getLocalEndpoint()))
                throw new Exception("Failed to remove all test SSG records");
            ;
        }
    }

    @Test
    public void testPersistence() throws Exception {
        eraseAllSsgs();
        assertTrue(sm.getSsgList().size() == 0);

        // Add a couple of SSGs
        Assertion policy1 = new AllAssertion(Arrays.asList(new Assertion[]{
            new HttpBasic(),
            new SslAssertion()
        }));

        Assertion policy2 = new ExactlyOneAssertion(Arrays.asList(new Assertion[]{
            policy1,
            new HttpDigest(),
            new HttpNegotiate()
        }));

        final String SSG1P1_URI = "http://foodd.bar.baz/asdf/fdsa";
        final String SSG1P1_SA = "http://sdgsdfg.asdf.rq/asdf/fdsa";
        final String SSG1P2_URI = "http://fasdlfh.grq.asdasdf/asdf";
        final String SSG1P2_SA = "http://jkherkjhreg.asdfasdf.qqwer";
        final String SSG2P2_URI = "http://gfsadfj.asfdgha.as/afdsasdf/fdsa";
        final String SSG2P2_SA = "http://foaaao.bar.baz/afdsasdf/fdsa";
        final String SSG2P1_URI = "http://asdf.fasd.awgq/";
        final String SSG2P1_SA = "http://agarg.geqw.qrgq";
        final SsgRuntime s1rt = SSG1.getRuntime();
        final PolicyManager s1pm = s1rt.getPolicyManager();
        s1pm.setPolicy(new PolicyAttachmentKey(SSG1P1_URI, SSG1P1_SA, null), new Policy(policy1, "test"));
        s1pm.setPolicy(new PolicyAttachmentKey(SSG1P2_URI, SSG1P2_SA, null), new Policy(policy2, "test"));
        final Policy ppolicy = new Policy(policy2, "test");
        final PolicyAttachmentKey ppak = new PolicyAttachmentKey(SSG1P2_URI, SSG1P2_SA, null);
        ppak.setPersistent(true);
        s1pm.setPolicy(ppak, ppolicy);
        final SsgRuntime s2rt = SSG2.getRuntime();
        final PolicyManager s2pm = s2rt.getPolicyManager();
        s2pm.setPolicy(new PolicyAttachmentKey(SSG2P2_URI, SSG2P2_SA, null), new Policy(policy2, "test"));
        s2pm.setPolicy(new PolicyAttachmentKey(SSG2P1_URI, SSG2P1_SA, null), new Policy(policy1, "test"));

        Map<String, String> ps = new HashMap<String, String>();
        ps.put("ssg1", "true");
        SSG1.setProperties(ps);

        sm.add(SSG1);
        sm.add(SSG2);
        assertTrue(sm.getSsgList().contains(SSG1));
        assertTrue(sm.getSsgList().contains(SSG2));

        // Find SSGs by name.
        assertTrue(sm.getSsgByHostname(SSG1.getSsgAddress()) == SSG1);
        assertTrue(sm.getSsgByHostname(SSG2.getSsgAddress()) == SSG2);

        // Find SSGs by local endpoint.
        assertTrue(sm.getSsgByEndpoint(SSG1.getLocalEndpoint()) == SSG1);
        assertTrue(sm.getSsgByEndpoint(SSG2.getLocalEndpoint()) == SSG2);

        // Unable to find nonexistent SSGs
        mustThrow(SsgNotFoundException.class, new Testable() {
            @Override
            public void run() throws Exception {
                sm.getSsgByHostname("Bloof Blaz");
            }
        });
        mustThrow(SsgNotFoundException.class, new Testable() {
            @Override
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
        Ssg loaded1 = sm.getSsgByHostname(SSG1.getSsgAddress());
        ps = loaded1.getProperties();
        assertTrue(ps.size() == 1);
        assertEquals(ps.get("ssg1"), "true");
        assertTrue(loaded1 != null);
        assertTrue(loaded1.getSsgAddress() != null);
        assertTrue(loaded1.getLocalEndpoint().equals(SSG1.getLocalEndpoint()));

        // policies tagged persistent get saved
        assertTrue(loaded1.getRuntime().getPolicyManager().getPolicy(new PolicyAttachmentKey(SSG1P2_URI, SSG1P2_SA, null)) != null);

        // policies not tagged as persistent do not get persisted
        assertTrue(loaded1.getRuntime().getPolicyManager().getPolicy(new PolicyAttachmentKey(SSG1P1_URI, SSG1P1_SA, null)) == null);
        assertTrue(loaded1.getRuntime().getPolicyManager().getPolicy(new PolicyAttachmentKey("asdfasdf", "argaerg", null)) == null);

        Ssg loaded2 = sm.getSsgByEndpoint(SSG2.getLocalEndpoint());
        assertTrue(loaded2 != null);
        assertTrue(loaded2.getSsgAddress() != null);
        assertTrue(loaded2.getSsgAddress().equals(SSG2.getSsgAddress()));
        final PolicyManager l2pm = loaded2.getRuntime().getPolicyManager();
        assertTrue(l2pm.getPolicy(new PolicyAttachmentKey(SSG2P1_URI, SSG2P1_SA, null)) == null); // policies not persisted
        assertTrue(l2pm.getPolicy(new PolicyAttachmentKey(SSG2P2_URI, SSG2P2_SA, null)) == null);
        assertTrue(loaded2.getTrustedGateway() != SSG1); // (this doesn't HAVE to fail, but it'd be amazing if it worked)
        assertTrue(loaded2.getTrustedGateway() == sm.getSsgById(SSG1.getId()));

        // May not add two Ssgs with the same Id
        // (Actually you shouldn't even be instantiating two Ssgs with the same Id)
        sm.add(SSG3);
        assertTrue(countNames(SSG3.getSsgAddress()) == 1);

        eraseAllSsgs();
        assertTrue(sm.getSsgList().size() == 0);
    }
}

