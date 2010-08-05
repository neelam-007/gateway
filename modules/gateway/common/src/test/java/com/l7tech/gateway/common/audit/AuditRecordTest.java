package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.util.Arrays;
import java.util.logging.Level;

import static org.junit.Assert.*;

/**
 * Puts audit records through their paces.
 */
public class AuditRecordTest {

    @Test
    @BugNumber(8968)
    public void testSigningSystem() throws Exception {
        testSigning(makeSystemAuditRecord());
    }

    @Test
    @BugNumber(8968)
    public void testSigningMessage() throws Exception {
        testSigning(makeMessageAuditRecord());
    }

    @Test
    @BugNumber(8968)
    public void testSigningAdmin() throws Exception {
        testSigning(makeAdminAuditRecord());
    }

    private void testSigning(AuditRecord rec) throws Exception {
        rec.setSignature("dummy");

        byte[] oldDigest = rec.computeSignatureDigest();
        assertNotNull(oldDigest);

        String oldMessage = rec.getMessage();
        rec.setMessage(oldMessage + " yop");

        byte[] newDigest = rec.computeSignatureDigest();
        assertFalse(Arrays.equals(oldDigest, newDigest));

        rec.setMessage(oldMessage);
        byte[] restoredDigest = rec.computeSignatureDigest();
        assertTrue(Arrays.equals(oldDigest, restoredDigest));
    }

    @Test
    public void testOrderedDetails() {
        AuditRecord rec = makeMessageAuditRecord();

        rec.getDetails().add(new AuditDetail(AssertionMessages.EXCEPTION_SEVERE));
        rec.getDetails().add(new AuditDetail(SystemMessages.AUDIT_ARCHIVER_ERROR));
        rec.getDetails().add(new AuditDetail(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO));

        AuditDetail[] ordered = rec.getDetailsInOrder();
        assertEquals(ordered[0].getMessageId(), AssertionMessages.EXCEPTION_SEVERE.getId());
        assertEquals(ordered[1].getMessageId(), SystemMessages.AUDIT_ARCHIVER_ERROR.getId());
        assertEquals(ordered[2].getMessageId(), MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO.getId());
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

    private AuditRecord makeSystemAuditRecord() {
        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO, "node1", Component.GW_TRUST_STORE, "One or more trusted certificates has expired or is expiring soon", false, -1, null, null, "Checking", "192.168.1.42");
        auditRecord.setThrown(new RuntimeException("main record throwable"));
        return auditRecord;
    }
}
