package com.l7tech.server;

import com.ca.apim.gateway.extension.lifecycle.LifecycleAwareExtension;
import com.l7tech.server.extension.registry.lifecycle.LifecycleExtensionRegistry;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * Core lifecycle component responsible to manage the execution of all registered {@link LifecycleAwareExtension}s.
 */
public class LifecycleExtensionComponent implements ServerComponentLifecycle {

    private static final Logger LOGGER = Logger.getLogger(LifecycleExtensionComponent.class.getName());
    private final LifecycleExtensionRegistry lifecycleExtensionRegistry;

    public LifecycleExtensionComponent(LifecycleExtensionRegistry lifecycleExtensionRegistry){
        this.lifecycleExtensionRegistry = lifecycleExtensionRegistry;
    }

    @Override
    public void start() throws LifecycleException {
        for (LifecycleAwareExtension lifecycleAwareExtension : lifecycleExtensionRegistry.getAllExtensions()) {
            try {
                LOGGER.log(INFO, "Starting extension {0}", lifecycleAwareExtension.getName());
                lifecycleAwareExtension.start();
            } catch (com.ca.apim.gateway.extension.lifecycle.LifecycleException e) {
                throw new LifecycleException(e);
            }
        }
    }

    @Override
    public void stop() throws LifecycleException {
        Collection<com.ca.apim.gateway.extension.lifecycle.LifecycleException> exceptions = new HashSet<>();
        for (LifecycleAwareExtension lifecycleAwareExtension : lifecycleExtensionRegistry.getAllExtensions()) {
            try {
                lifecycleAwareExtension.stop();
            } catch (com.ca.apim.gateway.extension.lifecycle.LifecycleException e) {
                exceptions.add(e);
            }
        }
        if(!exceptions.isEmpty()) {
            throw new LifecycleException("Multiple Lifecycle Exceptions:" + buildExceptionMessage(exceptions));
        }
    }

    private String buildExceptionMessage(Collection<com.ca.apim.gateway.extension.lifecycle.LifecycleException> exceptions) {
        StringBuilder message = new StringBuilder();
        for (com.ca.apim.gateway.extension.lifecycle.LifecycleException e : exceptions) {
            message.append("\n").append(ExceptionUtils.getMessage(e));
        }
        return message.toString();
    }

    @Override
    public void close() throws LifecycleException {
        stop();
    }

    @Override
    public String toString() {
        return "LifecycleExtensionComponent";
    }
}
