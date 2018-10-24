package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugId;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.util.Properties;
import java.util.logging.Level;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuditContextImplTest {
    private AuditRecord record = new MessageSummaryAuditRecord(Level.WARNING, null, null, AssertionStatus.NONE,
            null, null, 0,
            null, 0, 0, 0,
            null, null, null,
            false, null, null,
            null, null, null);
    @Mock
    private AuditLogListener auditLogListener;
    @Mock
    private AuditRecordManager auditRecordManager;

    @BugId("DE269676")
    @Test
    public void flushProcessesDetailsFromSingleSourceInAddedOrder() {
        Config config = new MockConfig(new Properties());
        AuditContextImpl context = makeAuditContext(config, null);
        context.setCurrentRecord(record);

        final AuditDetailAssertion source = new AuditDetailAssertion();
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "first log"), source);
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "second log"), source);
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "third log"), source);

        context.flush();
        final InOrder order = inOrder(auditLogListener);
        order.verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String) null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"first log"}), any(AuditLogFormatter.class), eq((Throwable) null));
        order.verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String) null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"second log"}), any(AuditLogFormatter.class), eq((Throwable) null));
        order.verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String) null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"third log"}), any(AuditLogFormatter.class), eq((Throwable) null));
    }

    @BugId("US550347")
    @Test
    public void notSaveMessageAuditToDB() throws Exception {
        Properties configProperties = new Properties();
        configProperties.put(ServerConfigParams.PARAM_AUDIT_MESSAGE_SAVE_TO_INTERNAL, "false");
        Config config = new MockConfig(configProperties);

        AuditContextImpl context = makeAuditContext(config, null);
        context.setCurrentRecord(record);

        final AuditDetailAssertion source = new AuditDetailAssertion();
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "first log"), source);

        context.flush();
        verify(auditRecordManager, never()).save(any(AuditRecord.class));
        verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String) null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"first log"}), any(AuditLogFormatter.class), eq((Throwable) null));
    }

    @BugId("US550347")
    @Test
    public void notSaveSystemAuditToDB() throws Exception {
        Properties configProperties = new Properties();
        configProperties.put(ServerConfigParams.PARAM_AUDIT_SYSTEM_SAVE_TO_INTERNAL, "false");
        Config config = new MockConfig(configProperties);

        AuditContextImpl context = makeAuditContext(config, null);

        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO, "node1", Component.GW_TRUST_STORE, "One or more trusted certificates has expired or is expiring soon", true, new Goid(0, -1), null, null, "Checking", "192.168.1.42");
        context.setCurrentRecord(auditRecord);

        context.flush();
        verify(auditRecordManager, never()).save(any(AuditRecord.class));
        verify(auditLogListener, atLeastOnce()).notifyRecordFlushed(eq(auditRecord), any(AuditLogFormatter.class), anyBoolean());
    }

    @BugId("US550347")
    @Test
    public void notSaveAdminAuditToDB() throws Exception {
        Properties configProperties = new Properties();
        configProperties.put(ServerConfigParams.PARAM_AUDIT_ADMIN_SAVE_TO_INTERNAL, "false");
        Config config = new MockConfig(configProperties);

        AuditContextImpl context = makeAuditContext(config, null);

        AuditRecord auditRecord = AuditRecordTest.makeAdminAuditRecord();
        context.setCurrentRecord(auditRecord);

        context.flush();
        verify(auditRecordManager, never()).save(any(AuditRecord.class));
        verify(auditLogListener, atLeastOnce()).notifyRecordFlushed(eq(auditRecord), any(AuditLogFormatter.class), anyBoolean());
    }

    @BugId("US550347")
    @Test
    public void auditSinkFailNotSaveToDB() throws Exception {
        Properties configProperties = new Properties();
        configProperties.put(ServerConfigParams.PARAM_AUDIT_SYSTEM_SAVE_TO_INTERNAL, "false");
        Config config = new MockConfig(configProperties);

        AuditPolicyEvaluator auditPolicyEvaluator = mock(AuditPolicyEvaluator.class);
        when(auditPolicyEvaluator.outputRecordToPolicyAuditSink(any(AuditRecord.class), any(PolicyEnforcementContext.class))).thenReturn(AssertionStatus.FALSIFIED);
        AuditContextImpl context = makeAuditContext(config, auditPolicyEvaluator);

        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO, "node1", Component.GW_TRUST_STORE, "One or more trusted certificates has expired or is expiring soon", true, new Goid(0, -1), null, null, "Checking", "192.168.1.42");
        context.setCurrentRecord(auditRecord);

        context.flush();
        verify(auditRecordManager, never()).save(any(AuditRecord.class));
        verify(auditLogListener, atLeastOnce()).notifyRecordFlushed(eq(auditRecord), any(AuditLogFormatter.class), anyBoolean());
    }

    @NotNull
    private AuditContextImpl makeAuditContext(Config config, AuditPolicyEvaluator auditPolicyEvaluator) {
        return new AuditContextImpl(config,
                auditRecordManager,
                auditPolicyEvaluator,
                null,
                null,
                null,
                auditLogListener);
    }
}
