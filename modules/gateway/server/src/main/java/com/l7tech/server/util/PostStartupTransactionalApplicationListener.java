package com.l7tech.server.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.*;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EventListener;
import java.util.logging.Logger;

/**
 * An "ApplicationListener" that is only registered on context start.
 */
public interface PostStartupTransactionalApplicationListener<E extends ApplicationEvent> extends EventListener {

    /**
     * Get handle an application event.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    @NotNull
    ApplicationListener<E> getListener();

    public static final class StartupListenerRegistration implements ApplicationContextAware, Lifecycle, Ordered {
        private static final Logger logger = Logger.getLogger(StartupListenerRegistration.class.getName());
        private ApplicationContext applicationContext;

        @Override
        public void setApplicationContext( final ApplicationContext applicationContext ) {
            this.applicationContext = applicationContext;
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void start() {
            logger.info("Registering post startup transactional application listeners.");
            final ApplicationEventMulticaster eventMulticaster = applicationContext.getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
            for ( final PostStartupTransactionalApplicationListener<?> listener : applicationContext.getBeansOfType( PostStartupTransactionalApplicationListener.class ).values() ) {
                eventMulticaster.addApplicationListener( listener.getListener() );
            }
        }

        @Override
        public void stop() {
        }

        @Override
        public int getOrder() {
            return -100000;
        }
    }
}
