package com.l7tech.objectmodel;

/**
 * Used in the PersistenceContext to get notifications after successful commits and rollbacks.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Dec 8, 2003<br/>
 * $Id$
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
