/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.server.event.admin.Updated;

/**
 * @author alex
 * @version $Revision$
 */
public class UserCertEvent extends Updated {
    public UserCertEvent(UserCertEventInfo cer) {
        super(cer.getUser(), EntityChangeSet.NONE, cer.getNote());
    }
}
