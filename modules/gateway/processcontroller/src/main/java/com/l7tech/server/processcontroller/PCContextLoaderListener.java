/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.ContextLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import javax.servlet.ServletContext;

/** @author alex */
public class PCContextLoaderListener extends ContextLoaderListener {
    protected ContextLoader createContextLoader() {
        return new ContextLoader() {
            protected ApplicationContext loadParentContext(ServletContext servletContext) throws BeansException {
                String instanceId = servletContext.getInitParameter(PCServletContainer.INIT_PARAM_INSTANCE_ID);
                PCServletContainer module = PCServletContainer.getInstance(Long.parseLong(instanceId));
                return module.getApplicationContext();
            }
        };
    }
}
