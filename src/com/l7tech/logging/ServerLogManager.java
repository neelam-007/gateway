package com.l7tech.logging;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 * SSG log manager that sets the right handlers to the root logger and
 * prepares the hibernate dumper when ready. This must be instantiated when
 * the server boots and once hibernate is ready, a call to suscribeDBHandler
 * must be made.
 * <p/>
 * Reads properties from ssglog.properties file. Tries to get this file from
 * /ssg/etc/conf/ssglog.properties. If not present, gets is from
 * webapps/ROOT/WEB-INF/classes/ssglog.properties
 * Creates log rotation files, in a path provided in properties file. If this path
 * is invalid, use home dir instead.
 * <p/>
 * NOTE: Please avoid calling any external class that uses logging itself.
 * <p/>
 * NOTE: Unusual exception handling because of the fact that logging subsystem
 * is not initialized yet.
 * <p/>
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Jul 3, 2003<br/>
 * Time: 11:42:08 AM<br/><br/>
 */
public class ServerLogManager {

    public static ServerLogManager getInstance() {
        return SingletonHolder.singleton;
    }

    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     * NOTE: the log messages whose message number equals to startMsgNumber and endMsgNumber
     * are not returned.
     *
     * @param highMsgNumber the message number to locate the start point.
     *                      Start from beginning of the message buffer if it equals to -1.
     * @param lowMsgNumber  the message number to locate the end point.
     *                      Retrieve messages until the end of the message buffer is hit
     *                      if it equals to -1.
     * @param nodeId        the node id for which to retrieve server logs on. if left null, retreives
     *                      log records for this node.
     * @param size          the max. number of messages retrieved
     * @return LogRecord[] the array of log records retrieved
     */
    public Collection getLogRecords(String nodeId, long highMsgNumber, long lowMsgNumber, int size) {
        HibernatePersistenceContext context = null;
        boolean ok = false;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();
            ok = true;
        } catch (SQLException e) {
            reportException("cannot get persistence context", e);
            return Collections.EMPTY_LIST;
        } catch (TransactionException e) {
            reportException("cannot get persistence context", e);
            return Collections.EMPTY_LIST;
        } finally {
            if (context != null && !ok) context.close();
        }

        String reqnode = nodeId;
        if (reqnode == null) reqnode = nodeid;
        Collection res = null;
        try {
            if (lowMsgNumber < 0 && highMsgNumber >= 0) {
                res = getRecordsBeforeLowId(reqnode, size, context, highMsgNumber);
            } else if (lowMsgNumber >= 0 && highMsgNumber < 0) {
                res = getRecordsBeyondHighId(reqnode, size, context, lowMsgNumber);
            } else if (lowMsgNumber >= 0 && highMsgNumber >= 0) {
                res = getRecordsInRange(reqnode, size, context, lowMsgNumber, highMsgNumber);
            } else {
                res = getLastRecords(reqnode, size, context);
            }
        } catch (HibernateException e) {
            reportException("Error getting log records", e);
            return Collections.EMPTY_LIST;
        } catch (SQLException e) {
            reportException("Error getting log records", e);
            return Collections.EMPTY_LIST;
        } finally {
            try {
                if (context != null) context.commitTransaction();
            } catch (TransactionException e) {
                reportException("Error getting log records", e);
                return Collections.EMPTY_LIST;
            }
        }
        if (res == null) return Collections.EMPTY_LIST;
        return res;
    }


    // ************************************************
    // PRIVATES
    // ************************************************

    private ServerLogManager() {
        nodeid = ClusterInfoManager.getInstance().thisNodeId();
    }

    private Collection getRecordsInRange(String nodeId, int maxSize, HibernatePersistenceContext context,
                                         long startid, long endid) throws HibernateException, SQLException {
        //
        // NOTE
        // it's ok to use order by limit here because we have other criteria before the node id
        //
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.").append(NODEID_COLNAME).append(" = ? ");
        hql.append("AND log.").append(OID_COLNAME).append(" > ? ");
        hql.append("AND log.").append(OID_COLNAME).append(" < ? ");
        hql.append("ORDER BY log.").append(OID_COLNAME).append(" DESC");
        Query q = context.getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setLong(1, startid);
        q.setLong(2, endid);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }

    /**
     * Return the server id
     *
     * @return
     */
    public String getNodeid() {
        return nodeid;
    }

    /**
     * we dont have to worry about this query here
     */
    private Collection getRecordsBeyondHighId(String nodeId, int maxSize, HibernatePersistenceContext context,
                                              long startid) throws HibernateException, SQLException {
        //
        // NOTE
        // it's ok to use order by limit here because we have a "low" criteria before the node id
        //
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.").append(NODEID_COLNAME).append(" = ? ");
        hql.append("AND log.").append(OID_COLNAME).append(" > ? ");
        hql.append("ORDER BY log.").append(OID_COLNAME).append(" DESC");
        Query q = context.getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setLong(1, startid);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }

    private Collection getRecordsBeforeLowId(String nodeId, int maxSize, HibernatePersistenceContext context, long endid) throws HibernateException, SQLException {
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.").append(NODEID_COLNAME).append(" = ? ");
        hql.append("AND log.").append(OID_COLNAME).append(" < ? ");
        hql.append("ORDER BY log.").append(OID_COLNAME).append(" DESC");
        Query q = context.getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setLong(1, endid);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }


    private Collection getLastRecords(String nodeId, int maxSize, HibernatePersistenceContext context)
      throws HibernateException, SQLException {
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.");
        hql.append(NODEID_COLNAME);
        hql.append(" = ? ");
        hql.append(" ORDER BY log.");
        hql.append(OID_COLNAME);
        hql.append(" DESC");
        Query q = context.getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }

    /**
     * the log handler does not use the normal logger because in case of an error,
     * that would cause some nasty loop.
     */
    private void reportException(String msg, Throwable e) {
        if (e == null)
            System.err.println(msg);
        else {
            System.err.println(msg + " " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }


    private static class SingletonHolder {
        private static ServerLogManager singleton = new ServerLogManager();
    }

    private final String nodeid;

    private static final String NODEID_COLNAME = "nodeId";
    private static final String OID_COLNAME = "oid";

}
