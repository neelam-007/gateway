package com.l7tech.common.audit;

import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.event.Created;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.service.PublishedService;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.hibernate.Session;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditPersistenceTest extends TestCase {
    /**
     * test <code>TimeZoneTest</code> constructor
     */
    public AuditPersistenceTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the TimeZoneTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( AuditPersistenceTest.class );
        return suite;
    }

    public void testSaveAdmin() throws Exception {
        HibernatePersistenceContext context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
        context.beginTransaction();
        Session s = context.getSession();
        Object id = s.save(new AdminAuditRecord(Level.INFO, "thisnode", new Created(new PublishedService()), "admin"));
        System.out.println("Saved " + id);
        context.commitTransaction();
    }

    public void testSaveMessage() throws Exception {
        HibernatePersistenceContext context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
        context.beginTransaction();
        Session s = context.getSession();
        final String requestId = RequestIdGenerator.next().toString();
        Object id = s.save(new MessageSummaryAuditRecord(Level.INFO, "thisnode", requestId, AssertionStatus.NONE, "127.0.0.1", "<haha/>", 7, "<bye/>", 6, 1234, "HelloService", true, 2345, "joe", "3456"));
        System.out.println("Saved " + id);
        context.commitTransaction();
    }

    public void setUp() throws Exception {
        HibernatePersistenceManager.initialize();
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>TimeZoneTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }
}