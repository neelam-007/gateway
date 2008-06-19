/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.service.ServiceTemplate;

import java.util.Set;

/** @author alex */
public interface ServiceTemplateManager {
    Set<ServiceTemplate> findAll();

    void register(ServiceTemplate template);

    void unregister(ServiceTemplate template);
}