package com.l7tech.external.assertions.concall.server;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.server.audit.AuditContext;

import java.util.*;

/**
 * An audit context that collects audit details for the current thread but never sends them anywhere.
 * The expectation is that users of this context will collect the details later on and do something with them.
 * Used by the ServerConcurrentAllAssertion.
 */
class DetailCollectingAuditContext implements AuditContext {
    private final Map<Object, List<AuditDetailEvent.AuditDetailWithInfo>> details = new LinkedHashMap<Object, List<AuditDetailEvent.AuditDetailWithInfo>>();

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail(new AuditDetailEvent.AuditDetailWithInfo(source, detail, null, null));
    }

    @Override
    public void addDetail(AuditDetailEvent.AuditDetailWithInfo auditDetailInfo) {
        Object source = auditDetailInfo.getSource();
        List<AuditDetailEvent.AuditDetailWithInfo> list = details.get(source);
        if (list == null) {
            list = new ArrayList<AuditDetailEvent.AuditDetailWithInfo>();
            details.put(source, list);
        }
        list.add(auditDetailInfo);
    }

    @Override
    public Set getHints() {
        return Collections.emptySet();
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        Map<Object,List<AuditDetail>> ads = new HashMap<Object,List<AuditDetail>>();

        for ( Map.Entry<Object,List<AuditDetailEvent.AuditDetailWithInfo>> entry : details.entrySet() ) {
            List<AuditDetail> ds = new ArrayList<AuditDetail>();
            for ( AuditDetailEvent.AuditDetailWithInfo detailWithInfo : entry.getValue() ) {
                ds.add( detailWithInfo.getDetail() );
            }
            ads.put(entry.getKey(), ds);
        }

        return Collections.unmodifiableMap(ads);
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
        throw new UnsupportedOperationException("Unsupported operation for Concurent All Assertion detail collector");
    }
}
