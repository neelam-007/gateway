package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.test.BugNumber;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import org.junit.Test;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import static org.junit.Assert.*;

/**
 * Puts audit records through their paces.
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class AuditRecordTest {
    // Any audit record (or one of its three tested subclasses) propery listed here is not expected to be covered by the signature.
    // All other properties ARE expected to be covered by the signature.
    private String[] propertiesNotCoveredBySignature = {
            // Omitted because they change after creation
            "oid",
            "signature",
            "version", 

            // inherited from LogRecord and probably not useful to include in sig
            "sequenceNumber",

            // TODO These should probably be included
            "mappingValuesEntity",
            "mappingValuesOid",

            // This one apparently IS included but this test currently does not appear to permute it properly
            "strRequestId",

            // Security zone is just for SSM rbac
            "securityZone"
    };

    int ordinal = 1;
    long gen = 23423;
    long seq = 53553;

    @Test
    @BugNumber(8968)
    public void testSigningSystem() throws Exception {
        testSignatureDigest(makeSystemAuditRecord());
    }

    @Test
    @BugNumber(8968)
    public void testSigningMessage() throws Exception {
        testSignatureDigest(makeMessageAuditRecord());
    }

    @Test
    @BugNumber(8968)
    public void testSigningAdmin() throws Exception {
        testSignatureDigest(makeAdminAuditRecord());
    }

    private void testSignatureDigest(AuditRecord rec) throws Exception {
        Set<PropertyDescriptor> props = BeanUtils.omitProperties(BeanUtils.getProperties(rec.getClass()), propertiesNotCoveredBySignature);

        // Ensure digest changes when any property is modified
        for (PropertyDescriptor prop : props) {
            testSignatureDigest(rec, prop);
        }
    }


    private void testSignatureDigest(AuditRecord rec, PropertyDescriptor propertyToPermute) throws Exception {
        String propName = propertyToPermute.getName();

        byte[] oldDigest = rec.computeSignatureDigest();
        assertNotNull("computeSignatureDigest must return a non-null hash", oldDigest);

        AuditRecord backup = rec.getClass().newInstance();
        BeanUtils.copyProperties(rec, backup);

        permuteProperty(rec, propertyToPermute);

        byte[] newDigest = rec.computeSignatureDigest();
        assertFalse("Hash must change after permuting property: " + propName, Arrays.equals(oldDigest, newDigest));

        restoreProperty(rec, propertyToPermute, backup);

        byte[] restoredDigest = rec.computeSignatureDigest();
        assertTrue("Hash must be restored after restoring property: " + propName, Arrays.equals(oldDigest, restoredDigest));
    }

    public void permuteProperty(AuditRecord rec, PropertyDescriptor pd) throws InvocationTargetException, IllegalAccessException {
        String propName = pd.getName();

        Object prop = pd.getReadMethod().invoke(rec);

        Object newProp;
        if ("strLvl".equals(propName)) {
            newProp = "INFO".equals(prop) ? "WARNING" : "INFO";
        } else if ("level".equals(propName)) {
            newProp = Level.INFO.equals(prop) ? Level.WARNING : Level.INFO;
        } else if (pd.getPropertyType().isAssignableFrom(RequestId.class)) {
            newProp = new RequestId(gen++, seq++);
        } else if ("authenticationType".equals(propName)) {
            newProp = prop != null ? null : SecurityTokenType.FTP_CREDENTIAL;
        } else if ("details".equals(propName)) {
            newProp = prop != null && ((Collection)prop).size() > 0 ? new HashSet<AuditDetail>()
                    : new HashSet<AuditDetail>(Arrays.asList(newDetail(MessageProcessingMessages.EVENT_MANAGER_EXCEPTION)));
        } else if ("action".equals(propName) && rec instanceof AdminAuditRecord) {
            newProp = "U".equals(String.valueOf(prop)) ? 'D' : 'U';
        } else {
            newProp = permuteValue(prop, pd.getPropertyType());
        }

        if (newProp != prop) {
            try {
                pd.getWriteMethod().invoke(rec, newProp);
            } catch (Exception e) {
                fail("Failed permuting " + propName + " value of " + prop + " to " + newProp + ": " + ExceptionUtils.getMessage(e));
            }
        } else {
            throw new RuntimeException("Didn't permute " + prop);
        }
    }

    public static Object permuteValue(Object prop, Class propClass) {
        if (String.class == propClass) {
            return prop + " yop";
        } else if (Integer.class == propClass || int.class == propClass) {
            Integer i = (Integer)prop;
            return i == null ? 1 : i + 1;
        } else if (Long.class == propClass || long.class == propClass) {
            Long l = (Long)prop;
            return l == null ? 1 : l + 1;
        } else if (Byte.class == propClass || byte.class == propClass) {
            Byte b = (Byte)prop;
            return b == null ? 1 : b + 1;
        } else if (Short.class == propClass || short.class == propClass) {
            Short s = (Short)prop;
            return s == null ? 1 : s + 1;
        } else if (Character.class == propClass || char.class == propClass) {
            Character c = (Character)prop;
            return c == null ? 1 : c + 1;
        } else {
            throw new RuntimeException("Don't know how to permute: " + propClass);
        }
    }

    private void restoreProperty(AuditRecord rec, PropertyDescriptor propertyToPermute, AuditRecord backup) throws InvocationTargetException, IllegalAccessException {
        BeanUtils.copyProperties(backup, rec, Arrays.asList(propertyToPermute));
    }

    @Test
    public void testOrderedDetails() {
        AuditRecord rec = makeMessageAuditRecord();

        Set<AuditDetail> details = new HashSet<AuditDetail>();        
        details.add(newDetail(AssertionMessages.EXCEPTION_SEVERE));
        details.add(newDetail(SystemMessages.AUDIT_ARCHIVER_ERROR));
        details.add(newDetail(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO));
        rec.setDetails(details);

        AuditDetail[] ordered = rec.getDetailsInOrder();
        assertEquals(ordered[0].getMessageId(), AssertionMessages.EXCEPTION_SEVERE.getId());
        assertEquals(ordered[1].getMessageId(), SystemMessages.AUDIT_ARCHIVER_ERROR.getId());
        assertEquals(ordered[2].getMessageId(), MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO.getId());
    }

    public AuditDetail newDetail(AuditDetailMessage msg) {
        AuditDetail detail = new AuditDetail(msg);
        detail.setOrdinal(ordinal++);
        return detail;
    }

    public static AuditRecord makeAdminAuditRecord() {
        AuditRecord auditRecord = new AdminAuditRecord(Level.INFO, "node1", 1234, User.class.getName(), "testuser", AdminAuditRecord.ACTION_UPDATED, "updated", -1, "admin", "1111", "2.3.4.5");
        auditRecord.setStrRequestId(new RequestId(3, 555).toString());
        final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
        auditRecord.getDetails().add(detail1);
        return auditRecord;
    }

    public static AuditRecord makeMessageAuditRecord() {
        AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "2342345-4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, 8859, "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, -2, "alice", "41123", 49585);
        final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
        auditRecord.getDetails().add(detail1);
        return auditRecord;
    }

    public static AuditRecord makeSystemAuditRecord() {
        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO, "node1", Component.GW_TRUST_STORE, "One or more trusted certificates has expired or is expiring soon", false, -1, null, null, "Checking", "192.168.1.42");
        return auditRecord;
    }
}
