package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class AuditRecordSelectorTest {
    private static final Logger logger = Logger.getLogger(AuditRecordSelectorTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);

    @Test
    public void testAuditRecordSelector() throws Exception {
        final AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "2342345-4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, 8859, "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, -2, "alice", "41123", 49585);
        Set<AuditDetail> details = new HashSet<AuditDetail>();
        AuditDetail ad1 = new AuditDetail(AssertionMessages.EXCEPTION_SEVERE);
        ad1.setOrdinal(0);
        AuditDetail ad2 = new AuditDetail(AssertionMessages.EXCEPTION_WARNING);
        ad2.setOrdinal(1);
        details.add(ad1);
        details.add(ad2);
        auditRecord.setDetails(details);
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("auditrecord", auditRecord);
        }};
        assertEquals(Integer.toString(AssertionMessages.EXCEPTION_SEVERE.getId()), ExpandVariables.process("${auditrecord.details.0.messageId}", vars, audit));
        assertEquals(Integer.toString(AssertionMessages.EXCEPTION_WARNING.getId()), ExpandVariables.process("${auditrecord.details.1.messageId}", vars, audit));


    }

    @BugNumber(13278)
    @Test
    public void testAuditRecordSelectorLength() throws Exception {
        final AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "2342345-4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, 8859, "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, -2, "alice", "41123", 49585);
        Set<AuditDetail> details = new HashSet<AuditDetail>();
        AuditDetail ad1 = new AuditDetail(AssertionMessages.EXCEPTION_SEVERE);
        ad1.setOrdinal(0);
        AuditDetail ad2 = new AuditDetail(AssertionMessages.EXCEPTION_WARNING);
        ad2.setOrdinal(1);
        details.add(ad1);
        details.add(ad2);
        auditRecord.setDetails(details);
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("auditrecord", auditRecord);
        }};
        assertEquals("", ExpandVariables.process("${auditrecord.length}", vars, audit));
        assertEquals(Integer.toString(details.size()), ExpandVariables.process("${auditrecord.details.length}", vars, audit));


    }
}
