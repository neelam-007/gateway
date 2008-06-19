/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.service.ServiceTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/** @author alex */
public class ServiceTemplateManagerImpl implements ServiceTemplateManager {
    private final Set<ServiceTemplate> templates = new ConcurrentSkipListSet<ServiceTemplate>();

    public Set<ServiceTemplate> findAll() {
        return Collections.unmodifiableSet(new HashSet<ServiceTemplate>(templates));
    }

    public void register(ServiceTemplate template) {
        templates.add(template);
    }

    public void unregister(ServiceTemplate template) {
        templates.remove(template);
    }
}