package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuditSelectorTest {
    private static final Logger logger = Logger.getLogger(AuditSelectorTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);

    @Test
    public void testAuditSelector() throws Exception {
        final AuditContext ac = new AuditContextStub();
        final AuditSelector.AuditHolder ah = new AuditSelector.AuditHolder(ac);
        AuditDetail ad = new AuditDetail(MessageProcessingMessages.COMPLETION_STATUS, new String[]{"p1", "p2"}, new NullPointerException());
        ad.setOrdinal(0);
        ac.addDetail(ad, this);
        AuditDetail ad2 = new AuditDetail(MessageProcessingMessages.ERROR_MESSAGE_COMPLETED);
        ad2.setOrdinal(1);
        ac.addDetail(ad2, this);

        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("audit", ah);
        }};
        assertEquals(Integer.toString(ad.getMessageId()), ExpandVariables.process("${audit.details.0.messageId}", vars, audit));
        assertTrue(ExpandVariables.process("${audit.details.0.exception}", vars, audit).startsWith(ad.getException()));

        assertEquals("p1, p2", ExpandVariables.process("${audit.details.0.params}", vars, audit));
        assertNotNull(ExpandVariables.process("${audit.details.0.time}", vars, audit));
        assertEquals(Integer.toString(ad2.getMessageId()), ExpandVariables.process("${audit.details.1.messageId}", vars, audit));

    }

    @BugNumber(13278)
    @Test
    public void testAuditSelectorLength() throws Exception {
        final AuditContext ac = new AuditContextStub();
        final AuditSelector.AuditHolder ah = new AuditSelector.AuditHolder(ac);
        AuditDetail ad = new AuditDetail(MessageProcessingMessages.COMPLETION_STATUS, new String[]{"p1", "p2"}, new NullPointerException());
        ad.setOrdinal(0);
        ac.addDetail(ad, this);
        AuditDetail ad2 = new AuditDetail(MessageProcessingMessages.ERROR_MESSAGE_COMPLETED);
        ad2.setOrdinal(1);
        ac.addDetail(ad2, this);

        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("audit", ah);
        }};
        assertEquals("2", ExpandVariables.process("${audit.details.length}", vars, audit));
        assertEquals("2", ExpandVariables.process("${audit.details.0.params.length}", vars, audit));

    }
}
