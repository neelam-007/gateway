/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.sla;

import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.FindException;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author alex
 */
public interface CounterIDManager {
    /**
     * Check if a counter exists or not.  If it does not exist, then create a new counter with a given counter name.
     * @param counterName: used to check if the database has such counter whose name is counterName.
     * @throws ObjectModelException: thrown when data access errors occur.
     */
    void checkOrCreateCounter(String counterName) throws ObjectModelException;

    @Transactional(readOnly=true)
    String[] getAllCounterNames() throws FindException;
}