/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel.event;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public interface UpdateListener extends EventListener {
    void entityUpdated(Updated updated);
}
