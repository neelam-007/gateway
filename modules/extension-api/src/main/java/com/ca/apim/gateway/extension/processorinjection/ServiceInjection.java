package com.ca.apim.gateway.extension.processorinjection;

import com.ca.apim.gateway.extension.Extension;

/**
 * The Service Injection Extension allow injection into the policy execution context at different stages of executing
 * a service. For example, you could create an extension to inject context variable into the policy execution context before the policy is executed.
 */
public interface ServiceInjection extends Extension {
    /**
     * Allows modification of the execution context of a service.
     *
     * @param context The context for the service that is/was executed.
     * @return in pre-service injections return false in order to stop service processing. In post-service injections return false in order to falsify service processing
     */
    boolean execute(ServiceInjectionContext context);
}
