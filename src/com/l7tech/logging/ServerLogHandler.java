package com.l7tech.logging;

import com.l7tech.common.RequestId;
import com.l7tech.common.util.Background;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
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
    private final ServerLogManager serverLogManager;

    /**
     * note the two phase construction ServerLogHandler.initialize()
     */
    public ServerLogHandler(ServerLogManager serverLogManager) {
        super();
        this.serverLogManager = serverLogManager;
        configure();
        manager.addPropertyChangeListener(this);
        flusherTask = new TimerTask() {
            public void run() {
                cleanAndFlush();
            }
        };
        // as configurable properties
        Background.schedule(flusherTask, FLUSH_FREQUENCY, FLUSH_FREQUENCY);
    }

    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }


        final PolicyEnforcementContext currentContext = MessageProcessor.getCurrentContext();
        RequestId reqid = currentContext == null ? null : currentContext.getRequestId();
        SSGLogRecord newRecord = new SSGLogRecord(record, reqid, serverLogManager.getNodeid());
        try {
            String msg;
            Formatter formatter = getFormatter();
            if (formatter != null) {
                msg = formatter.format(record);
                newRecord.setMessage(msg);
            }
        } catch (Exception ex) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
        }
        add(newRecord);
    }

    public void flush() {
        cleanAndFlush();
    }


    public void close() throws SecurityException {
        flusherTask.cancel();
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
        ServerLogHandler newHandler = new ServerLogHandler(serverLogManager);
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
        SSGLogRecord[] data = null;
        synchronized (cache) {
            if (cache.isEmpty()) return;
            data = (SSGLogRecord[])cache.toArray(new SSGLogRecord[]{});
            cache.clear();
        }
        try {
            serverLogManager.save(data);
        } catch (SaveException e) {
            reportException("error saving to database", e);
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

    /**
     * where log records are stored waiting to be flushed to the database
     */
    private ArrayList cache = new ArrayList();
    private TimerTask flusherTask = null;

    private static long MAX_CACHE_SIZE = 1000;
    private static final long FLUSH_FREQUENCY = 15000;

}
