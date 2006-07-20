/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.module.ca_wsdm;

import com.l7tech.server.event.RoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.event.PostRoutingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 */
public class CaWsdmRoutingListener implements ApplicationListener, InitializingBean, DisposableBean {
    private static final Logger logger = Logger.getLogger(CaWsdmRoutingListener.class.getName());

    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof RoutingEvent)) return;

        if (event instanceof PreRoutingEvent) {
            PreRoutingEvent preRoutingEvent = (PreRoutingEvent) event;
            logger.log(Level.INFO, "About to route to {0}", preRoutingEvent.getUrl());
        } else if (event instanceof PostRoutingEvent) {
            PostRoutingEvent postRoutingEvent = (PostRoutingEvent) event;
            logger.log(Level.INFO, "Routed message to {0}, got status {1}", new Object[] { postRoutingEvent.getUrl(), postRoutingEvent.getHttpResponseStatus() });
        }
    }

    public void afterPropertiesSet() throws Exception {
        logger.log(Level.INFO, getClass().getName() + " starting");
    }

    public void destroy() throws Exception {
        logger.log(Level.INFO, getClass().getName() + " stopping");
    }
}
