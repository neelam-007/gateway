/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.util.ExceptionUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Configurable whose value is a URL.  Ensures that user-provided URLs are syntactically valid and conform to the list
 * of permitted protocols.
 *
 * @author alex
 */
public abstract class UrlConfigurableBean extends EditableConfigurationBean<URL> {
    private Set<String> protocols;

    protected UrlConfigurableBean(String id, String shortIntro, URL defaultValue, String... protocols) {
        super(id, shortIntro, defaultValue);
        Set<String> tempProtos = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        tempProtos.addAll(Arrays.asList(protocols));
        this.protocols = tempProtos;
    }

    protected String processUserInput( final String userInput ){
        return userInput;
    }

    @Override
    public final URL parse(String userInput) throws ConfigurationException {
        try {
            URL url = new URL(processUserInput(userInput));
            if (!protocols.isEmpty() && !protocols.contains(url.getProtocol())) {
                throw new ConfigurationException("Unsupported protocol (accepts " + protocols.toString() + ")");
            }

            if ( url.getHost() == null || url.getHost().length()==0 ) {
                throw new ConfigurationException("No hostname in URL");
            }

            return url;
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Malformed URL: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
