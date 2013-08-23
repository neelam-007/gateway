package com.l7tech.server.util;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.BusExtensionPostProcessor;
import org.apache.cxf.bus.spring.SpringBeanLocator;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * BusExtensionPostProcessor that avoids circular lookup of the "cxf" bean.
 */
public class SafeCxfBusExtensionPostProcessor extends BusExtensionPostProcessor {

    private Bus bus;
    private ApplicationContext context;

    public void setBus( final Bus bus ) {
        if ( this.bus == null ) {
            this.bus = bus;
        }
    }

    public void setApplicationContext( final ApplicationContext ctx ) {
        context = ctx;
        bus.setExtension( context, ApplicationContext.class );
        bus.setExtension( new SpringBeanLocator( context, getBus() ), ConfiguredBeanLocator.class );
    }

    public Object postProcessBeforeInitialization( final Object bean,
                                                   final String beanId )
            throws BeansException {
        if ( null != getBus() && (bean instanceof BusExtension) ) {
            Class cls = ((BusExtension) bean).getRegistrationType();
            getBus().setExtension( bean, cls );
        }
        return bean;
    }

    private Bus getBus() {
        return bus;
    }
}
