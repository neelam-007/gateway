package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited for admin events.
 * The ID range 1500 - 1999 inclusive is reserved for these messages.
 * 
 * @author ghuang
 */
public class AdminMessages extends Messages {
    // Full details on audit search criteria
    public static final M AUDIT_SEARCH_CRITERIA_FULL_DETAILS = m(1500, Level.INFO, "Full Details of Audit Search Criteria: {0}");
    
    // MAX -                                                   m(1999
}
