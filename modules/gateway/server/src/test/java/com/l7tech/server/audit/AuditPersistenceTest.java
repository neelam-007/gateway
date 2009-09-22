package com.l7tech.server.audit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.BeforeClass;

/**
 * todo: restore this test
 * @author alex
 */
@Ignore
public class AuditPersistenceTest {

    @Test
    public void testSaveAdmin() throws Exception {
//        HibernatePersistenceContext context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
//        context.beginTransaction();
//        Session s = context.getSession();
//        Object id = s.save(new AdminAuditRecord(Level.INFO, "thisnode", 1234, PublishedService.class.getName(), "My Service", AdminAuditRecord.ACTION_CREATED, "Created", "admin", InetAddress.getLocalHost().getHostAddress()));
//        System.out.println("Saved " + id);
//        context.commitTransaction();
    }

    @Test
    public void testSaveMessage() throws Exception {
//        HibernatePersistenceContext context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
//        context.beginTransaction();
//        Session s = context.getSession();
//        final String requestId = RequestIdGenerator.next().toString();
//        Object id = s.save(new MessageSummaryAuditRecord(Level.INFO, "thisnode", requestId, AssertionStatus.NONE, "127.0.0.1", "<haha/>", 7, "<bye/>", 6, 1234, "HelloService", true, 2345, "joe", "3456"));
//        System.out.println("Saved " + id);
//        context.commitTransaction();
    }

    @Test
    public void testFind() throws Exception {
//        AuditRecordManager man = (AuditRecordManager) Locator.getDefault().lookup(AuditRecordManager.class);
//        Collection found = man.find(new AuditSearchCriteria(null, 0, 0, 0));
//        for ( Iterator i = found.iterator(); i.hasNext(); ) {
//            AuditRecord auditRecord = (AuditRecord) i.next();
//            System.out.println(auditRecord.toString());
//        }
    }

    @BeforeClass
    public void setUp() throws Exception {
        //HibernatePersistenceManager.initialize(null);
    }

}