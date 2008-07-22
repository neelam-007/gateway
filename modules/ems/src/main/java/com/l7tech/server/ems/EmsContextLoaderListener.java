package com.l7tech.server.ems;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;

/**
 * Sets up the Spring sub-context for a servlet, ensuring that it is parented by the owning EmsServletContainer's
 * ApplicationContext.
 */
public class EmsContextLoaderListener extends ContextLoaderListener {
    protected ContextLoader createContextLoader() {
        return new ContextLoader() {
            protected ApplicationContext loadParentContext(ServletContext servletContext) throws BeansException {
                String instanceId = servletContext.getInitParameter(EmsServletContainer.INIT_PARAM_INSTANCE_ID);
                EmsServletContainer module = EmsServletContainer.getInstance(Long.parseLong(instanceId));
                return module.getApplicationContext();
            }
        };
    }
}

