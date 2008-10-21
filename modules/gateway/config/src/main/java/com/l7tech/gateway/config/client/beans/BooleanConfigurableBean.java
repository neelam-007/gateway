/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.options.OptionType;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.text.ParseException;

/** @author alex */
public abstract class BooleanConfigurableBean extends EditableConfigurationBean<Boolean> {
    private static final Logger logger = Logger.getLogger(BooleanConfigurableBean.class.getName());

    public BooleanConfigurableBean(String id, String shortIntro, Boolean defaultValue) {
        super(id, shortIntro, defaultValue);
    }

    @Override
    public Boolean parse(String userInput) throws ConfigurationException {
        Pattern booleanPattern = Pattern.compile(OptionType.BOOLEAN.getDefaultRegex());
        if ( booleanPattern.matcher(userInput).matches() ) {
            try {
                return (Boolean) OptionType.BOOLEAN.getFormat().parseObject(userInput);
            } catch ( ParseException pe ) {
                logger.log(Level.WARNING, "Error parsing option.", pe);
            }
        }

        throw new ConfigurationException("Valid inputs are (true/false)");
    }
}
