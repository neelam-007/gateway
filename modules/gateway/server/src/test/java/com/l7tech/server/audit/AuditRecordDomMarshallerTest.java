package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordDomMarshaller;
import com.l7tech.gateway.common.audit.AuditRecordTest;
import com.l7tech.test.BenchmarkRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.MarshalException;

/**
 * Tests the ability to convert audit records to and from XML.
 */
public class AuditRecordDomMarshallerTest {

    @Test
    public void testSimpleMarshalAdmin() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = AuditRecordTest.makeAdminAuditRecord();
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Test
    public void testSmallMarshalAdmin() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = AuditRecordTest.makeAdminAuditRecord();
        auditRecord.setThrown(null);
        //noinspection deprecation
        auditRecord.getDetails().iterator().next().setException(null);
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Test
    public void testSimpleMarshalMessage() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = AuditRecordTest.makeMessageAuditRecord();
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Test
    public void testSmallMarshalMessage() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = AuditRecordTest.makeMessageAuditRecord();
        auditRecord.setThrown(null);
        //noinspection deprecation
        auditRecord.setOid(232);
        //noinspection deprecation
        auditRecord.getDetails().iterator().next().setException(null);
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Test
    public void testSimpleMarshalSystem() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditRecordDomMarshaller m = new AuditRecordDomMarshaller();
        AuditRecord auditRecord = AuditRecordTest.makeSystemAuditRecord();
        Element got = m.marshal(d, auditRecord);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);
    }

    @Ignore("Enable this test to run a performance test of audit record DOM marshalling")
    @Test
    public void testMarshalPerformance() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        final AuditRecord rec = AuditRecordTest.makeAdminAuditRecord();
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
}
