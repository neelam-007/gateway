package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
public class AuditRecordSelectorTest {
    private static final Logger logger = Logger.getLogger(AuditRecordSelectorTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);
    private static final String MSG_INVALID_NUMERIC_INDEX_FOR_AUDIT_SELECTOR = "Invalid numeric index for audit detail lookup";
    private static final String MSG_NUMERIC_INDEX_OUT_OF_BOUNDS_FOR_AUDIT_SELECTOR = "Index out of bounds for audit detail lookup";

    private AuditRecord auditRecord;
    private Map<String, Object> vars;
    private Set<AuditDetail> details;
    private AuditDetail[] auditDetails;

    @Mock
    private Logger mockLogger;

    @Before
    public void setup() {
        auditRecord = AuditRecordSelectorTest.getExampleMessageSummaryAuditRecord();
        details = new HashSet<>();
        AuditDetail ad1 = new AuditDetail(AssertionMessages.EXCEPTION_SEVERE);
        ad1.setOrdinal(0);
        AuditDetail ad2 = new AuditDetail(AssertionMessages.EXCEPTION_WARNING);
        ad2.setOrdinal(1);
        details.add(ad1);
        details.add(ad2);
        auditRecord.setDetails(details);
        vars = new HashMap<String, Object>() {{
            put("auditrecord", auditRecord);
        }};

        auditDetails = new AuditDetail[3];
        auditDetails[0] = ad1;
        auditDetails[1] = ad2;
        auditDetails[2] = new AuditDetail(AssertionMessages.EXCEPTION_INFO);
    }

    @Test
    public void testAuditRecordSelector() throws Exception {
        assertEquals(Integer.toString(AssertionMessages.EXCEPTION_SEVERE.getId()), ExpandVariables.process("${auditrecord.details.0.messageId}", vars, audit));
        assertEquals(Integer.toString(AssertionMessages.EXCEPTION_WARNING.getId()), ExpandVariables.process("${auditrecord.details.1.messageId}", vars, audit));
    }

    @BugNumber(13278)
    @Test
    public void testAuditRecordSelectorLength() throws Exception {
        assertEquals("", ExpandVariables.process("${auditrecord.length}", vars, audit));
        assertEquals(Integer.toString(details.size()), ExpandVariables.process("${auditrecord.details.length}", vars, audit));
    }

    @Test
    public void testAuditRecordSelectorNormalCase() throws Exception {
        ExpandVariables.Selector.Selection myDetail = AuditRecordSelector.selectDetails("details.0.params.length", auditDetails, mockLogger);
        assertNotNull(myDetail);
        assertEquals("params.length", myDetail.getRemainingName());

        assertEquals(auditDetails[0], myDetail.getSelectedValue());
        assertNotEquals(auditDetails[1], myDetail.getSelectedValue());
    }

    @Test
    public void testAuditRecordSelectorOutOfBounds() throws Exception {
        assertNotNull(AuditRecordSelector.selectDetails("details.0", auditDetails, mockLogger));
        assertNotNull(AuditRecordSelector.selectDetails("details.2", auditDetails, mockLogger));
        verify(mockLogger, times(0)).fine(MSG_NUMERIC_INDEX_OUT_OF_BOUNDS_FOR_AUDIT_SELECTOR);
        assertNull(AuditRecordSelector.selectDetails("details.3", auditDetails, mockLogger));
        verify(mockLogger, times(1)).fine(MSG_NUMERIC_INDEX_OUT_OF_BOUNDS_FOR_AUDIT_SELECTOR);
        assertNull(AuditRecordSelector.selectDetails("details.10", auditDetails, mockLogger));
        verify(mockLogger, times(2)).fine(MSG_NUMERIC_INDEX_OUT_OF_BOUNDS_FOR_AUDIT_SELECTOR);
    }

    @Test
    public void testAuditRecordSelectorDotLength() throws Exception {
        verify(mockLogger, times(0)).warning(MSG_INVALID_NUMERIC_INDEX_FOR_AUDIT_SELECTOR);
        assertNull(AuditRecordSelector.selectDetails("details.thisIsNotAnIndexOrLastOrLength", auditDetails, mockLogger));
        assertNull(AuditRecordSelector.selectDetails("details.shouldInvalidNumericIndex", auditDetails, mockLogger));
        verify(mockLogger, times(2)).warning(MSG_INVALID_NUMERIC_INDEX_FOR_AUDIT_SELECTOR);
        assertNull(AuditRecordSelector.selectDetails("details.length", auditDetails, mockLogger));
        verify(mockLogger, times(2)).warning(MSG_INVALID_NUMERIC_INDEX_FOR_AUDIT_SELECTOR);
    }

    private static MessageSummaryAuditRecord getExampleMessageSummaryAuditRecord() {
        return new MessageSummaryAuditRecord(Level.INFO, "node1", "2342345-4545", AssertionStatus.NONE,
                "3.2.1.1", null, 4833, null, 9483,
                200, 232, new Goid(0, 8859), "ACMEWarehouse",
                "listProducts", true, SecurityTokenType.HTTP_BASIC, new Goid(0, -2),
                "alice", "41123", null);
    }
}

