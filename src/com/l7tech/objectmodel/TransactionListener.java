package com.l7tech.objectmodel;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Dec 8, 2003
 * Time: 11:46:16 AM
 * $Id$
 *
 * Used in the PersistenceContext to get notifications after successful commits and rollbacks.
 *
 */
public interface TransactionListener {
    /**
     * PersistenceContext will call this back once the transaction has successfully comitted
     */
    void postCommit();

    /**
     * PersistenceContext will call this back once the transaction has successfully rolledback
     */
    void postRollback();
}
