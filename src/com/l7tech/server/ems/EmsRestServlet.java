package com.l7tech.server.ems;

import com.noelios.restlet.ext.servlet.ServerServlet;
import com.noelios.restlet.application.ApplicationContext;
import org.restlet.Application;
import org.restlet.Component;

import javax.servlet.ServletException;

/**
 * A servlet that splices a Restlet Application into the URL space mapped to the servlet.
 */
public class EmsRestServlet extends ServerServlet {
    private final Component restComponent;
    private final Application restApplication;

    public EmsRestServlet(Component restComponent, Application restApplication) {
        this.restComponent = restComponent;
        this.restApplication = restApplication;
    }

    @Override
    public void init() throws ServletException {
        // Wire up WarClient
        ApplicationContext applicationContext = (ApplicationContext)getApplication().getContext();
        applicationContext.setWarClient(createWarClient(applicationContext, getServletConfig()));

        super.init();
    }

    @Override
    public Application getApplication() {
        return restApplication;
    }

    @Override
    public Component getComponent() {
        return restComponent;
    }
}
