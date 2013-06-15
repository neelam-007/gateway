package com.l7tech.server.transport.http;

import com.l7tech.server.transport.http.HttpConnectionManagerListener2.HttpConnectionManagerListenerAdapter2;
import com.l7tech.server.util.ManagedTimerTask;
import static com.l7tech.util.CollectionUtils.foreach;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions.UnaryVoid;
import org.apache.http.conn.ClientConnectionManager;
import org.springframework.context.Lifecycle;
import org.springframework.core.Ordered;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages idle timeout of outbound HTTP connections.
 *
 * <p>Based on Apache HTTP Client IdleConnectionTimeoutThread, but uses weak
 * references to allow GC of connection managers.</p>
 */
public class HttpConnectionIdleTimeoutManager2 extends HttpConnectionManagerListenerAdapter2 implements Ordered, Lifecycle {

    //- PUBLIC

    public HttpConnectionIdleTimeoutManager2(final Config config,
                                             final Timer timer) {
        this.config = config;
        this.timer = timer;
    }

    @Override
    public void start() {
        synchronized ( lifecycleSync ) {
            if ( task == null ) {
                final long interval = config.getTimeUnitProperty( "com.l7tech.server.transport.http.httpConnectionIdleInterval", 5000L );
                if ( interval > 0 ) {
                    task = new IdleTimeoutTask( config, connectionManagers );
                    timer.schedule( task, interval, interval );
                }
            }
        }
    }

    @Override
    public void stop() {
        synchronized ( lifecycleSync ) {
            if ( task != null ) {
                task.cancel();
                task = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        boolean running;
        synchronized ( lifecycleSync ) {
            running = task != null;
        }
        return running;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    /**
     * Register an HttpConnectionManager for idle connection timeout.
     *
     * @param httpConnectionManager The manager to register, null values are ignored.
     */
    public void registerHttpConnectionManager( final ClientConnectionManager httpConnectionManager ) {
        if ( httpConnectionManager != null ) synchronized ( connectionManagers ) {
            connectionManagers.put( httpConnectionManager, null );
        }
    }

    /**
     * un-Register an HttpConnectionManager for idle connection timeout.
     *
     * @param httpConnectionManager The manager to register, null values are ignored.
     */
    public void unRegisterHttpConnectionManager( final ClientConnectionManager httpConnectionManager ) {
        if ( httpConnectionManager != null ) synchronized ( connectionManagers ) {
            connectionManagers.remove(httpConnectionManager);
        }
    }


    @Override
    public void notifyHttpConnectionManagerCreated( final ClientConnectionManager manager ) {
        registerHttpConnectionManager( manager );
    }

    @Override
    public void notifyHttpConnectionManagerDestroyed(ClientConnectionManager manager) {
        unRegisterHttpConnectionManager(manager);
    }

    //- PRIVATE

    private final Object lifecycleSync = new Object();
    private final Map<ClientConnectionManager,Void> connectionManagers = new WeakHashMap<ClientConnectionManager,Void>();
    private final Config config;
    private final Timer timer;
    private TimerTask task;

    private static final class IdleTimeoutTask extends ManagedTimerTask {
        private final Config config;
        private final Map<ClientConnectionManager,Void> connectionManagers;

        private IdleTimeoutTask( final Config config,
                                 final Map<ClientConnectionManager,Void> connectionManagers ) {
            this.config = config;
            this.connectionManagers = connectionManagers;
        }

        @Override
        protected void doRun() {
            final Set<ClientConnectionManager> managers = new HashSet<ClientConnectionManager>();
            synchronized ( connectionManagers ) {
                managers.addAll( connectionManagers.keySet() );
            }

            final long timeout = config.getTimeUnitProperty( "com.l7tech.server.transport.http.httpConnectionIdleTimeout", 0L );
            if ( timeout > 0 ) foreach( managers, false, new UnaryVoid<ClientConnectionManager>(){
                @Override
                public void call( final ClientConnectionManager httpConnectionManager ) {
                    httpConnectionManager.closeIdleConnections( timeout, TimeUnit.MILLISECONDS);
                }
            } );

            final boolean enabled = ConfigFactory.getBooleanProperty("com.l7tech.common.http.prov.apache.staleCheckEnabled", true);
            if (enabled) {
                foreach( managers, false, new UnaryVoid<ClientConnectionManager>(){
                    @Override
                    public void call( final ClientConnectionManager httpConnectionManager ) {
                        httpConnectionManager.closeExpiredConnections();
                    }
                } );
            }
        }
    }
}
