package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.ServerConfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global Thread pool that holds/manages all Jms worker threads.
 *
 * @author: vchan
 */
public class JmsThreadPool<T extends Runnable> implements PropertyChangeListener {

    private static final Logger _logger = Logger.getLogger(JmsThreadPool.class.getName());

    /** Singleton instance */
    private static JmsThreadPool _instance;

    /* Cluster property keys */
    private static final String PARAM_THREAD_LIMIT = "jmsListenerThreadLimit";
    private static final String PARAM_THREAD_DISTRIBUTION = "jmsEndpointThreadDistribution";

    /* constants passed to the executor */
    private static int CORE_SIZE = 5;
    private static final long KEEP_ALIVE = 30000l; // 30 sec
    private static final long MAX_SHUTDOWN_TIME = 8000l; // 8 sec
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    /** Executor that handles the execution tasks via a threadpool */
    private ThreadPoolExecutor workerPool;

    /** Configured global thread size limit */
    private int maxPoolSize;

    /** Thread pool initialized flag */
    private boolean initialized;

    /** Mutex */
    private final Object synchLock = new Object();
    /** Mutex for ThreadPool singleton instance updates */
    private static final Object updateLock = new Object();

    /** Thread pool stop flag */
    private boolean stop;

    /**
     * Default private constructor.
     *
     * @throws Exception if the pool initialization fails.
     */
    private JmsThreadPool() {

        try {
            this.init();
        } catch (Exception ex) {
            // should not get to this point
            ex.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance of the JmsThreadPool.
     *
     * @return the JmsThreadPool singleton
     */
    public static JmsThreadPool getInstance() {

        if (_instance == null) {
            _instance = new JmsThreadPool();
        }

        return _instance;
    }

    /**
     * Re-creates the singleton instance of the JmsThreadPool to pickup property changes.  Only
     * used for cluster property changes.
     */
    private static void recreateInstance() {

        JmsThreadPool oldThreadPool;
        synchronized (updateLock) {
            // clear the singleton instance so it gets re-created on next getInstance call;
            oldThreadPool = _instance;
            _instance = null;
        }

        // the shutdown is controlled and gives time to existing threads to complete execution
        oldThreadPool.shutdown();
    }

    /**
     * Initializer method.
     *
     * @throws Exception when errors are encountered while initializing the pool
     */
    private void init() throws Exception {

        // do we need synchronization? -- yes
        synchronized (synchLock) {
            if (!initialized) {

                // read cluster properties from the serverConfig
                ServerConfig serverCfg = ServerConfig.getInstance();

                // max Jms processing thread pool size
                // - what is emergency default??
                maxPoolSize = serverCfg.getIntProperty(ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT, 25); // or DEFAULT_JMS_THREAD_POOL_SIZE?

                // create the pool executor
                workerPool = new ThreadPoolExecutor(
                        CORE_SIZE,
                        getMaxSize(),
                        KEEP_ALIVE,
                        TIME_UNIT,
                        new LinkedBlockingQueue(CORE_SIZE) // TODO: need to revisit
                        // Use default ThreadFactory for now
                );

                // set the flag
                initialized = true;
            }
        }
    }


    public void propertyChange(PropertyChangeEvent evt) {

        if (PARAM_THREAD_LIMIT.equals(evt.getPropertyName())) {

            JmsThreadPool.recreateInstance();

            String newValue = (evt.getNewValue() != null ? evt.getNewValue().toString() : null);
            _logger.log(Level.CONFIG, "Updated JMS ThreadPool size to {0}.", newValue);

        } else if (PARAM_THREAD_DISTRIBUTION.equals(evt.getPropertyName())) {
            
        }
    }

    public void shutdown() {
        
        synchronized (synchLock) {
            if (!stop) {

                // shutdown the executor
                workerPool.shutdown();

                // check to ensure tasks left in the work queue are not dropped
                try {
                    long start = System.currentTimeMillis();
                    long now = start;
                    for (;(workerPool.getQueue().size() > 0) && (now-start < MAX_SHUTDOWN_TIME); )
                    {
                        workerPool.awaitTermination(2000L, TIME_UNIT);
                        now = System.currentTimeMillis();
                    }

                } catch (InterruptedException iex) {
                    _logger.info("JmsThreadPool shutdown interrupted.");
                }

                // log threadpool stats
                _logger.info(stats());
                stop = this.workerPool.isShutdown();

                // null the instance to force - re-initialize on next access
                _instance = null;
            }
        }
    }

    /**
     * Add a new task for the workpool to execute.
     *
     * @param newTask
     */
    public void newTask(T newTask) {
        workerPool.execute(newTask);
    }


    private String stats() {

        final String SEP="\n";

        StringBuffer sb = new StringBuffer("Stats:").append(SEP);
        sb.append("MaxRunningThreads=").append(workerPool.getLargestPoolSize()).append(SEP);
        sb.append("CompletedTasks=").append(workerPool.getCompletedTaskCount()).append(SEP);
        sb.append("TasksCount=").append(workerPool.getTaskCount()); //.append(SEP);

        return sb.toString();
    }

    private int getMaxSize() {
        return maxPoolSize;
    }
}
