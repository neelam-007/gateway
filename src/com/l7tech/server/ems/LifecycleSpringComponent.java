package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;
import org.restlet.ext.spring.SpringComponent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A SpringComponent that starts itself when the application context is first refreshed, and stops itself
 * when the application context is destroyed.
 */
public class LifecycleSpringComponent extends SpringComponent implements ApplicationContextAware, ApplicationListener, DisposableBean {
    private static final Logger logger = Logger.getLogger(LifecycleSpringComponent.class.getName());
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void destroy() throws Exception {
        stop();
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ContextRefreshedEvent evt = (ContextRefreshedEvent) event;
            if (evt.getApplicationContext().equals(applicationContext)) {
                try {
                    start();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unable to start context: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }
}
