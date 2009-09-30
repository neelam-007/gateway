package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditRecordToXmlAssertion;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.logging.Level;

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
        ServerAuditRecordToXmlAssertion sass = new ServerAuditRecordToXmlAssertion(new AuditRecordToXmlAssertion(), null, null);
        PolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(makeMessageAuditRecord());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        assertEquals("audit", context.getRequest().getXmlKnob().getDocumentReadOnly().getDocumentElement().getLocalName());
    }

    private AuditRecord makeMessageAuditRecord() {
        AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "req4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, 8859, "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, -2, "alice", "41123", 49585);
        auditRecord.setThrown(new RuntimeException("main record throwable"));
        final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
        auditRecord.getDetails().add(detail1);
        return auditRecord;
    }
}
