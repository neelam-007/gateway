/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;
import cirrus.hibernate.HibernateException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Iterator;

import com.l7tech.logging.LogManager;

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
        } catch ( SQLException se ) {
            logger.throwing( getClass().getName(), "commitTransaction", se );
            close();
            throw new TransactionException( se.toString(), se );
        } catch ( HibernateException he ) {
            logger.throwing( getClass().getName(), "commitTransaction", he );
            throw new TransactionException( he.toString(), he );
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
        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, "in flush()", se );
            close();
            throw new ObjectModelException( se.getMessage(), se );
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
            if ( _session != null ) _session.close();
            _session = null;
            super.releaseContext();
        } catch ( HibernateException he ) {
            logger.log(Level.SEVERE, null, he);
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
        }
    }

    /**
     * Retrieves the current {@see cirrus.hibernate.Session} associated with this context, getting a new one if necessary.
     * Also tries to "ping" the connection up to {@see MAXRETRIES} times to be sure it's not dead.
     *
     * @return a valid Hibernate Session (hopefully!)
     * @throws SQLException if MAXRETRIES have been made, the SQLException resulting from the last connection attempt.
     * @throws HibernateException if MAXRETRIES have been made, the HibernateException resulting from the last connection attempt.
     */
    public synchronized Session getSession() throws SQLException, HibernateException {
        Connection conn = null;
        ResultSet rs = null;
        Statement pingStmt = null;
        SQLException sqlException = null;
        HibernateException hibernateException = null;

        try {
            for ( int i = 0; i < MAXRETRIES; i++ ) {
                try {
                    if ( _session == null || !_session.isOpen() || !_session.isConnected() )
                        _session = _manager.makeSession();
                    conn = _session.connection();
                    pingStmt = conn.createStatement();
                    rs = pingStmt.executeQuery( PINGSQL );
                    return _session;
                } catch ( SQLException se ) {
                    logger.log( Level.WARNING, "Try #" + (i+1) + " caught SQLException", se );
                    _session = null;
                    sqlException = se;
                } catch ( HibernateException he ) {
                    logger.log( Level.WARNING, "Try #" + (i+1) + " caught HibernateException", he );
                    _session = null;
                    hibernateException = he;
                }
            }
        } finally {
            try {
                if ( pingStmt != null ) pingStmt.close();
            } catch ( SQLException se ) {
                logger.log( Level.WARNING, "SQLException closing pingStmt", se );
            }

            try {
                if ( rs != null ) rs.close();
            } catch ( SQLException se ) {
                logger.log( Level.WARNING, "SQLException closing rs", se );
            }
        }

        Exception e;
        if ( sqlException == null )
            e = hibernateException;
        else
            e = sqlException;

        String err = "Tried " + MAXRETRIES + " times to obtain a valid Session and failed with exception " + e;
        logger.log( Level.SEVERE, err, e );

        if ( sqlException != null )
            throw sqlException;
        else if ( hibernateException != null )
            throw hibernateException;
        else {
            err = "Some other failure has occurred!";
            logger.log( Level.SEVERE, err, e );
            throw new RuntimeException( err );
        }
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
    protected cirrus.hibernate.Transaction _htxn;
    protected ArrayList txListenerList = new ArrayList();
    private Logger logger = LogManager.getInstance().getSystemLogger();
}
