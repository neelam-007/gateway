/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.publish;

import com.l7tech.service.Service;
import com.l7tech.policy.Policy;

/**
 * @author alex
 */
public class PublishedService {

    public PublishedService( Service service, Policy policy ) {
        _service = service;
        _policy = policy;
    }

    protected Service _service;
    protected Policy _policy;
}
