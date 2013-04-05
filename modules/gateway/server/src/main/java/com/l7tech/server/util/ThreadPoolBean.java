package com.l7tech.server.util;

import com.l7tech.util.Config;
import com.l7tech.util.Resolver;
import com.l7tech.util.ThreadPool;
import com.l7tech.util.ValidatedConfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a ThreadPool. Responsible for updating the thread pool when cluster properties change. This class should be
 * extended when the thread pool needs more configuration e.g. to change the core size.
 * <p/>
 * Usages of this bean must explicitly start the bean and stop the bean.
 * See {@link com.l7tech.util.ThreadPool} for detailed information on the behaviour of the underlying thread pool.
 *
 */
public class ThreadPoolBean implements PropertyChangeListener {
    public ThreadPoolBean(final Config config,
                          final String poolName,
                          final String maxPoolSizeClusterPropName,
                          final String maxPoolSizeClusterPropUiName,
                          final int maxPoolSizeEmergencyDefault) {
        this(config, poolName, maxPoolSizeClusterPropName, maxPoolSizeClusterPropUiName, maxPoolSizeEmergencyDefault, null);
    }

    public ThreadPoolBean(final Config config,
                          final String poolName,
                          final String maxPoolSizeClusterPropName,
                          final String maxPoolSizeClusterPropUiName,
                          final int maxPoolSizeEmergencyDefault,
                          final RejectedExecutionHandler handler) {
        this.poolName = poolName;
        this.maxPoolSizeClusterPropName = maxPoolSizeClusterPropName;
        this.maxPoolSizeClusterPropUiName = maxPoolSizeClusterPropUiName;
        this.maxPoolSizeEmergencyDefault = maxPoolSizeEmergencyDefault;
        this.validatedConfig = validated(config);
        threadPool = new ThreadPool(poolName, CORE_SIZE, getMaxPoolSize(), handler);
    }

    public void start() {
        threadPool.start();
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    public boolean isShutdown() {
        return threadPool.isShutdown();
    }

    public int getActiveCount() {
        return threadPool.getActiveCount();
    }

    /**
     * Submit a task for execution.
     *
     * @param task to process asynchronously
     * @throws ThreadPool.ThreadPoolShutDownException if the underlying thread pool has already been shut down.
     *
     */
    public void submitTask(final Runnable task) throws ThreadPool.ThreadPoolShutDownException {
        threadPool.submitTask( task );
    }

    /**
     * Submit a task for execution.
     *
     * @param task to process asynchronously
     * @return A future for the result
     * @throws ThreadPool.ThreadPoolShutDownException if the underlying thread pool has already been shut down.
     *
     */
    public <T> Future<T> submitTask( final Callable<T> task ) throws ThreadPool.ThreadPoolShutDownException {
        return threadPool.submitTask(task);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (maxPoolSizeClusterPropName.equals(evt.getPropertyName())) {
            final int poolSize = getMaxPoolSize();
            threadPool.setMaxPoolSize(poolSize);

            //use the value from getMaxPoolSize in case the user supplied value was invalid and ignored.
            logger.log(Level.CONFIG, "Updated " + poolName + " size to {0}.", poolSize);
        }
    }

    // - PRIVATE

    private Config validated(final Config config) {
        final ValidatedConfig vc = new ValidatedConfig(config, logger, new Resolver<String, String>() {
            @Override
            public String resolve(final String key) {
                if (maxPoolSizeClusterPropName.equals(key)) {
                    return maxPoolSizeClusterPropUiName;
                }
                return null;
            }
        });

        vc.setMinimumValue(maxPoolSizeClusterPropName, CORE_SIZE);
        return vc;
    }

    private int getMaxPoolSize() {
        return validatedConfig.getIntProperty(maxPoolSizeClusterPropName, maxPoolSizeEmergencyDefault);
    }

    private final Config validatedConfig;
    private final ThreadPool threadPool;
    private final String poolName;
    private final String maxPoolSizeClusterPropName;
    private final String maxPoolSizeClusterPropUiName;
    private final int maxPoolSizeEmergencyDefault;
    private final static int CORE_SIZE = 5;
    private static Logger logger = Logger.getLogger(ThreadPoolBean.class.getName());
}


