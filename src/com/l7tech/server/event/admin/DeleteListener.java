/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public interface DeleteListener extends EventListener {
    void entityDeleted(Deleted deleted);
}
