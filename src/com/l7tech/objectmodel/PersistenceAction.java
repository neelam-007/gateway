/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * A basic {@link Runnable}-like interface for persistence tasks
 * @see HibernatePersistenceContext#doInTransaction(PersistenceAction)
 * @author alex
 * @version $Revision$
 */
public interface PersistenceAction {
    Object run() throws ObjectModelException;
}
