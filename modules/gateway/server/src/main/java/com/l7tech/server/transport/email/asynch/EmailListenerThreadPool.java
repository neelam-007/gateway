package com.l7tech.server.transport.email.asynch;

import com.l7tech.server.ServerConfig;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages a pool of threads that process messages from email listeners.
 */
public class EmailListenerThreadPool<T extends Runnable> {
    private static final Logger _logger = Logger.getLogger(EmailListenerThreadPool.class.getName());

    /** Singleton instance */
    private static EmailListenerThreadPool _instance;

    /* constants passed to the executor */
    private static int CORE_SIZE = 5;
    private static final long KEEP_ALIVE = 30000l; // 30 sec
    private static final long MAX_SHUTDOWN_TIME = 8000l; // 8 sec
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    
    /** Executor that handles the execution tasks via a threadpool */
    private ThreadPoolExecutor workerPool;
    
    /** Configured global thread size limit */
    private int maxPoolSize;

    /** The thread distribution scheme the pool will used to deploy worker threads across all Jms endpoints */
    private ThreadDistributionScheme distributionScheme = ThreadDistributionScheme.EVENLY_DISTRIBUTED;

    /** Thread pool initialized flag */
    private boolean initialized;

    /** Mutex */
    private Object synchLock = new Object();

    /** Thread pool stop flag */
    private boolean stop;
    
    public EmailListenerThreadPool() {
        try {
            this.init();
        } catch (Exception ex) {
            // TODO proper exception handling
            ex.printStackTrace();
        }
    }

    public static final EmailListenerThreadPool getInstance() {
        if (_instance == null) {
            _instance = new EmailListenerThreadPool();
        }

        return _instance;
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
                maxPoolSize = serverCfg.getIntProperty(ServerConfig.PARAM_EMAIL_LISTENER_THREAD_LIMIT, 25); // or DEFAULT_JMS_THREAD_POOL_SIZE?

                // thread distribution scheme
                String distSetting;
                if ((distSetting = serverCfg.getProperty(ServerConfig.PARAM_EMAIL_LISTENER_THREAD_DISTRIBUTION)) != null && distSetting.length() > 0) {
                    try {
                        distributionScheme = ThreadDistributionScheme.valueOf(distSetting);
                    } catch (IllegalArgumentException ill) {
                        distributionScheme = ThreadDistributionScheme.getDefault();
                    }
                }

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

    protected enum ThreadDistributionScheme {
        EVENLY_DISTRIBUTED,
        ADHOC;

        static ThreadDistributionScheme getDefault() {
            return EVENLY_DISTRIBUTED;
        }
    }
}
