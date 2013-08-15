package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditRecordToXmlAssertion;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Set;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ServerAuditRecordToXmlAssertionTest {
    @BeforeClass
    public static void assreg() {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @Test
    public void testMetadata() throws Exception {
        assertEquals(ServerAuditRecordToXmlAssertion.class.getName(),
                new AuditRecordToXmlAssertion().meta().get(AssertionMetadata.SERVER_ASSERTION_CLASSNAME));
    }

    @Test
    public void testSimpleConvertAudit() throws Exception {
        ServerAuditRecordToXmlAssertion sass = new ServerAuditRecordToXmlAssertion(new AuditRecordToXmlAssertion());
        PolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(makeMessageAuditRecord(false), PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null ), null);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        assertEquals("audit", context.getRequest().getXmlKnob().getDocumentReadOnly().getDocumentElement().getLocalName());
    }

    @BugNumber(9575)
    @Test
    public void testAuditDetailsOrder() throws Exception {
        ServerAuditRecordToXmlAssertion sass = new ServerAuditRecordToXmlAssertion(new AuditRecordToXmlAssertion());
        PolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(makeMessageAuditRecord(true), PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null ), null);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document auditDom = context.getRequest().getXmlKnob().getDocumentReadOnly();
        assertEquals("audit", auditDom.getDocumentElement().getLocalName());
        final NodeList details = auditDom.getElementsByTagName( "detail" );
        assertEquals("details count", 10, details.getLength());
        for ( int i=0; i<10; i++) {
            assertEquals( "details ordinal " + (i+1), i+1, Integer.parseInt(((Element)details.item(i)).getAttributeNS( null, "ordinal" )) );
        }
    }

    private AuditRecord makeMessageAuditRecord(boolean details) {
        AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "req4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, new Goid(0,8859), "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, new Goid(0,-2), "alice", "41123", 49585);
        if ( details ) {
            int ordinal = 1;
            final Set<AuditDetail> auditDetails = auditRecord.getDetails();
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.SERVICE_NOT_FOUND, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.SERVICE_DISABLED, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.POLICY_VERSION_WRONG_FORMAT, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.POLICY_ID_NOT_PROVIDED, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.CANNOT_GET_POLICY, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.REQUEST_INVALID_XML_FORMAT, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.MESSAGE_NOT_SOAP, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.MESSAGE_NOT_SOAP_NO_WSS, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.MESSAGE_NO_SIG_CONFIRMATION, ordinal++ ) );
            auditDetails.add( makeAuditDetail( MessageProcessingMessages.ERROR_RETRIEVE_XML, ordinal ) );
        } else {
            final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
            auditRecord.getDetails().add(detail1);
        }
        return auditRecord;
    }

    private AuditDetail makeAuditDetail( final AuditDetailMessage message, final int ordinal ) {
        final AuditDetail auditDetail = new AuditDetail( message );
        auditDetail.setOrdinal( ordinal );
        return auditDetail;
    }
}
