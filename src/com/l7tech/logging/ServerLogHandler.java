package com.l7tech.logging;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.RequestId;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.MessageProcessor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


/**
 * A logging handler that records SSGLogRecord objects and stored them in a database table.
 *
 * Initialization of this handler requires the node id which is retrieved through the ClusterInfoManager
 * and in turn requires the availability of the persistence context. Because the persistence context
 * makes use the log manager, initialization of this handler within the log manager potentially causes
 * a race condition and must be handled in a special way.
 *
 * The way this is achieved in the log manager involves initializing this handler in a seperate thread,
 * catch the IllegalStateException and retry until it works.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 13, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerLogHandler extends Handler {

    /**
     * note the two phase construction ServerLogHandler.initialize()
     */
    public ServerLogHandler() {
        super();
    }

    public void publish(LogRecord record) {
        Request req = MessageProcessor.getCurrentRequest();
        RequestId reqid = null;
        if (req != null) {
            reqid = req.getId();
        }
        SSGLogRecord newRecord = new SSGLogRecord(record, reqid, nodeid);
        add(newRecord);
    }

    public void flush() {
    }

    private void flushtodb(Session session) {
        Object[] data = null;
        synchronized (cache) {
            if (cache.isEmpty()) return;
            data = cache.toArray();
            cache.clear();
        }
        // save extracted data in the database
        try {
            for (int i = 0; i < data.length; i++)
                session.save(data[i]);
        } catch (HibernateException e) {
            reportException("error saving to database", e);
        }

    }

    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     * NOTE: the log messages whose message number equals to startMsgNumber and endMsgNumber
     * are not returned.
     *
     * @param highMsgNumber the message number to locate the start point.
     *                       Start from beginning of the message buffer if it equals to -1.
     * @param lowMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the message buffer is hit
     *                       if it equals to -1.
     * @param nodeId         the node id for which to retrieve server logs on. if left null, retreives
     *                       log records for this node.
     * @param size  the max. number of messages retrieved
     * @return LogRecord[] the array of log records retrieved
     */
    public Collection getLogRecords(String nodeId, long highMsgNumber, long lowMsgNumber, int size) {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
        } catch (SQLException e) {
            reportException("cannot get persistence context", e);
            return Collections.EMPTY_LIST;
        }
        String reqnode = nodeId;
        if (reqnode == null) reqnode = nodeid;

        // build the "where" part of the request
        String whereStatement = "where " + TABLE_NAME + "." + NODEID_COLNAME + " = \'" +
                                        reqnode + "\'";
        if (lowMsgNumber < 0 && highMsgNumber >= 0) {
            whereStatement += " and " + TABLE_NAME + "." + OID_COLNAME + " < " + highMsgNumber;
        } else if (lowMsgNumber >= 0 && highMsgNumber < 0) {
            whereStatement += " and " + TABLE_NAME + "." + OID_COLNAME + " > " + lowMsgNumber;
        } else if (lowMsgNumber >= 0 && highMsgNumber >= 0) {
            whereStatement += " and " + TABLE_NAME + "." + OID_COLNAME + " > " + lowMsgNumber +
                              " and " + TABLE_NAME + "." + OID_COLNAME + " < " + highMsgNumber;
        }

        // first thing, see how many records in the table
        // "select count(*) from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName()
        int counted = -1;
        String countselstatement = "select count(*) from " + TABLE_NAME + " in class " +
                                   SSGLogRecord.class.getName() + " " + whereStatement;
        Iterator i = null;
        try {
            i = context.getSession().iterate(countselstatement);
        } catch (HibernateException e) {
            reportException("Could not count from table", e);
            return Collections.EMPTY_LIST;
        } catch (SQLException e) {
            reportException("Could not count from table", e);
            return Collections.EMPTY_LIST;
        }
        if (i == null || !i.hasNext()) {
            reportException("count did not work - no result given", null);
            return Collections.EMPTY_LIST;
        } else {
            Integer toto = (Integer)i.next();
            counted = toto.intValue();
        }

        // obviously, if count was 0, no need to go further
        if (counted == 0) return Collections.EMPTY_LIST;

        // get the records using limit and offset statements
        String selStatement = "select from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                              " " + whereStatement;
        if (counted > size) {
            //int offsetvalue = counted - size;
            //selStatement += " limit " + size + "," + offsetvalue;
            //selStatement += " limit " + size + " offset " + offsetvalue;
            selStatement += " order by " + TABLE_NAME + "." + OID_COLNAME + " desc limit " + size;
        }

        List res = null;
        try {
            res = context.getSession().find(selStatement);
        } catch (HibernateException e) {
            reportException("Exception getting records with " + selStatement, e);
        } catch (SQLException e) {
            reportException("Exception getting records with " + selStatement, e);
        }
        if (res == null) return Collections.EMPTY_LIST;
        return res;
    }

    public void close() throws SecurityException {
        flusherDeamon.cancel();
    }

    /**
     * record a log record in tmp cache. it will eventually be flushed by a deamon thread
     */
    protected void add(SSGLogRecord arg) {
        synchronized (cache) {
            if (cache.size() >= MAX_CACHE_SIZE) {
                // todo, maybe we should force a flush?
                cache.remove(0);
            }
            cache.add(arg);
        }
    }

    /**
     * during initialization, this class requires the use of ClusterInfoManager
     * in turn, that class uses the LogManager.getInstance(). therefore, this manager
     * should not be initialized at part of LogManager.getInstance().
     */
    public synchronized void initialize() throws IllegalStateException {
        nodeid = ClusterInfoManager.getInstance().thisNodeId();
        // start the deamon
        if (flusherTask == null) {
            flusherTask = new TimerTask() {
                public void run() {
                    cleanAndFlush();
                }
            };
            flusherDeamon.schedule(flusherTask, FLUSH_FREQUENCY, FLUSH_FREQUENCY);
        }
    }

    /**
     * this clears all recorded log records for a given node. it is called by
     * ClusterStatusAdmin.removeStaleNode
     */
    public static void cleanAllRecordsForNode(HibernatePersistenceContext context, String nodeid)
                                                throws DeleteException {
        String deleteSQLStatement = "from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                    " where " + TABLE_NAME + "." + NODEID_COLNAME +
                                    " = \'" + nodeid + "\'";
        try {
            context.getSession().delete(deleteSQLStatement);
        } catch (HibernateException e) {
            throw new DeleteException("exception deleting logs for node " + nodeid, e);
        } catch (SQLException e) {
            throw new DeleteException("exception deleting logs for node " + nodeid, e);
        }
    }

    /**
     * performs the regular maintenance task including cleaning the log table if necessary
     * and flushing new cached log entries to database.
     * this method is responsible to manager its own persistence context.
     */
    protected void cleanAndFlush() {
        // get the persistence context
        HibernatePersistenceContext context = null;
        Session session = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            session = context.getSession();
        } catch (SQLException e) {
            reportException("cannot get persistence context", e);
            if ( context != null ) context.close();
            return;
        } catch (HibernateException e) {
            reportException("cannot get session", e);
            if ( context != null ) context.close();
            return;
        }

        try {
            context.beginTransaction();

            /*
            This commented out code used to delete previous records for this node before first dump
            we decided to make this the responsibility of an external cron job.
            if (fullClean) {
                String deleteSQLStatement = "from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                            " where " + TABLE_NAME + "." + NODEID_COLNAME +
                                            " = \'" + nodeid + "\'";
                session.iterate(deleteSQLStatement);
                session.flush();
            }*/
            // flush new records
            flushtodb(session);
            context.commitTransaction();
        }  catch(TransactionException e) {
            reportException("Exception with hibernate transaction", e);
        } finally {
            if ( context != null ) context.close();
        }
    }

    /**
     * the log handler does not use the normal logger because in case of an error,
     * that would cause some nasty loop.
     */
    protected static void reportException(String msg, Throwable e) {
        if (e == null) System.err.println(msg);
        else {
            System.err.println(msg + " " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * where log records are stored waiting to be flushed to the database
     */
    private ArrayList cache = new ArrayList();
    private static long MAX_CACHE_SIZE = 1000;
    private String nodeid;
    private static TimerTask flusherTask = null;
    private final Timer flusherDeamon = new Timer(true);

    private static final long FLUSH_FREQUENCY = 6000;
    //private static final long FLUSH_FREQUENCY = 10;
    //private static final long HOW_LONG_WE_KEEP_LOGS = (48*3600*1000);

    //private static final String TIMESTAMP_COLNAME = "millis";
    private static final String TABLE_NAME = "ssg_logs";
    private static final String NODEID_COLNAME = "nodeId";
    private static final String OID_COLNAME = "oid";
}
