package com.l7tech.server.extension.event.metrics;

import com.ca.apim.gateway.extension.event.EventListener;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.extension.registry.event.EventListenerRegistry;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jetbrains.annotations.TestOnly;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;

import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.NORM_PRIORITY;

/**
 * Listener for {@link ServiceMetricsEvent}. Looks to the event listener registry and delegates for the registered listeners.
 */
public class ServiceMetricsEventListenerProxy implements Lifecycle, PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(ServiceMetricsEventListenerProxy.class.getName());

    private static final String THREAD_NAME_PATTERN = "ServiceMetricsEventListenerProxy_%s";
    private static final long THREAD_KEEP_ALIVE_TIME_SECONDS = 5L;
    private static final long THREAD_TERMINATION_TIMEOUT_SECONDS = 10L;
    private static final String CLUSTER_PROP_THREAD_POOL_SIZE = "serviceMetricsEventListener.maxPoolSize";
    private static final int CLUSTER_PROP_THREAD_POOL_SIZE_DEFAULT_VALUE = 25;

    @Inject
    private EventListenerRegistry eventListenerRegistry;
    @Inject
    private Config config;
    @Inject
    private EventChannel eventChannel;
    private ThreadPoolExecutor executorService;

    public void handleEvent(ApplicationEvent event) {
        if (event instanceof MessageProcessed) {
            MessageProcessed messageProcessed = (MessageProcessed) event;
            if (!messageProcessed.getContext().isPolicyExecutionAttempted()) {
                return;
            }
            Collection<EventListener<ServiceMetricsEvent>> listeners = this.eventListenerRegistry.getEventListenersFor(ServiceMetricsEvent.class);
            if (listeners.isEmpty()) {
                LOGGER.log(Level.FINE, "No listeners available for event of type " + ServiceMetricsEvent.class.getName());
                return;
            }

            ServiceMetricsEvent metricsEvent = new ServiceMetricsEvent(messageProcessed.getContext());
            for (final EventListener<ServiceMetricsEvent> listener : listeners) {
                // the ExecutorService takes care of calling our listeners in a queue.
                this.submit(listener, metricsEvent);
            }
        }
    }

    /**
     * Send the event to be processed in a separarate thread.
     *
     * @param listener the event listener to be called
     * @param metricsEvent the event fired
     */
    void submit(final EventListener<ServiceMetricsEvent> listener, ServiceMetricsEvent metricsEvent) {
        // the ExecutorService takes care of calling our listeners in a queue.
        this.executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onEvent(metricsEvent);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, ExceptionUtils.getDebugException(e), new Supplier<String>() {
                        @Override
                        public String get() {
                            return "Error invoking listener '" + listener.getClass() + "'. " + ExceptionUtils.getMessage(e);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void start() {
        // add self to the event channel from message processing
        this.eventChannel.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent applicationEvent) {
                handleEvent(applicationEvent);
            }
        });

        final int poolSize = this.config.getIntProperty(CLUSTER_PROP_THREAD_POOL_SIZE, CLUSTER_PROP_THREAD_POOL_SIZE_DEFAULT_VALUE);

        // Set core and max pool size the same, and use unbounded work queue.
        // If all threads are busy, new tasks to wait in the queue until a thread becomes available.
        this.executorService = new ThreadPoolExecutor(
                poolSize, poolSize,
                THREAD_KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), // Unbounded work queue.
                new BasicThreadFactory.Builder().namingPattern(THREAD_NAME_PATTERN).priority(NORM_PRIORITY).build());
        this.executorService.allowCoreThreadTimeOut(true);
    }

    @Override
    public void stop() {
        try {
            this.executorService.awaitTermination(THREAD_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            this.executorService.shutdownNow();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, ExceptionUtils.getDebugException(e), new Supplier<String>() {
                @Override
                public String get() {
                    return "Error stopping service metrics thread pool. " + ExceptionUtils.getMessage(e);
                }
            });
        }
    }

    @Override
    public boolean isRunning() {
        return this.executorService != null && !this.executorService.isShutdown();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        // uses the default in case of any weird behaviour
        if (CLUSTER_PROP_THREAD_POOL_SIZE.equals(propertyName)) {
            this.executorService.setCorePoolSize(this.config.getIntProperty(CLUSTER_PROP_THREAD_POOL_SIZE, CLUSTER_PROP_THREAD_POOL_SIZE_DEFAULT_VALUE));
            this.executorService.setMaximumPoolSize(this.config.getIntProperty(CLUSTER_PROP_THREAD_POOL_SIZE, CLUSTER_PROP_THREAD_POOL_SIZE_DEFAULT_VALUE));
        }
    }

    @TestOnly
    void setEventListenerRegistry(EventListenerRegistry eventListenerRegistry) {
        this.eventListenerRegistry = eventListenerRegistry;
    }
}
