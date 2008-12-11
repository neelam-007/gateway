package com.l7tech.server.ems.util;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.BeansException;
                                           
/**
 * Event publisher
 */
public class ApplicationEventPublisherImpl implements ApplicationEventPublisher, ApplicationContextAware {

    //- PUBLIC

    /**
     * Publish the given event to the spring context.
     *
     * @param event The event to publish (not null)
     */
    @Override
    public void publishEvent( final ApplicationEvent event ) {
        applicationContext.publishEvent( event );
    }


    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        if ( this.applicationContext == null ) {
            this.applicationContext = applicationContext;
        }
    }

    //- PRIVATE

    private ApplicationContext applicationContext;
}
