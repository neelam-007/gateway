/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.util.EventListener;


/**
 * Superclass extended by those who wish to be notified of changes to an SSG.
 *
 * User: mike
 * Date: Sep 3, 2003
 * Time: 9:57:42 AM
 */
public interface SsgListener extends EventListener {

    /**
     * This event is fired when a policy is attached to an Ssg with a PolicyAttachmentKey, either new
     * or updated.
     *
     * @param evt
     */
    void policyAttached(SsgEvent evt);
}
