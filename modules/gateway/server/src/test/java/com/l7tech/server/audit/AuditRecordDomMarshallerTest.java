package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.test.BenchmarkRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.*;

import javax.xml.bind.MarshalException;

import static junit.framework.Assert.assertEquals;

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
        //noinspection deprecation
        auditRecord.setGoid(new Goid(236,232));
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

    @Test
    public void testSimpleAuditDetailUnmarshal() throws Exception {
        Document d = XmlUtil.stringAsDocument("<a/>");
        AuditDetailPropertiesDomMarshaller m = new AuditDetailPropertiesDomMarshaller();
        String[] origParams = new String[]{"foomp"};
        AuditDetail detail =  new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO,origParams, new IllegalArgumentException("Exception for foomp detail"));
        Element got = m.marshal(d, detail);
        XmlUtil.nodeToFormattedOutputStream(got, System.out);

        for(int i = 0 ; i < got.getChildNodes().getLength() ; ++i){
            if( got.getChildNodes().item(i).getNodeName().equals("params"))
            {
                Node paramsNode = got.getChildNodes().item(0);
                NodeList paramNodeList = paramsNode.getChildNodes();

                String[] params = new String[paramNodeList.getLength()];
                for(int j = 0 ; j < paramNodeList.getLength(); ++j){
                    Node paramNode = paramNodeList.item(j);
                    params[j]=paramNode.getChildNodes().item(0).getNodeValue();
                }

                for(int k = 0 ; k < origParams.length ; ++k){
                    assertEquals(origParams[k], params[k]);
                }
            }
        }

    }
}
