/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.cluster;

import com.l7tech.objectmodel.UpdateException;

/**
 * The status update interface
 * @author emil
 * @version Jan 3, 2005
 */
public interface StatusUpdateManager {
    /**
     * The update method, that performs all the necessary updates
     * @throws UpdateException
     */
    void update() throws UpdateException;
}