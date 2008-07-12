package com.l7tech.server.ems;

import com.noelios.restlet.application.ApplicationContext;
import com.noelios.restlet.ext.servlet.ServerServlet;
import org.restlet.Application;
import org.restlet.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A servlet that splices a Restlet Application into the URL space mapped to the servlet.
 */
public class EmsRestServlet extends ServerServlet {
    private transient Component restComponent;
    private transient Application restApplication;

    @Override
    public Application getApplication() {
        if (restApplication == null) {
            synchronized (this) {
                WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
                Application app = (Application)springContext.getBean("restApplication", Application.class);

                ApplicationContext applicationContext = (ApplicationContext)app.getContext();
                applicationContext.setWarClient(createWarClient(applicationContext, getServletConfig()));

                this.restApplication = app;
            }
        }
        return restApplication;
    }

    @Override
    public Component getComponent() {
        if (restComponent == null) {
            synchronized (this) {
                WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
                restComponent = (Component)applicationContext.getBean("restComponent", Component.class);
            }
        }
        return restComponent;
    }
}
