/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.objectmodel.imp.EntityImp;
import com.l7tech.message.Request;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServiceResolutionParameter extends EntityImp {
    public ServiceResolutionParameter() {
        super();
    }

    public abstract boolean matches( Request request );
}
