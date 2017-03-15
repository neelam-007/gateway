package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.test.BugId;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;
import java.util.logging.Level;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuditContextImplTest {
    private AuditContextImpl context;
    private Config config;
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

    @Before
    public void setup() {
        config = new MockConfig(new Properties());
        context = new AuditContextImpl(config,
                auditRecordManager,
                null,
                null,
                null,
                null,
                auditLogListener);
        context.setCurrentRecord(record);
    }

    @BugId("DE269676")
    @Test
    public void flushProcessesDetailsFromSingleSourceInAddedOrder() {
        final AuditDetailAssertion source = new AuditDetailAssertion();
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "first log"), source);
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "second log"), source);
        context.addDetail(new AuditDetail(AssertionMessages.USERDETAIL_WARNING, "third log"), source);

        context.flush();
        final InOrder order = inOrder(auditLogListener);
        order.verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String)null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"first log"}), any(AuditLogFormatter.class), eq((Throwable)null));
        order.verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String)null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"second log"}), any(AuditLogFormatter.class), eq((Throwable)null));
        order.verify(auditLogListener).notifyDetailFlushed(eq(AuditDetailAssertion.class.getName()), eq((String)null),
                eq(AssertionMessages.USERDETAIL_WARNING), eq(new String[]{"third log"}), any(AuditLogFormatter.class), eq((Throwable)null));
    }
}
