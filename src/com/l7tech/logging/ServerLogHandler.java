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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
        // add order by something and limit
        String selStatement = "from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                        " where " + TABLE_NAME + "." + NODEID_COLNAME + " = \'" +
                                        reqnode + "\'";
        if (lowMsgNumber < 0 && highMsgNumber >= 0) {
            selStatement += " and " + TABLE_NAME + "." + SEQ_COLNAME + " < " + highMsgNumber;
        } else if (lowMsgNumber >= 0 && highMsgNumber < 0) {
            selStatement += " and " + TABLE_NAME + "." + SEQ_COLNAME + " > " + lowMsgNumber;
        } else if (lowMsgNumber >= 0 && highMsgNumber >= 0) {
            selStatement += " and " + TABLE_NAME + "." + SEQ_COLNAME + " > " + lowMsgNumber +
                            " and " + TABLE_NAME + "." + SEQ_COLNAME + " < " + highMsgNumber;
        }
        selStatement += " order by " + TABLE_NAME + "." + SEQ_COLNAME + " desc limit " + size;

        List res = null;
        try {
            res = context.getSession().find(selStatement);
        } catch (HibernateException e) {
            reportException("Exception getting records", e);
        } catch (SQLException e) {
            reportException("Exception getting records", e);
        }
        if (res == null) return Collections.EMPTY_LIST;
        return res;
    }

    public void close() throws SecurityException {
        flusherDeamon.interrupt();
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
    public void initialize() throws IllegalStateException {
        nodeid = ClusterInfoManager.getInstance().thisNodeId();
        // start the deamon
        synchronized (flusherDeamon) {
            if (flusherDeamon.parent == null) {
                flusherDeamon.parent = this;
                flusherDeamon.setDaemon(true);
                flusherDeamon.start();
            }
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
    protected void cleanAndFlush(boolean fullClean) {
        // get the persistence context
        HibernatePersistenceContext context = null;
        Session session = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            session = context.getSession();
        } catch (SQLException e) {
            reportException("cannot get persistence context", e);
            return;
        } catch (HibernateException e) {
            reportException("cannot get session", e);
            return;
        } finally {
            if ( context != null ) context.close();
        }

        try {
            context.beginTransaction();

            if (fullClean) {
                String deleteSQLStatement = "from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                            " where " + TABLE_NAME + "." + NODEID_COLNAME +
                                            " = \'" + nodeid + "\'";
                session.delete(deleteSQLStatement);
                session.flush();
            } /*else {
                // delete records older than 48 hrs
                long deadline = System.currentTimeMillis();
                deadline -= HOW_LONG_WE_KEEP_LOGS;
                String deleteSQLStatement = "from " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                            " where " + TABLE_NAME + "." + TIMESTAMP_COLNAME +
                                            " < " + deadline;
                session.delete(deleteSQLStatement);
            }*/
            // flush new records
            flushtodb(session);
            context.commitTransaction();
        } catch (HibernateException e) {
            reportException("error deleting old records", e);
        } catch(TransactionException e) {
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

    private static class LogDumper extends Thread {
        public LogDumper() {
            super( "LogDumper" );
        }

        public void run() {
            if (parent == null) throw new IllegalStateException("parent not set");
            try {
                sleep(FLUSH_FREQUENCY*2);
            } catch (InterruptedException e) {
                reportException("flusherDeamon interrupted. " +
                                "this log handler will stop dumping logs to db", null);
                return;
            }
            boolean fullclean = true;
            while (true) {
                try {
                    sleep(FLUSH_FREQUENCY);
                } catch (InterruptedException e) {
                    reportException("flusherDeamon interrupted. " +
                                    "this log handler will stop dumping logs to db", null);
                    break;
                }
                try {
                    parent.cleanAndFlush(fullclean);
                } catch (Throwable e) {
                    reportException("unhandled exception", e);
                }
                fullclean = false;
            }
        }
        ServerLogHandler parent = null;
    }

    /**
     * where log records are stored waiting to be flushed to the database
     */
    private ArrayList cache = new ArrayList();
    private static long MAX_CACHE_SIZE = 1000;
    private String nodeid;
    private static final LogDumper flusherDeamon = new LogDumper();
    private static final long FLUSH_FREQUENCY = 6000;
    //private static final long FLUSH_FREQUENCY = 10;
    private static final long HOW_LONG_WE_KEEP_LOGS = (48*3600*1000);

    private static final String TIMESTAMP_COLNAME = "millis";
    private static final String TABLE_NAME = "ssg_logs";
    private static final String NODEID_COLNAME = "nodeId";
    private static final String SEQ_COLNAME = "sequenceNumber";
}
