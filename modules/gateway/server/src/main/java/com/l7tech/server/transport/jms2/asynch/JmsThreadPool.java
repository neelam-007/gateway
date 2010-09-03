package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.ServerConfig;
import com.l7tech.util.Config;
import com.l7tech.util.Resolver;
import com.l7tech.util.ValidatedConfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global Thread pool that holds/manages all Jms worker threads.
 *
 * @author: vchan
 */
public class JmsThreadPool<T extends Runnable> implements PropertyChangeListener {

    private static final Logger _logger = Logger.getLogger(JmsThreadPool.class.getName());

    /** Hold singleton instance */
    private static final AtomicReference<JmsThreadPool> _instance = new AtomicReference<JmsThreadPool>();

    /* Cluster property keys */
    private static final String PARAM_THREAD_DISTRIBUTION = "jmsEndpointThreadDistribution";

    /* constants passed to the executor */
    private static final int CORE_SIZE = 5;
    private static final long KEEP_ALIVE = 30000l; // 30 sec
    private static final long MAX_SHUTDOWN_TIME = 8000l; // 8 sec
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private static volatile ServerConfig serverConfig;//to support testing, volatile only for correctness

    /** Executor that handles the execution tasks via a threadpool */
    private ThreadPoolExecutor workerPool;

    /** Configured global thread size limit */
    private int maxPoolSize;

    /** Mutex for instances of JmsThreadPool, multiple may exist as while pool is shutting down _instance may
     * not longer refer to it, and instead it may refer to a new thread pool.*/
    private final Object synchLock = new Object();

    /** Thread pool stop flag */
    private boolean stop;

    /**
     * Default private constructor.
     */
    private JmsThreadPool() {
        this.init();
    }

    /**
     * Returns the singleton instance of the JmsThreadPool.
     *
     * @return the JmsThreadPool singleton
     */
    public static JmsThreadPool getInstance() {

        final JmsThreadPool threadPoolTest = _instance.get();
        if (threadPoolTest == null) {
            synchronized (JmsThreadPool.class){
                //locked so only a single thread pool is created
                final JmsThreadPool doubleCheck = _instance.get();
                if(doubleCheck == null){
                    _instance.set(new JmsThreadPool());
                }
            }
        }

        return _instance.get();
    }

    /**
     * Re-creates the singleton instance of the JmsThreadPool to pickup property changes.  Only
     * used for cluster property changes.
     */
    private static void recreateInstance() {
        final JmsThreadPool prev = _instance.getAndSet(null);
        if (prev != null) prev.shutdown();
    }

    /**
     * Initializer method. Only called from synchronized block in private constructor.
     */
    private void init()  {
        // read cluster properties from a validated ServerConfig instance.
        final Config serverCfg = validated( getServerConfig() );

        // max Jms processing thread pool size
        // - what is emergency default?? - 25. If an illegal value is supplied it is ignored.
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
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT.equals(evt.getPropertyName())) {

            JmsThreadPool.recreateInstance();

            String newValue = (evt.getNewValue() != null ? evt.getNewValue().toString() : null);
            _logger.log(Level.CONFIG, "Updated JMS ThreadPool size to {0}.", newValue);

        } else if (PARAM_THREAD_DISTRIBUTION.equals(evt.getPropertyName())) {
            
        }
    }

    public void shutdown() {

        boolean doCompareAndSet = false;
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
                doCompareAndSet = true;
            }
        }

        if(doCompareAndSet){
            // Set the instance to null only if it holds a reference to the JmsThreadPool which is being shutdown.
            _instance.compareAndSet(this, null);
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

    public static void setServerConfig(ServerConfig serverConfig) {
        JmsThreadPool.serverConfig = serverConfig;
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

    private static ServerConfig getServerConfig() {
        if(serverConfig != null) return serverConfig;

        return ServerConfig.getInstance();
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, _logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                if(ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT.equals(key)){
                    return "jms.listenerThreadLimit";
                }
                return null;
            }
        } );

        vc.setMinimumValue( ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT, 1 );
        return vc;
    }
    
}
