package com.l7tech.logging;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.objectmodel.SaveException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.Collection;
import java.util.Collections;

/**
 * SSG log manager is a central log component for retrieving ands saving the logs.
 * <p/>
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Jul 3, 2003<br/>
 * Time: 11:42:08 AM<br/><br/>
 */
public class ServerLogManager extends HibernateDaoSupport {
    private ClusterInfoManager clusterInfoManager;
    private String thisNodeId;


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

        String reqnode = nodeId;
        if (reqnode == null) reqnode = getNodeid();
        Collection res = null;
        try {
            if (lowMsgNumber < 0 && highMsgNumber >= 0) {
                res = getRecordsBeforeLowId(reqnode, size, highMsgNumber);
            } else if (lowMsgNumber >= 0 && highMsgNumber < 0) {
                res = getRecordsBeyondHighId(reqnode, size, lowMsgNumber);
            } else if (lowMsgNumber >= 0 && highMsgNumber >= 0) {
                res = getRecordsInRange(reqnode, size, lowMsgNumber, highMsgNumber);
            } else {
                res = getLastRecords(reqnode, size);
            }
        } catch (HibernateException e) {
            reportException("Error getting log records", e);
            return Collections.EMPTY_LIST;
        }
        if (res == null) return Collections.EMPTY_LIST;
        return res;
    }

    /**
     * Save the log records
     *
     * @param data the array of log records
     */
    public void save(SSGLogRecord[] data) throws SaveException {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        Session session = getSession();
        try {
            for (int i = data.length - 1; i >= 0; i--) {
                SSGLogRecord ssgLogRecord = data[i];
                session.save(ssgLogRecord);
            }
        } catch (HibernateException e) {
            throw new SaveException("Error saving log records", e);
        }
    }

    /**
     * Return the server id
     *
     * @return
     */
    public String getNodeid() {
        if (thisNodeId == null) {
            thisNodeId = clusterInfoManager.thisNodeId();
        }
        return thisNodeId;
    }

    public void setClusterInfoManager(ClusterInfoManager clusterInfoManager) {
        this.clusterInfoManager = clusterInfoManager;
    }

    protected void initiDao() throws Exception {
        if (clusterInfoManager == null) {
            throw new IllegalArgumentException("Cluster Info Manager is required");
        }
    }

    private Collection getRecordsInRange(String nodeId, int maxSize, long startid, long endid) throws HibernateException {
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
        Query q = getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setLong(1, startid);
        q.setLong(2, endid);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }

    /**
     * we dont have to worry about this query here
     */
    private Collection getRecordsBeyondHighId(String nodeId, int maxSize, long startid) throws HibernateException {
        //
        // NOTE
        // it's ok to use order by limit here because we have a "low" criteria before the node id
        //
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.").append(NODEID_COLNAME).append(" = ? ");
        hql.append("AND log.").append(OID_COLNAME).append(" > ? ");
        hql.append("ORDER BY log.").append(OID_COLNAME).append(" DESC");
        Query q = getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setLong(1, startid);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }

    private Collection getRecordsBeforeLowId(String nodeId, int maxSize, long endid) throws HibernateException {
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.").append(NODEID_COLNAME).append(" = ? ");
        hql.append("AND log.").append(OID_COLNAME).append(" < ? ");
        hql.append("ORDER BY log.").append(OID_COLNAME).append(" DESC");
        Query q = getSession().createQuery(hql.toString());
        q.setString(0, nodeId);
        q.setLong(1, endid);
        q.setMaxResults(maxSize);
        Collection found = q.list();
        return found;
    }


    private Collection getLastRecords(String nodeId, int maxSize)
      throws HibernateException {
        StringBuffer hql = new StringBuffer("FROM log IN CLASS ");
        hql.append(SSGLogRecord.class.getName());
        hql.append(" WHERE log.");
        hql.append(NODEID_COLNAME);
        hql.append(" = ? ");
        hql.append(" ORDER BY log.");
        hql.append(OID_COLNAME);
        hql.append(" DESC");
        Query q = getSession().createQuery(hql.toString());
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

    private static final String NODEID_COLNAME = "nodeId";
    private static final String OID_COLNAME = "oid";

}
