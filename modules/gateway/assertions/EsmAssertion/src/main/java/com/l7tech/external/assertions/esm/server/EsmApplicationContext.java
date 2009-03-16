/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.esm.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.l7tech.server.wsdm.ServiceManagementAdministrationService;

/** @author alex */
public final class EsmApplicationContext {
    private final ClassPathXmlApplicationContext spring;
    private final ServiceManagementAdministrationService wsdmService;

    private EsmApplicationContext(ApplicationContext parentSpring) {
        ClassLoader old = null;
        try {
            old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            spring = new ClassPathXmlApplicationContext(new String[] { "com/l7tech/external/assertions/esm/server/resources/esmAssertionContext.xml" }, true, parentSpring);
            wsdmService = (ServiceManagementAdministrationService)spring.getBean("wsdmService");
        } finally {
            if (old != null) Thread.currentThread().setContextClassLoader(old);
        }

    }

    public static EsmApplicationContext getInstance(ApplicationContext parentSpring) {
        if (INSTANCE == null) {
            INSTANCE = new EsmApplicationContext(parentSpring);
        }
        return INSTANCE;
    }

    public ApplicationContext getApplicationContext() {
        return spring;
    }

    public ServiceManagementAdministrationService getEsmService() {
        return wsdmService;
    }

    private static volatile EsmApplicationContext INSTANCE;
}
