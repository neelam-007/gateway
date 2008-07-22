package com.l7tech.server.transport.http;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;

/**
 * A ContextLoaderListener that will ensure that the Gateway's ApplicationContext is hooked up
 * as the parent of the WebApplicationContext.
 */
public class SsgContextLoaderListener extends ContextLoaderListener {
    protected ContextLoader createContextLoader() {
        return new ContextLoader() {
            protected ApplicationContext loadParentContext(ServletContext servletContext) throws BeansException {
                String instanceId = servletContext.getInitParameter(HttpTransportModule.INIT_PARAM_INSTANCE_ID);
                HttpTransportModule module = HttpTransportModule.getInstance(Long.parseLong(instanceId));
                return module.getApplicationContext();
            }
        };
    }
}
