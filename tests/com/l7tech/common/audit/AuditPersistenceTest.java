package com.l7tech.common.audit;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.service.PublishedService;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.hibernate.Session;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
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
        Object id = s.save(new AdminAuditRecord(Level.INFO, "thisnode", 1234, PublishedService.class.getName(), "My Service", AdminAuditRecord.ACTION_CREATED, "Created", "admin", InetAddress.getLocalHost().getHostAddress()));
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

    public void testFind() throws Exception {
        AuditRecordManager man = (AuditRecordManager) Locator.getDefault().lookup(AuditRecordManager.class);
        Collection found = man.find(new AuditSearchCriteria(null, 0, 0, 0));
        for ( Iterator i = found.iterator(); i.hasNext(); ) {
            AuditRecord auditRecord = (AuditRecord) i.next();
            System.out.println(auditRecord.toString());
        }
    }

    public void setUp() throws Exception {
        HibernatePersistenceManager.initialize(null);
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