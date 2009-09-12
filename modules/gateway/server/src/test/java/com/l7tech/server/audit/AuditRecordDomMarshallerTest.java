package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.test.BenchmarkRunner;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.MarshalException;
import java.util.logging.Level;

/**
 * Tests the ability to convert audit records to and from XML.
 */
public class AuditRecordDomMarshallerTest {

    @Test
    public void testSimpleMarshalAdmin() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = makeAdminAuditRecord();
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Test
    public void testSimpleMarshalMessage() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = makeMessageAuditRecord();
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Test
    public void testMarshalPerformance() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        final AuditRecord rec = makeAdminAuditRecord();
        final AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        m.marshal(d, rec);

        new BenchmarkRunner(new Runnable() {
            @Override
            public void run() {
                try {
                    Document d = XmlUtil.stringAsDocument("<a/>");
                    for (int i = 0; i < 100; i++) {
                        m.marshal(d, rec);
                    }
                } catch (MarshalException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1000, 8, "Marshal").run();
    }

    private AuditRecord makeAdminAuditRecord() {
        AuditRecord auditRecord = new AdminAuditRecord(Level.INFO, "node1", 1234, User.class.getName(), "testuser", AdminAuditRecord.ACTION_UPDATED, "updated", -1, "admin", "1111", "2.3.4.5");
        auditRecord.setReqId(new RequestId(3, 555));
        auditRecord.setThrown(new RuntimeException("main record throwable"));
        final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
        auditRecord.getDetails().add(detail1);
        return auditRecord;
    }

    private AuditRecord makeMessageAuditRecord() {
        AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "req4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, 8859, "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, -2, "alice", "41123", 49585);
        auditRecord.setThrown(new RuntimeException("main record throwable"));
        final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
        auditRecord.getDetails().add(detail1);
        return auditRecord;
    }
}
