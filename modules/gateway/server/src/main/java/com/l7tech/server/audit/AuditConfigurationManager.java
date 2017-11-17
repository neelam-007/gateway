/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

/**
 */
public interface AuditConfigurationManager extends EntityManager<AuditConfiguration, EntityHeader> {

    AuditConfiguration findByPrimaryKey(Goid goid) throws FindException;
}

