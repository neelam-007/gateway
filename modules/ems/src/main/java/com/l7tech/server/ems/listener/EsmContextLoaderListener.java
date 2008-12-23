package com.l7tech.server.ems.listener;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;

/**
 * Sets up the Spring sub-context for a servlet, ensuring that it is parented by the owning EmsServletContainer's
 * ApplicationContext.
 */
public class EsmContextLoaderListener extends ContextLoaderListener {
    @Override
    protected ContextLoader createContextLoader() {
        return new ContextLoader() {
            @Override
            protected ApplicationContext loadParentContext(ServletContext servletContext) throws BeansException {
                String instanceId = servletContext.getInitParameter(EsmServletContainer.INIT_PARAM_INSTANCE_ID);
                EsmServletContainer module = EsmServletContainer.getInstance(Long.parseLong(instanceId));
                return module.getApplicationContext();
            }
        };
    }
}

