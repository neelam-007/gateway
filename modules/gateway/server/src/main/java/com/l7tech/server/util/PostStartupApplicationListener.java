package com.l7tech.server.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.Ordered;

import java.util.EventListener;
import java.util.logging.Logger;

/**
 * An "ApplicationListener" that is only registered on context start.
 */
public interface PostStartupApplicationListener<E extends ApplicationEvent> extends EventListener {

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);

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
            logger.info("Registering post startup application listeners.");
            final ApplicationEventMulticaster eventMulticaster = applicationContext.getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
            for ( final PostStartupApplicationListener<?> listener : applicationContext.getBeansOfType( PostStartupApplicationListener.class ).values() ) {
                eventMulticaster.addApplicationListener( asApplicationListener(listener) );
            }
        }

        @Override
        public void stop() {
        }

        @Override
        public int getOrder() {
            return -100000;
        }

        private <T extends ApplicationEvent> ApplicationListener<T> asApplicationListener( final PostStartupApplicationListener<T> listener ) {
            return new ApplicationListener<T>(){
               @Override
               public void onApplicationEvent( final T event ) {
                   listener.onApplicationEvent( event );
               }
           };
        }
    }
}
