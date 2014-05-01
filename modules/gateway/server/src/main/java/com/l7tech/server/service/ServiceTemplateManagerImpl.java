/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/** @author alex */
public class ServiceTemplateManagerImpl implements ServiceTemplateManager {
    private final Set<ServiceTemplate> templates = new ConcurrentSkipListSet<ServiceTemplate>();
    private final Map<String, String> autoProvisionNameMap = new ConcurrentHashMap<>();

    public Set<ServiceTemplate> findAll() {
        return Collections.unmodifiableSet(new HashSet<ServiceTemplate>(templates));
    }

    @Override
    public ServiceTemplate findByAutoProvisionName(String autoProvisionName) {
        String uri = autoProvisionNameMap.get(autoProvisionName);
        if(uri == null)
            return null;

        for(ServiceTemplate template: templates){
            if(template.getDefaultUriPrefix().equals(uri)){
                return template;
            }
        }
        return null;
    }

    public void register(ServiceTemplate template) {
        templates.add(template);
    }

    public void register(ServiceTemplate template, String autoProvisionName) {
        templates.add(template);
        autoProvisionNameMap.put(autoProvisionName,template.getDefaultUriPrefix());
    }

    public void unregister(ServiceTemplate template) {
        templates.remove(template);
    }
}