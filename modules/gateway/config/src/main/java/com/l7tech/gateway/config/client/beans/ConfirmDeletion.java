/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.beans.ConfigurationBean;

/** @author alex */
public class ConfirmDeletion extends BooleanConfigurableBean {
    private final ConfigurationBean victim;

    public ConfirmDeletion(ConfigurationBean victim) {
        super("_delete." + victim.getId(), "Confirm delete " + victim.getConfigName() + " " + victim.getShortValueDescription(), false);
        this.victim = victim;
    }

    @Override
    public ConfigResult onConfiguration(Boolean value, ConfigurationContext context) {
        if (value) {
            context.removeBean(victim);
            return ConfigResult.pop();
        } else {
            return ConfigResult.stay();
        }
    }
}
