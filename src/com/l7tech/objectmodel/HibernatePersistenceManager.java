/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.server.ServerConfig;
import net.sf.hibernate.*;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.type.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class HibernatePersistenceManager extends PersistenceManager {
    public static final String DEFAULT_HIBERNATE_RESOURCEPATH = "SSG.hbm.xml";
    public static final String DEFAULT_PINGSQL = "select 1";
    public static String pingStatement = DEFAULT_PINGSQL;

    public static void initialize() throws IOException, SQLException {
        HibernatePersistenceManager me = new HibernatePersistenceManager();
        PersistenceManager.setInstance(me);
    }

    private HibernatePersistenceManager() throws IOException, SQLException {
        FileInputStream fis = null;
        try {
            Configuration cfg = new Configuration();
            cfg.addResource(DEFAULT_HIBERNATE_RESOURCEPATH, getClass().getClassLoader());

            String propsPath = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_HIBERNATE);

            if (new File(propsPath).exists()) {
                logger.info("Loading database configuration from " + propsPath);
                Properties props = new Properties();
                fis = new FileInputStream(propsPath);
                props.load(fis);
                cfg.setProperties(props);
            } else {
                logger.info("Loading database configuration from system classpath");
            }
            _sessionFactory = cfg.buildSessionFactory();

            String temp = cfg.getProperty("hibernate.dbcp.validationQuery");
            if ( temp != null && temp.length() > 0 ) pingStatement = temp;
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "<init>", he);
            throw new SQLException(he.toString());
        } finally {
            if (fis != null) fis.close();
        }
    }

    public PersistenceContext doMakeContext() throws SQLException {
        try {
            return new HibernatePersistenceContext(makeSession());
        } catch (HibernateException he) {
            throw new SQLException(he.toString());
        }
    }

    Session makeSession() throws HibernateException, SQLException {
        if (_sessionFactory == null) throw new IllegalStateException("HibernatePersistenceManager must be initialized before calling makeSession()!");
        Session session = _sessionFactory.openSession();
        //Connection conn = session.connection();
        //conn.setAutoCommit(false);
        return session;
    }

    private SessionFactory _sessionFactory;

    class ContextHolder {
        HibernatePersistenceContext _context;
        Session _session;
    }

    private ContextHolder getContextHolder(PersistenceContext context) throws SQLException, HibernateException {
        if (context == null || !(context instanceof HibernatePersistenceContext))
            throw new IllegalArgumentException("Invalid context passed to a HibernatePersistenceManager method!");
        ContextHolder holder = new ContextHolder();
        holder._context = (HibernatePersistenceContext)context;
        holder._session = holder._context.getSession();

        return holder;
    }

    List doFind(PersistenceContext context, String query) throws FindException {
        try {
            ContextHolder h = getContextHolder(context);
            Session s = h._session;
            return s.find(query);
        } catch (net.sf.hibernate.ObjectNotFoundException onfe) {
            return EMPTYLIST;
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "doFind", he);
            throw new FindException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doFind", se);
            close(context);
            throw new FindException(se.toString(), se);
        }
    }

    private Type getHibernateType(Object param) throws FindException {
        Type type;
        if (param instanceof String)
            type = Hibernate.STRING;
        else if (param instanceof Long)
            type = Hibernate.LONG;
        else if (param instanceof Boolean)
            type = Hibernate.BOOLEAN;
        else
            throw new FindException("I don't know how to find with parameters that aren't either String or Long!");
        return type;
    }

    public void close(PersistenceContext context) {
        try {
            context.close();
        } catch (Exception e) {
            logger.throwing(getClass().getName(), "Session.close()", e);
        }
    }

    List doFind(PersistenceContext context, String query, Object param, Class paramClass) throws FindException {
        try {
            ContextHolder h = getContextHolder(context);
            Session s = h._session;
            return s.find(query, param, getHibernateType(param));
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "doFind", he);
            throw new FindException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doFind", se);
            close(context);
            throw new FindException(se.toString(), se);
        }
    }

    List doFind(PersistenceContext context, String query, Object[] params, Class[] paramClasses) throws FindException {
        try {
            ContextHolder h = getContextHolder(context);
            Session s = h._session;
            Type[] types = new Type[paramClasses.length];
            for (int i = 0; i < paramClasses.length; i++)
                types[i] = getHibernateType(params[i]);
            return s.find(query, params, types);
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "doFind", he);
            throw new FindException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doFind", se);
            close(context);
            throw new FindException(se.toString(), se);
        }
    }

    Entity doFindByPrimaryKey(PersistenceContext context, Class clazz, long oid) throws FindException {
        return doFindByPrimaryKey(context, clazz, oid, false);
    }

    Entity doFindByPrimaryKey(PersistenceContext context, Class clazz, long oid, boolean forUpdate) throws FindException {
        try {
            ContextHolder h = getContextHolder(context);
            Session s = h._session;
            Object o = s.load(clazz, new Long(oid), forUpdate ? LockMode.WRITE : LockMode.READ);
            return (Entity)o;
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doFindByPrimaryKey", se);
            close(context);
            throw new FindException(se.toString(), se);
        } catch (net.sf.hibernate.ObjectNotFoundException e) {
            logger.log(Level.FINE, "object not found, returning null", e);
            return null;
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "doFindByPrimaryKey", he);
            throw new FindException(he.toString(), he);
        }
    }

    void doUpdate(PersistenceContext context, Entity obj) throws UpdateException {
        try {
            ContextHolder h = getContextHolder(context);
            Session s = h._session;

            s.update(obj);
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "doUpdate", he);
            throw new UpdateException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doUpdate", se);
            close(context);
            throw new UpdateException(se.toString(), se);
        }
    }

    long doSave(PersistenceContext context, Entity obj) throws SaveException {
        try {
            ContextHolder h = getContextHolder(context);
            Session s = h._session;

            Object key = s.save(obj);
            if (key instanceof Long) {
                return ((Long)key).longValue();
            } else {
                throw new RuntimeException("Generated primary key is not a long!");
            }
        } catch (HibernateException he) {
            logger.throwing(getClass().getName(), "doSave", he);
            throw new SaveException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doSave", se);
            close(context);
            throw new SaveException(se.toString(), se);
        }
    }

    void doDelete(PersistenceContext context, Entity obj) throws DeleteException {
        try {
            ContextHolder h = getContextHolder(context);
            h._session.delete(obj);
        } catch (HibernateException he) {
            throw new DeleteException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doDelete", se);
            close(context);
            throw new DeleteException(se.toString(), se);
        }
    }

    void doDelete( PersistenceContext context, Class entityClass, long oid ) throws DeleteException {
        try {
            ContextHolder h = getContextHolder(context);
            String deleteQuery = "from temp in class " +
                                 entityClass.getName() +
                                 " where temp.oid = ?";
            h._session.delete( deleteQuery, new Long( oid ), Hibernate.LONG );
        } catch (HibernateException he) {
            throw new DeleteException(he.toString(), he);
        } catch (SQLException se) {
            logger.throwing(getClass().getName(), "doDelete", se);
            close(context);
            throw new DeleteException(se.toString(), se);
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
