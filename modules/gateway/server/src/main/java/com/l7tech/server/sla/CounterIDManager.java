/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.sla;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.FindException;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author alex
 */
public interface CounterIDManager {
    long getCounterId(String counterName, User identity) throws ObjectModelException;

    @Transactional(readOnly=true)
    String[] getDistinctCounterNames() throws FindException;
}
