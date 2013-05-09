package com.l7tech.server;

import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.server.audit.AuditRecordManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.logging.Level;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EntityFinderImplTest {
    private EntityFinderImpl finder;
    @Mock
    private AuditRecordManager auditRecordManager;

    @Before
    public void setup() {
        finder = new EntityFinderImpl();
        finder.setAuditRecordManager(auditRecordManager);
    }

    @Test
    public void findAuditRecordHeaderDelegatesToManager() throws Exception {
        final AuditRecordHeader auditRecordHeader = new AuditRecordHeader(1234L, "Name", "Description", null, null, "nodeId", 1234L, Level.INFO, 0);
        finder.find(auditRecordHeader);
        verify(auditRecordManager).findByHeader(auditRecordHeader);
    }
}
