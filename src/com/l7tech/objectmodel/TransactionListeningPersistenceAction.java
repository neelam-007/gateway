/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * Convenience interface for anonymous transactional persistence actions
 * @see HibernatePersistenceContext#doInTransaction(PersistenceAction)
 * @author alex
 * @version $Revision$
 */
public interface TransactionListeningPersistenceAction extends PersistenceAction, TransactionListener {
}
