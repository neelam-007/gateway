/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.server.Debug;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.PoolableConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class HibernatePersistenceContext extends PersistenceContext {
    public static final int MAXRETRIES = 2;
    public static final int RETRYTIME = 250;

    public HibernatePersistenceContext(Session mainSession, Session auditSession) {
        this.mainSession = mainSession;
        this.auditSession = auditSession;

        manager = (HibernatePersistenceManager)PersistenceManager.getInstance();
        if ( Debug.isEnabled() ) {
            logger.fine("Creating new HibernatePersistenceContext");
            try {
                throw new Exception("HibernatePersistenceContext was created here");
            } catch ( Exception e ) {
                this.createdAt = e;
            }
        }
    }

    public void commitIfPresent() throws TransactionException {
        if (mainSession != null && mainTransaction != null) commitTransaction();
    }

    public void commitTransaction() throws TransactionException {
        try {
            if ( mainSession == null )
                throw new IllegalStateException( "Can't commit when there's no session!" );

            if ( mainTransaction == null ) {
                logger.warning( "Commit called with no transaction active!" );
            } else {
                mainTransaction.commit();
                auditTransaction.commit();
            }
            for (Iterator i = txListenerList.iterator(); i.hasNext();) {
                TransactionListener toto = (TransactionListener)i.next();
                toto.postCommit();
            }
            txListenerList.clear();
            mainSession.flush();
            auditSession.flush();
        } catch ( HibernateException he ) {
            logger.throwing( getClass().getName(), "commitTransaction", he );
            if(he.getCause() != null && he.getCause().getMessage() != null) {
                throw new TransactionException(he.getCause().getMessage(), he);
            } else {
                throw new TransactionException(he);
            }

        } finally {
            try {
                //if ( _session != null ) _session.close();
                //_session = null;
                mainTransaction = null;
                auditTransaction = null;
            } catch ( Exception e ) {
                throw new TransactionException( e.toString(), e );
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            if ( Debug.isEnabled() && (mainTransaction != null || mainSession != null || auditTransaction != null || auditSession != null) ) {
                logger.log(Level.SEVERE, "HibernatePersistenceContext finalized before being closed!", createdAt);
            }
            close();
        }
    }

    public void flush() throws ObjectModelException {
        try {
            if ( mainTransaction != null ) {
                logger.info( "Flush called with active transaction. Committing." );
                mainTransaction.commit();
                mainTransaction = null;

                auditTransaction.commit();
                auditTransaction = null;
            }

            if ( mainSession == null )
                logger.warning( "Flush called with no session active!" );
            else
                mainSession.flush();

            if ( auditSession == null )
                logger.warning( "Flush called with no auditSession active!" );
            else
                auditSession.flush();
        } catch ( HibernateException he ) {
            logger.log( Level.SEVERE, "in flush()", he );
            throw new ObjectModelException( he.getMessage(), he );
        }
    }

    public void close() {
        try {
            if ( mainTransaction != null ) {
                logger.warning( "Close called with active transaction. Rolling back." );
                mainTransaction.rollback();
                mainTransaction = null;
            }

            if ( auditTransaction != null ) {
                logger.warning( "Close called with active audit transaction. Rolling back." );
                auditTransaction.rollback();
                auditTransaction = null;
            }

            if (mainSession != null && mainSession.isOpen()) {
                mainSession.clear();
                mainSession.close();
            }
            mainSession = null;

            if (auditSession != null && auditSession.isOpen()) {
                auditSession.clear();
                auditSession.close();
            }
            auditSession = null;
        } catch (HibernateException e) {
            logger.log(Level.FINE, "error closing context", e);
        } finally {
            PersistenceContext.releaseContext();
        }
    }

    public Session getAuditSession() throws SQLException, HibernateException {
        if (auditSession == null || !auditSession.isOpen()) {
            auditSession = manager.makeAuditSession();
        }
        return auditSession;
    }

    /**
     * Retrieves the current {@see cirrus.hibernate.Session} associated with this context, getting a new one if necessary.
     * Also tries to "ping" the connection up to {@link MAXRETRIES} times to be sure it's not dead.
     *
     * @return a valid Hibernate Session (hopefully!)
     * @throws SQLException if MAXRETRIES have been made, the SQLException resulting from the last connection attempt.
     * @throws net.sf.hibernate.HibernateException if MAXRETRIES have been made, the HibernateException resulting from the last connection attempt.
     */
    public synchronized Session getSession() throws SQLException, HibernateException {
        return getSession(MAXRETRIES);
    }

    /**
     * Retrieves the current {@see cirrus.hibernate.Session} associated with this context, getting a new one if necessary.
     * Also tries to "ping" the connection up to maxretries times to be sure it's not dead.
     *
     * @param maxretries the maximum number of times to retry the connection. Pass <code>-1</code> to retry forever, but beware that this can block the current thread for a very long time.
     * @return a valid Hibernate Session (hopefully!)
     * @throws SQLException if MAXRETRIES have been made, the SQLException resulting from the last connection attempt.
     * @throws net.sf.hibernate.HibernateException if MAXRETRIES have been made, the HibernateException resulting from the last connection attempt.
     */
    public synchronized Session getSession(int maxretries) throws SQLException, HibernateException {
        Exception lastException = null;

        // Test underlying connection before returning the session
        // if the connection does not seem to be working, this will try to recover up to MAXRETRIES times.
        for ( int i = 0; (maxretries == -1 || i < maxretries); i++ ) {
            Connection conn = null;
            ResultSet rs = null;
            Statement pingStmt = null;
            try {
                // is the session created and ready ?
                if ( mainSession == null || !mainSession.isOpen() || !mainSession.isConnected() ) {
                    logger.info("Session broken - will try to make new one.");

                    // If the session could not be restored on first try. we might need to wait a little to
                    // allow for something like a database restart ot another node to take over.
                    if (i > 1) {
                        try {
                            wait(1500);
                        } catch (InterruptedException e) {
                            logger.fine("could not wait to make new session " + e.getMessage());
                        }
                    }

                    // Try to get new session
                    if (mainSession != null) mainSession.close();
                    mainSession = manager.makeSession();
                }

                // test the connection and return the session
                conn = mainSession.connection();
                pingStmt = conn.createStatement();
                rs = pingStmt.executeQuery( HibernatePersistenceManager.pingStatement );

                return mainSession;
            } catch (SQLException e) {
                String msg = "Try #" + (i+1) + " caught SQLException";
                logger.log(Level.WARNING, msg, e);
                lastException = e;
            } catch (HibernateException e) {
                String msg = "Try #" + (i+1) + " caught HibernateException";
                logger.log(Level.WARNING, msg, e);
                lastException = e;
            } finally {
                // clean stuff
                try {
                    if (pingStmt != null) pingStmt.close();
                } catch (SQLException se) {
                    logger.log(Level.WARNING, "SQLException closing pingStmt", se);
                }
                try {
                    if (rs != null) rs.close();
                } catch (SQLException se) {
                    logger.log(Level.WARNING, "SQLException closing rs", se);
                }
            }

            // if jdbc connection failure, close the session and null it
            logger.fine("session is broken, trying to reconnect jdbc connection.");

            if ( conn != null && !conn.isClosed() ) {
                if (conn instanceof PoolableConnection) {
                    logger.fine("Closing PoolableConnection");
                    ((PoolableConnection)conn).reallyClose();
                } else if (conn instanceof DelegatingConnection ) {
                    logger.fine("Calling DelegatingConnection");
                    final Connection delegate = ((DelegatingConnection)conn).getInnermostDelegate();
                    try {
                        if (delegate != null && !delegate.isClosed()) delegate.close();
                        if (!conn.isClosed()) conn.close();
                    } catch (Exception e) {
                        logger.log(Level.FINE, "Caught exception closing DelegatingConnection", e);
                    }
                } else {
                    logger.fine("can't call REALLY close, type not handled: " + conn.getClass().getName());
                    conn.close();
                }
            }

            if (mainSession.isOpen()) mainSession.close();

            mainSession = null;
        }

        String err = "Tried " + maxretries + " times to obtain a valid Session and failed with exception " + lastException;
        logger.log( Level.SEVERE, err, lastException );

        if (lastException instanceof SQLException) {
            throw (SQLException)lastException;
        }
        if (lastException instanceof HibernateException) {
            throw (HibernateException)lastException;
        }

        throw new RuntimeException("Database connection failed for an unexpected reason", lastException);
    }

    public void beginTransaction() throws TransactionException {
        try {
            mainTransaction = getSession().beginTransaction();
            auditTransaction = getAuditSession().beginTransaction();
        } catch ( SQLException se ) {
            logger.throwing( getClass().getName(), "beginTransaction", se );
            close();
            throw new TransactionException( se.toString(), se );
        } catch ( HibernateException he ) {
            logger.throwing( getClass().getName(), "beginTransaction", he );
            throw new TransactionException( he.toString(), he );
        }
    }

    public void rollbackTransaction() throws TransactionException {
        try {
            if ( mainTransaction != null ) mainTransaction.rollback();
            if ( auditTransaction != null ) auditTransaction.rollback();
            for (Iterator i = txListenerList.iterator(); i.hasNext();) {
                TransactionListener toto = (TransactionListener)i.next();
                toto.postRollback();
            }
            txListenerList.clear();
        } catch ( HibernateException he ) {
            logger.throwing( getClass().getName(), "rollbackTransaction", he );
            throw new TransactionException( he.toString(), he );
        } finally {
            mainTransaction = null;
            auditTransaction = null;
        }
    }

    public void registerTransactionListener(TransactionListener listener)
                                              throws TransactionException {
        txListenerList.add(listener);
    }

    public Object doInTransaction(PersistenceAction r) throws ObjectModelException {
        beginTransaction();
        if (r instanceof TransactionListener) {
            registerTransactionListener((TransactionListener)r);
        }
        boolean ok = false;
        Object result;
        try {
            result = r.run();
            ok = true;
        } finally {
            if ( ok )
                commitTransaction();
            else
                rollbackTransaction();
        }
        return result;
    }


    protected HibernatePersistenceManager manager;
    protected Session mainSession;
    protected Transaction mainTransaction;

    protected Session auditSession;
    protected Transaction auditTransaction;

    protected ArrayList txListenerList = new ArrayList();

    private final Logger logger = Logger.getLogger(getClass().getName());
    private Exception createdAt;
}
