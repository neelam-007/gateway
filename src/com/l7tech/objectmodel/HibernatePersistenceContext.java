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

import javax.sql.DataSource;
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
    public static final int MAXRETRIES = 5;
    public static final int RETRYTIME = 250;

    public HibernatePersistenceContext( Session session ) {
        _session = session;
        _manager = (HibernatePersistenceManager)PersistenceManager.getInstance();
        if ( Debug.isEnabled() ) {
            logger.fine("Creating new HibernatePersistenceContext");
            try {
                throw new Exception("HibernatePersistenceContext was created here");
            } catch ( Exception e ) {
                this.createdAt = e;
            }
        }
    }

    public void commitTransaction() throws TransactionException {
        try {
            if ( _session == null )
                throw new IllegalStateException( "Can't commit when there's no session!" );

            if ( _htxn == null ) {
                logger.warning( "Commit called with no transaction active!" );
            } else {
                _htxn.commit();
            }
            for (Iterator i = txListenerList.iterator(); i.hasNext();) {
                TransactionListener toto = (TransactionListener)i.next();
                toto.postCommit();
            }
            txListenerList.clear();
            _session.flush();
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
                _htxn = null;
            } catch ( Exception e ) {
                throw new TransactionException( e.toString(), e );
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            if ( Debug.isEnabled() && ( _htxn != null || _session != null ) ) {
                logger.log(Level.SEVERE, "HibernatePersistenceContext finalized before being closed!", createdAt);
            }
            close();
        }
    }

    public void flush() throws ObjectModelException {
        try {
            if ( _htxn != null ) {
                logger.info( "Flush called with active transaction. Committing." );
                _htxn.commit();
                _htxn = null;
            }

            if ( _session == null )
                logger.warning( "Flush called with no session active!" );
            else
                _session.flush();
        } catch ( HibernateException he ) {
            logger.log( Level.SEVERE, "in flush()", he );
            throw new ObjectModelException( he.getMessage(), he );
        }
    }

    public void close() {
        try {
            if ( _htxn != null ) {
                logger.warning( "Close called with active transaction. Rolling back." );
                _htxn.rollback();
                _htxn = null;
            }
            if (_session != null && _session.isOpen()) {
                _session.clear();
                _session.close();
            }
            _session = null;
        } catch (HibernateException e) {
            logger.log(Level.FINE, "error closing context", e);
        } finally {
            PersistenceContext.releaseContext();
        }
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
                if ( _session == null || !_session.isOpen() || !_session.isConnected() ) {
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
                    if (_session != null) _session.close();
                    _session = _manager.makeSession();
                }

                // test the connection and return the session
                conn = _session.connection();
                pingStmt = conn.createStatement();
                rs = pingStmt.executeQuery( HibernatePersistenceManager.pingStatement );

                return _session;
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
            try {
                logger.fine("session is broken, trying to reconnect jdbc connection.");

                Connection theConnection = _session.connection();
                if ( !theConnection.isClosed() ) {
                    if (theConnection instanceof PoolableConnection) {
                        logger.fine("Closing PoolableConnection");
                        ((PoolableConnection)theConnection).reallyClose();
                    } else if (theConnection instanceof DelegatingConnection ) {
                        logger.fine("Calling DelegatingConnection");
                        final Connection delegate = ((DelegatingConnection)theConnection).getInnermostDelegate();
                        try {
                            if (delegate != null && !delegate.isClosed()) delegate.close();
                            if (!theConnection.isClosed()) theConnection.close();
                        } catch (Exception e) {
                            logger.log(Level.FINE, "Caught exception closing DelegatingConnection", e);
                        }
                    } else {
                        logger.fine("can't call REALLY close, type not handled: " + theConnection.getClass().getName());
                        theConnection.close();
                    }
                }
            } catch ( HibernateException he ) {
                logger.log( Level.WARNING, "exception closing session", he );
                lastException = he;
            }
            if (_session.isOpen()) _session.close();
            _session = null;
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
            _htxn = getSession().beginTransaction();
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
            if ( _htxn != null ) _htxn.rollback();
            for (Iterator i = txListenerList.iterator(); i.hasNext();) {
                TransactionListener toto = (TransactionListener)i.next();
                toto.postRollback();
            }
            txListenerList.clear();
        } catch ( HibernateException he ) {
            logger.throwing( getClass().getName(), "rollbackTransaction", he );
            throw new TransactionException( he.toString(), he );
        } finally {
            _htxn = null;
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


    protected HibernatePersistenceManager _manager;
    protected Session _session;
    protected DataSource _dataSource;
    protected Connection _conn;
    protected Transaction _htxn;
    protected ArrayList txListenerList = new ArrayList();

    private final Logger logger = Logger.getLogger(getClass().getName());
    private Exception createdAt;
}
