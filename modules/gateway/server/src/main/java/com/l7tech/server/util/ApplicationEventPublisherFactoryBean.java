package com.l7tech.server.util;

import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Factory bean to make the default ApplicationEventPublisher available as a regular bean.
 */
public class ApplicationEventPublisherFactoryBean implements SmartFactoryBean<ApplicationEventPublisher>, ApplicationEventPublisherAware {

    //- PUBLIC

    @Override
    public ApplicationEventPublisher getObject() throws Exception {
        return applicationEventPublisher;
    }

    @Override
    public Class<ApplicationEventPublisher> getObjectType() {
        return ApplicationEventPublisher.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isEagerInit() {
        return true;
    }

    @Override
    public boolean isPrototype() {
        return false;
    }

    @Override
    public void setApplicationEventPublisher( final ApplicationEventPublisher applicationEventPublisher ) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    //- PRIVATE

    private ApplicationEventPublisher applicationEventPublisher;
}
