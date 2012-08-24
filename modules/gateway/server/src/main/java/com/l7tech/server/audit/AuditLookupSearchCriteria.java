package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditSearchCriteria;

/**
 * For holding the search parameters for the audit lookup policy
*/
public class AuditLookupSearchCriteria {

    private AuditSearchCriteria criteria;
    private String[] guids;
    private Long maxMessageSize;

    public AuditLookupSearchCriteria(AuditSearchCriteria criteria) {
        this.criteria = criteria;
    }

    public AuditLookupSearchCriteria(String[] guids, Long maxMessageSize) {
        this.guids = guids;
        this.maxMessageSize = maxMessageSize;
    }

    public AuditSearchCriteria getCriteria() {
        return criteria;
    }

    public String[] getGuids() {
        return guids;
    }

    public Long getMaxMessageSize() {
        return maxMessageSize;
    }
}
