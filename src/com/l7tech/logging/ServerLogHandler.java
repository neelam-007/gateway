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
     * Gets the last records of the table. Ugly logic because this table can grow to unreal proportions. We first
     * find a record id for the node involved near the end of the table using timestamp column. Then we play with
     * a record id range until we find the id range that gives us the number of records wanted.
     * @param nodeId the node id for which we want those records
     * @param maxSize how many records we want
     */
    private Collection getLastRecords(String nodeId, int maxSize, HibernatePersistenceContext context)
                                                                throws HibernateException, SQLException {
        // we need an id near the end of the table, make a query using
        // the logrecords timestamps
        long timewarp = 1;
        long recentTimeStamp = System.currentTimeMillis() - timewarp;
        boolean recentIdFound = false;
        long recendRecordId = -1;
        Exception lastException = null;
        while (!recentIdFound) {
            String findRecentRecordIdQuery = "SELECT " + TABLE_NAME + "." + OID_COLNAME + " FROM " + TABLE_NAME +
                                             " in class " + SSGLogRecord.class.getName() + " WHERE " + TABLE_NAME +
                                             "." + TSTAMP_COLNAME + " > " + recentTimeStamp + " AND " +
                                             TABLE_NAME + "." + NODEID_COLNAME + " = \'" + nodeId + "\'";
            Iterator i = context.getSession().iterate(findRecentRecordIdQuery);
            if (i != null && i.hasNext()) {
                Long recentRecord = (Long)i.next();
                recendRecordId = recentRecord.longValue();
                recentIdFound = true;
                break;
            } else {
                timewarp *= 10;
                recentTimeStamp = System.currentTimeMillis() - timewarp;
            }
            if (timewarp > 1000*3600*24*5) {
                // assume there is nothing at all beyond 5 days old
                break;
            }
        }
        if (!recentIdFound) {
            reportException("cannot not get id for recent record for node id " + nodeId, lastException);
            return Collections.EMPTY_LIST;
        }

        // we now have an idea of the most recent record ids
        // we will now make a search using the id to find the desired window size
        long idA = recendRecordId;
        long idB = recendRecordId - maxSize;
        int resA = countRecordsWithIdBiggerThan(idA, nodeId, context);
        int resB = countRecordsWithIdBiggerThan(idB, nodeId, context);

        // round up the data, this is tricky because it is a moving target
        if (resB > maxSize && resA > maxSize) {
            long range = idA - idB;
            idB = idA;
            idA = idA + range;
            resA = countRecordsWithIdBiggerThan(idA, nodeId, context);
            resB = countRecordsWithIdBiggerThan(idB, nodeId, context);
            int iterations = 1;
            while (resB > maxSize && resA > maxSize) {
                ++iterations;
                if (iterations > 10) {
                    reportException("could not round up range (too high " +
                                    resA + " for " + idA + " and " +
                                    resB + " for " + idB + ")", null);
                    return Collections.EMPTY_LIST;
                }
                range *= 10;
                idB = idA;
                idA = idA + range;
                resB = resA;
                resA = countRecordsWithIdBiggerThan(idA, nodeId, context);
            }
        } else if (resB < maxSize && resA < maxSize) {
            long step = idA - idB;
            final long max_step = 500000;
            idA = idB;
            idB = idB - step;
            resA = countRecordsWithIdBiggerThan(idA, nodeId, context);
            resB = countRecordsWithIdBiggerThan(idB, nodeId, context);
            int iterations = 1;
            boolean lessThanMaxTotal = false;
            while (resB < maxSize && resA < maxSize) {
                ++iterations;
                if (iterations > 10) {
                    if (resA > 0 || resB > 0) {
                        lessThanMaxTotal = true;
                        break;
                    } else {
                        reportException("could not round up range (too low " +
                                        resA + " for " + idA + " and " +
                                        resB + " for " + idB + ")", null);
                        return Collections.EMPTY_LIST;
                    }
                }
                step *= 10;
                // dont let range get out of control
                if (step > max_step) {
                    step = max_step;
                }
                if (idB == 0) {
                    // we're done
                    iterations = 10;
                } else if ((idB-step) <= 0) {
                    // one last time
                    idB = 0;
                    iterations = 9;
                } else {
                    idA = idB;
                    idB = idB - step;
                    resA = resB;
                    resB = countRecordsWithIdBiggerThan(idB, nodeId, context);
                }
            }
            if (lessThanMaxTotal) {
                String selStatement = "SELECT FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                      " WHERE "  + TABLE_NAME + "." + OID_COLNAME + " > " + idB + " AND " +
                                      TABLE_NAME + "." + NODEID_COLNAME + " = \'" + nodeId + "\'";
                return context.getSession().find(selStatement);
            }
        }

        // at this point, the data is rounded up
        // binary search
        long ultimateId = -1;
        if (resB > maxSize && resA < maxSize) {
            int iterations = 1;
            while (resB > maxSize && resA < maxSize) {
                if (iterations > 10) {
                    // we can't do this for ever!
                    ultimateId = (idA + idB)/2;
                    break;
                }
                long midPoint = (idA + idB)/2;
                int midPointValue = countRecordsWithIdBiggerThan(midPoint, nodeId, context);
                if (midPointValue > maxSize) {
                    idB = midPoint;
                } else if (midPointValue < maxSize) {
                    idA = midPoint;
                } else {
                    ultimateId = midPoint;
                    break;
                }
                resA = countRecordsWithIdBiggerThan(idA, nodeId, context);
                resB = countRecordsWithIdBiggerThan(idB, nodeId, context);
            }
        } else if (resB < maxSize && resA > maxSize) {
            // should not get here
            reportException("should not get here 167", null);
            return Collections.EMPTY_LIST;
        } else if (resB == maxSize) {
            ultimateId = idB;
        } else if (resA == maxSize) {
            ultimateId = idA;
        }

        if (ultimateId == -1) {
            reportException("could not find appropriate range", null);
            return Collections.EMPTY_LIST;
        }

        String selStatement = "SELECT FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() + " WHERE "  +
                              TABLE_NAME + "." + OID_COLNAME + " > " + ultimateId + " AND " + TABLE_NAME + "." +
                              NODEID_COLNAME + " = \'" + nodeId + "\'";
        List output = context.getSession().find(selStatement);
        return output;
    }

    private int countRecordsWithIdBiggerThan(long startId, String nodeId, HibernatePersistenceContext context)
                                                                        throws HibernateException, SQLException {
        String countselstatement = "SELECT COUNT(*) FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                   " WHERE "  + TABLE_NAME + "." + OID_COLNAME + " > " + startId + " AND " +
                                   TABLE_NAME + "." + NODEID_COLNAME + " = \'" + nodeId + "\'";

        Iterator i = context.getSession().iterate(countselstatement);
        if (i != null && i.hasNext()) {
            Integer toto = (Integer)i.next();
            return toto.intValue();
        } else return 0;
    }

    private int countRecordsInRange(long startId, long endId, String nodeId, HibernatePersistenceContext context)
                                                                        throws HibernateException, SQLException {
        String countselstatement = "SELECT COUNT(*) FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                                   " WHERE "  + TABLE_NAME + "." + OID_COLNAME + " > " + startId + " AND " +
                                   TABLE_NAME + "." + OID_COLNAME + " < " + endId + " AND " +
                                   TABLE_NAME + "." + NODEID_COLNAME + " = \'" + nodeId + "\'";

        Iterator i = context.getSession().iterate(countselstatement);
        if (i != null && i.hasNext()) {
            Integer toto = (Integer)i.next();
            return toto.intValue();
        } else return 0;
    }

    private Collection getRecordsInRange(String nodeId, int maxSize, HibernatePersistenceContext context,
                                        long startid, long endid) throws HibernateException, SQLException {
        //
        // NOTE
        // it's ok to use order by limit here because we have other criteria before the node id
        //
        String selStatement = "SELECT FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                              " WHERE " + TABLE_NAME + "." + OID_COLNAME + " > " + startid +
                              " AND " + TABLE_NAME + "." + OID_COLNAME + " < " + endid + " AND " + TABLE_NAME +
                              "." + NODEID_COLNAME + " = \'" + nodeId + "\' ORDER BY " +
                              TABLE_NAME + "." + OID_COLNAME + " DESC LIMIT " + maxSize;
        return context.getSession().find(selStatement);
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
        String selStatement = "SELECT FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                              " WHERE " + TABLE_NAME + "." + OID_COLNAME + " > " + startid +
                              " AND " + TABLE_NAME + "." + NODEID_COLNAME + " = \'" + nodeId + "\' ORDER BY " +
                              TABLE_NAME + "." + OID_COLNAME + " DESC LIMIT " + maxSize;
        return context.getSession().find(selStatement);
    }

    private Collection getRecordsBeforeLowId(String nodeId, int maxSize, HibernatePersistenceContext context,
                                            long endid) throws HibernateException, SQLException {
        // find an appropriate range that will give max records closest to endid of size maxSize
        // round up the id that will provide this
        long step = maxSize;
        long idA = endid - step + 1; // the highest possible value
        long idB = idA - step;
        int resA = countRecordsInRange(idA, endid, nodeId, context);
        int resB = countRecordsInRange(idB, endid, nodeId, context);

        // try to count beyond the desired max
        int iterations = 1;
        long magicId = -1;
        while (resB < maxSize) {
            ++iterations;
            if (iterations > 10) {
                // dont do this for ever
                if (resA == resB) magicId = idA;
                else magicId = idB;
                break;
            }
            step *= 10;
            if (step > 500000) step = 500000;
            idA = idB;
            idB = idB - step;
            resA = resB;
            resB = countRecordsInRange(idB, endid, nodeId, context);
        }
        if (resB == maxSize) { // did we get lucky?
            magicId = idB;
        } else if (resB > maxSize) { // did we round up the data?
            // binary search
            iterations = 1;
            while (true) {
                long midpoint = (idA + idB)/2;
                if (iterations > 10) {
                    // dont do this for ever
                    magicId = midpoint;
                    break;
                }
                // peek at the middle
                int resmid = countRecordsInRange(midpoint, endid, nodeId, context);
                if (resmid > maxSize) {
                    idB = midpoint;
                } else if (resmid < maxSize) {
                    idA = midpoint;
                } else {
                    magicId = midpoint;
                    break;
                }
            }
        }
        String selStatement = "SELECT FROM " + TABLE_NAME + " in class " + SSGLogRecord.class.getName() +
                              " WHERE " + TABLE_NAME + "." + OID_COLNAME + " < " + endid +
                              " AND " + TABLE_NAME + "." + OID_COLNAME + " > " + magicId +
                              " AND " + TABLE_NAME + "." + NODEID_COLNAME + " = \'" + nodeId + "\'";
        return context.getSession().find(selStatement);
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
        Collection res = null;
        try {
            if (lowMsgNumber < 0 && highMsgNumber >= 0) {
                res = getRecordsBeyondHighId(reqnode, size, context, highMsgNumber);
            } else if (lowMsgNumber >= 0 && highMsgNumber < 0) {
                res = getRecordsBeforeLowId(reqnode, size, context, lowMsgNumber);
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
        }
        if (res == null) return Collections.EMPTY_LIST;
        return res;
    }

    /*
    public Collection getLogRecordsOld(String nodeId, long highMsgNumber, long lowMsgNumber, int size) {
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
            //selStatement += " limit " + size + " " + offsetvalue;
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
    }*/

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
    private static final String TSTAMP_COLNAME = "millis";
}
