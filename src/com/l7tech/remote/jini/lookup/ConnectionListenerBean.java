/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.jini.lookup;

import com.l7tech.console.security.LogonEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;

import java.util.Iterator;

/**
 * @author emil
 * @version Oct 6, 2004
 */
public class ConnectionListenerBean extends ApplicationObjectSupport
  implements ApplicationListener {
    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogonEvent) {
            LogonEvent logonEvent = (LogonEvent)event;
            if (logonEvent.getType() == LogonEvent.LOGOFF) {
                resetServiceStubs();
            }
        }
    }

    private void resetServiceStubs() {
        //todo: rework
//        final ApplicationContext ctx = getApplicationContext();
//        Iterator it = ctx.getBeansOfType(JiniProxyFactoryBean.class, false, true).keySet().iterator();
//        while (it.hasNext()) {
//            try {
//                JiniProxyFactoryBean bean = (JiniProxyFactoryBean)ctx.getBean("&" + it.next());
//                bean.resetStub();
//            } catch (Exception e) {
//                logger.error("Error while resetting service ", e);
//            }
//        }
//        HttpServiceLookup serviceLookup = (HttpServiceLookup)getApplicationContext().getBean("serviceLookup");
//        serviceLookup.resetRegistrar();
    }
}