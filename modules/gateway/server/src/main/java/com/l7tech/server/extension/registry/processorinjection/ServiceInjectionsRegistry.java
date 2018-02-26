package com.l7tech.server.extension.registry.processorinjection;

import com.ca.apim.gateway.extension.processorinjection.ServiceInjection;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;

import java.util.logging.Logger;

/**
 * A Service Injection Registry holds Service Injections that can be run either before or after service processing.
 * <p>
 * For example: To execute a service injection that adds a context variable before all services tagged with `virtualize` add the following code to you onModuleLoad:
 * <pre>
 *  preServiceInvocationInjectionsRegistry = context.getBean("preServiceInvocationInjectionsRegistry", ServiceInjectionsRegistry.class);
 *  if (preServiceInvocationInjectionsRegistry != null) {
 *      preServiceInvocationInjectionsRegistry.register(
 *      SERVICE_VISUALIZATION_INJECTION_KEY,
 *      serviceInjectionContext -> serviceInjectionContext.setVariable("virtual.url", "virtual-url.com"),
 *      "virtualize");
 *  }
 * </pre>
 */
public class ServiceInjectionsRegistry extends AbstractRegistryImpl<ServiceInjection> {
    private static final Logger LOGGER = Logger.getLogger(ServiceInjectionsRegistry.class.getName());

    // Use this tag to register a Service injection that will be triggered for all service executions
    public static final String GLOBAL_TAG = "*";

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
