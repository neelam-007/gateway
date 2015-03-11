package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.server.mapping.MessageContextMappingKeyManager;
import com.l7tech.server.mapping.MessageContextMappingValueManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by vkazakov on 3/10/2015.
 */
public class AuditRecordManagerTest extends EntityManagerTest {
    private AuditRecordManager auditRecordManager;
    private MessageContextMappingValueManager messageContextMappingValueManager;
    private MessageContextMappingKeyManager messageContextMappingKeyManager;

    @Before
    public void setUp() throws Exception {
        auditRecordManager = applicationContext.getBean("auditRecordManager", AuditRecordManager.class);
        messageContextMappingValueManager = applicationContext.getBean("messageContextMappingValueManager", MessageContextMappingValueManager.class);
        messageContextMappingKeyManager = applicationContext.getBean("messageContextMappingKeyManager", MessageContextMappingKeyManager.class);
    }

    @After
    public void tearDown() throws Exception {
        Collection<AuditRecord> auditRecords = auditRecordManager.findAll();
        for (AuditRecord auditRecord : auditRecords) {
            auditRecordManager.delete(auditRecord);
        }
    }

    @Test
    public void testAdminAuditRecord() throws SaveException {
        AdminAuditRecord adminAuditRecord = new AdminAuditRecord(Level.FINE,
                UUID.randomUUID().toString().replaceAll("-", ""),
                new Goid(123, 456),
                "ENTITY_CLASS",
                "entityName",
                AdminAuditRecord.ACTION_CREATED,
                "test audit record",
                new Goid(0, 1),
                "admin",
                new Goid(0, 3).toString(),
                "127.0.0.1");
        AuditDetail auditDetail = new AuditDetail(new AuditDetailMessage(1, Level.FINE, "detail Message"), "myParam");
        auditDetail.setAuditRecord(adminAuditRecord);
        adminAuditRecord.setDetails(CollectionUtils.set(auditDetail));
        auditRecordManager.save(adminAuditRecord);

        session.flush();
    }

    @Test
    public void testSystemAuditRecord() throws SaveException {
        SystemAuditRecord adminAuditRecord = new SystemAuditRecord(Level.FINE,
                UUID.randomUUID().toString().replaceAll("-", ""),
                Component.GATEWAY,
                "test audit record",
                true,
                new Goid(0, 1),
                "admin",
                new Goid(0, 3).toString(),
                "action",
                "127.0.0.1");
        AuditDetail auditDetail = new AuditDetail(new AuditDetailMessage(1, Level.FINE, "detail Message"), "myParam");
        auditDetail.setAuditRecord(adminAuditRecord);
        adminAuditRecord.setDetails(CollectionUtils.set(auditDetail));
        auditRecordManager.save(adminAuditRecord);

        session.flush();
    }

    @Test
    public void testMessageSummaryAuditRecord() throws SaveException, FindException, DeleteException {
        final MessageContextMappingKeys messageContextMappingKeys = createMessageContextMappingKeys();
        final MessageContextMappingValues messageContextMappingValues = createMessageContextMappingValues(messageContextMappingKeys.getGoid());
        try {
            MessageSummaryAuditRecord adminAuditRecord = new MessageSummaryAuditRecord(Level.FINE,
                    UUID.randomUUID().toString().replaceAll("-", ""),
                    UUID.randomUUID().toString().replaceAll("-", ""),
                    AssertionStatus.FAILED,
                    "clientAdr",
                    "<request></request>",
                    1,
                    "<responseXml></responseXml>",
                    1,
                    2,
                    3,
                    new Goid(123, 456),
                    "serviceName",
                    "Operation_name",
                    true,
                    SecurityTokenType.HTTP_BASIC,
                    new Goid(0, 1),
                    "admin",
                    new Goid(0, 3).toString(),
                    new Functions.Nullary<Goid>() {
                        @Override
                        public Goid call() {
                            return messageContextMappingValues.getGoid();
                        }
                    });
            AuditDetail auditDetail = new AuditDetail(new AuditDetailMessage(1, Level.FINE, "detail Message"), "myParam");
            auditDetail.setAuditRecord(adminAuditRecord);
            adminAuditRecord.setDetails(CollectionUtils.set(auditDetail));
            auditRecordManager.save(adminAuditRecord);

            session.flush();
        } finally {
            messageContextMappingValueManager.delete(messageContextMappingValues);
            messageContextMappingKeyManager.delete(messageContextMappingKeys);
        }
    }

    private MessageContextMappingKeys createMessageContextMappingKeys() throws SaveException {
        MessageContextMappingKeys messageContextMappingKeys = new MessageContextMappingKeys();
        messageContextMappingKeys.setDigested("digested");
        messageContextMappingKeyManager.save(messageContextMappingKeys);
        return messageContextMappingKeys;
    }

    private MessageContextMappingValues createMessageContextMappingValues(Goid keysId) throws SaveException, FindException {
        MessageContextMappingValues messageContextMappingValues = new MessageContextMappingValues();
        messageContextMappingValues.setDigested("digested");
        messageContextMappingValues.setMappingKeysGoid(keysId);
        messageContextMappingValueManager.save(messageContextMappingValues);
        return messageContextMappingValues;
    }
}
