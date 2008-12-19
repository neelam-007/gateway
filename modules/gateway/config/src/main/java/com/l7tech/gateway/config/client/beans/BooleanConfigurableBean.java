/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.options.OptionType;

/** @author alex */
public abstract class BooleanConfigurableBean extends TypedConfigurableBean<Boolean> {
    public BooleanConfigurableBean(String id, String shortIntro, Boolean defaultValue) {
        super(id, shortIntro, "Invalid value, please try again (Yes/No)", defaultValue, null, OptionType.BOOLEAN);
    }
}
