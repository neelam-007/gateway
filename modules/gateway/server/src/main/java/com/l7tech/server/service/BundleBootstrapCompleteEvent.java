package com.l7tech.server.service;

import org.springframework.context.ApplicationEvent;

/**
 * Signals that the bootstrap bundles have been loaded.
 */
public class BundleBootstrapCompleteEvent extends ApplicationEvent {
    public BundleBootstrapCompleteEvent(Object source) {
        super(source);
    }
}
