package com.l7tech.logging;

import com.l7tech.common.RequestId;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.server.MessageProcessor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.*;


/**
 * A logging handler that records SSGLogRecord objects and stored them in a database table.
 * <p/>
 * Initialization of this handler requires the node id which is retrieved through the ClusterInfoManager
 * and in turn requires the availability of the persistence context. Because the persistence context
 * makes use the log manager, initialization of this handler within the log manager potentially causes
 * a race condition and must be handled in a special way.
 * <p/>
 * The way this is achieved in the log manager involves initializing this handler in a seperate thread,
 * catch the IllegalStateException and retry until it works.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 13, 2004<br/>
 * $Id$<br/>
 */
public class ServerLogHandler extends Handler implements PropertyChangeListener {
    private LogManager manager = LogManager.getLogManager();
    private final ServerLogManager serverLogManager = ServerLogManager.getInstance();

    /**
     * note the two phase construction ServerLogHandler.initialize()
     */
    public ServerLogHandler() {
        super();
        configure();
        manager.addPropertyChangeListener(this);
        flusherTask = new TimerTask() {
            public void run() {
                cleanAndFlush();
            }
        };
        // as configurable properties
        flusherDeamon.schedule(flusherTask, FLUSH_FREQUENCY, FLUSH_FREQUENCY);
    }

    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        Request req = MessageProcessor.getCurrentRequest();
        RequestId reqid = null;
        if (req != null) {
            reqid = req.getId();
        }
        SSGLogRecord newRecord = new SSGLogRecord(record, reqid, serverLogManager.getNodeid());
        add(newRecord);
    }

    public void flush() {
        cleanAndFlush();
    }


    public void close() throws SecurityException {
        flusherDeamon.cancel();
    }

    /**
     * This method gets called when a logmanager config gets changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // need to add new instance  again on change, as this handler was added programatically
        manager.removePropertyChangeListener(this);
        final Logger rootLogger = manager.getLogger("");
        ServerLogHandler newHandler = new ServerLogHandler();
        manager.addPropertyChangeListener(newHandler);
        rootLogger.addHandler(newHandler);
    }

    // Private method to configure Handler from LogManager
    // properties and/or default values as specified in the class
    private void configure() {
        String cname = ServerLogHandler.class.getName();

        setLevel(getLevelProperty(cname + ".level", Level.ALL));
        setFilter(getFilterProperty(cname + ".filter", null));
        setFormatter(getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        try {
            setEncoding(getStringProperty(cname + ".encoding", null));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {
                // doing a setEncoding with null should always work.
                // assert false;
            }
        }
    }


    /**
     * record a log record in tmp cache. it will eventually be flushed by a deamon thread
     */
    private void add(SSGLogRecord arg) {
        synchronized (cache) {
            if (cache.size() >= MAX_CACHE_SIZE) {
                // todo, maybe we should force a flush?
                cache.remove(0);
            }
            cache.add(arg);
        }
    }

    /**
     * performs the regular maintenance task including cleaning the log table if necessary
     * and flushing new cached log entries to database.
     * this method is responsible to manager its own persistence context.
     */
    private void cleanAndFlush() {
        // get the persistence context
        HibernatePersistenceContext context = null;
        Session session = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            session = context.getSession();
        } catch (SQLException e) {
            reportException("cannot get persistence context", e);
            if (context != null) context.close();
            return;
        } catch (HibernateException e) {
            reportException("cannot get session", e);
            if (context != null) context.close();
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
        } catch (TransactionException e) {
            reportException("Exception with hibernate transaction", e);
        } finally {
            if (context != null) context.close();
        }
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

    // Package private method to get a Level property.
    // If the property is not defined or cannot be parsed
    // we return the given default value.
    private Level getLevelProperty(String name, Level defaultValue) {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Level.parse(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    // Package private method to get a filter property.
    // We return an instance of the class named by the "name"
    // property. If the property is not defined or has problems
    // we return the defaultValue.
    private Filter getFilterProperty(String name, Filter defaultValue) {
        String val = manager.getProperty(name);
        try {
            if (val != null) {
                Class clz = getClass().getClassLoader().loadClass(val);
                return (Filter)clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return defaultValue;
    }


    // Package private method to get a formatter property.
    // We return an instance of the class named by the "name"
    // property. If the property is not defined or has problems
    // we return the defaultValue.
    private Formatter getFormatterProperty(String name, Formatter defaultValue) {
        String val = manager.getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Formatter)clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return defaultValue;
    }

    // Package private method to get a String property.
    // If the property is not defined we return the given
    // default value.
    private String getStringProperty(String name, String defaultValue) {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return val.trim();
    }

    // Package private method to get an integer property.
    // If the property is not defined or cannot be parsed
    // we return the given default value.
    private int getIntProperty(String name, int defaultValue) {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * where log records are stored waiting to be flushed to the database
     */
    private ArrayList cache = new ArrayList();
    private TimerTask flusherTask = null;
    private final Timer flusherDeamon = new Timer(true);

    private static long MAX_CACHE_SIZE = 1000;
    private static final long FLUSH_FREQUENCY = 15000;

}
