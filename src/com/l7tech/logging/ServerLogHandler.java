package com.l7tech.logging;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.ArrayList;

import com.l7tech.common.RequestId;
import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.server.MessageProcessor;
import com.l7tech.message.Request;

/**
 * A logging handler that records SSGLogRecord objects and stored them in a database table.
 *
 * Not plugged in yet but this will eventually replace the MemHandler.
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
     * during initialization, this class requires the use of ClusterInfoManager
     * in turn, that class uses the LogManager.getInstance(). therefore, this manager
     * should not be initialized at part of LogManager.getInstance().
     */
    public ServerLogHandler() {
        nodeid = ClusterInfoManager.getInstance().thisNodeId();
        // todo, initialize the deamon that flushes the cache to db
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

    public void close() throws SecurityException {
    }

    /**
     * record a log record in tmp cache. it will eventually be flushed by a deamon thread
     */
    protected void add(SSGLogRecord arg) {
        Sync ciReadLock = rwlock.readLock();
        try {
            ciReadLock.acquire();
        } catch (InterruptedException e) {
            reportException("Exception locking. Log record lost", e);
            return;
        }
        try {
            // what is the deamon is not flushing fast enough?
            if (cache.size() >= MAX_CACHE_SIZE) {
                // todo, maybe we should force a flush?
                cache.remove(0);
            }
            cache.add(arg);
        } finally {
            if (ciReadLock != null) ciReadLock.release();
        }
    }

    protected void reportException(String msg, Throwable e) {
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
    // read-write lock for thread safety
    private final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();
    private static long MAX_CACHE_SIZE = 6000;
    private String nodeid;
}
