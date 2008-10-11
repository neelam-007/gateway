/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** @author alex */
public abstract class BooleanConfigurableBean extends EditableConfigurationBean<Boolean> {
    private static final Set<String> YESSES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("yes", "y", "true")));
    private static final Set<String> NOS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("no", "n", "false")));

    public BooleanConfigurableBean(String id, String shortIntro, Boolean defaultValue) {
        super(id, shortIntro, defaultValue);
    }

    @Override
    public Boolean parse(String userInput) throws ConfigurationException {
        if (YESSES.contains(userInput.toLowerCase())) return true;
        if (NOS.contains(userInput.toLowerCase())) return false;
        throw new ConfigurationException("Valid inputs are (yes, y, no, n)");
    }
}
