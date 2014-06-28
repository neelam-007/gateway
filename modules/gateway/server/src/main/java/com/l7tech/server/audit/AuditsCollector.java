package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

// Collects the data required to delay emitting audit records.
public class AuditsCollector extends ArrayList<Triple<AuditRecord,Object,Collection<AuditDetail>>> {

    public void collectAudit(final AdminAuditRecord auditRecord, @Nullable final Object source, @Nullable final Collection<AuditDetail> details){
        add(new Triple<AuditRecord,Object,Collection<AuditDetail>>(auditRecord,source,details));
    }
}
