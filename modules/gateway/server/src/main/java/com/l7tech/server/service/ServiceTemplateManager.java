/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/** @author alex */
public interface ServiceTemplateManager {
    Set<ServiceTemplate> findAll();
    ServiceTemplate findByAutoProvisionName(String autoProvisionName);

    void register(ServiceTemplate template);
    void register(ServiceTemplate template,  @NotNull String autoProvisionName);

    void unregister(ServiceTemplate template);
}