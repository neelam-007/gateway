package com.l7tech.external.assertions.gatewaymanagement.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class GatewayManagementApplicationContext {


    public static synchronized ApplicationContext getAssertionApplicationContext(ApplicationContext parentSpring) {
        if (applicationContextMap.containsKey(parentSpring) ) {
            return applicationContextMap.get(parentSpring);
        }
        ApplicationContext assContext = new ClassPathXmlApplicationContext(new String[] { "com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml" }, true, parentSpring);
        applicationContextMap.put(parentSpring,assContext);
        return assContext;
    }

    private static volatile Map<ApplicationContext,ApplicationContext> applicationContextMap = new HashMap<>();

}
