/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import javax.sql.DataSource;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Iterator;

import com.l7tech.logging.LogManager;
import net.sf.hibernate.Session;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Transaction;
import org.apache.commons.dbcp.PoolableConnection;

/**
 * @author alex
 */
public class HibernatePersistenceContext extends PersistenceContext {
    public static final int MAXRETRIES = 5;
    public static final int RETRYTIME = 250;
    // TODO: This statement doesn't work on all databases!
    public static final String PINGSQL = "select 1";

    public HibernatePersistenceContext( Session session ) {
        _session = session;
        _manager = (HibernatePersistenceManager)PersistenceManager.getInstance();
    }

    public void commitTransaction() throws TransactionException {
        try {
            if ( _session == null )
                throw new IllegalStateException( "Can't commit when there's no session!" );
            else
                _session.flush();

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
        } catch ( HibernateException he ) {
            logger.throwing( getClass().getName(), "commitTransaction", he );
            throw new TransactionException(he);
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
        close();
        super.finalize();
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
            PersistenceContext.releaseContext();
        } catch (HibernateException e) {
            logger.log(Level.FINE, "error closing context", e);
        }
    }

    /**
     * Retrieves the current {@see cirrus.hibernate.Session} associated with this context, getting a new one if necessary.
     * Also tries to "ping" the connection up to {@see MAXRETRIES} times to be sure it's not dead.
     *
     * @return a valid Hibernate Session (hopefully!)
     * @throws SQLException if MAXRETRIES have been made, the SQLException resulting from the last connection attempt.
     * @throws net.sf.hibernate.HibernateException if MAXRETRIES have been made, the HibernateException resulting from the last connection attempt.
     */
    public synchronized Session getSession() throws SQLException, HibernateException {
        Exception lastException = null;

        // Test underlying connection before returning the session
        // if the connection does not seem to be working, this will try to recover up to MAXRETRIES times.
        for ( int i = 0; i < MAXRETRIES; i++ ) {
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
                rs = pingStmt.executeQuery( PINGSQL );

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
                        logger.fine("calling REALLY close");
                        ((PoolableConnection)theConnection).reallyClose();
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

        String err = "Tried " + MAXRETRIES + " times to obtain a valid Session and failed with exception " + lastException;
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

    protected HibernatePersistenceManager _manager;
    protected Session _session;
    protected DataSource _dataSource;
    protected Connection _conn;
    protected Transaction _htxn;
    protected ArrayList txListenerList = new ArrayList();

    private Logger logger = LogManager.getInstance().getSystemLogger();
}
