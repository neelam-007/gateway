/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.io.Serializable;
import java.util.Set;

/**
 * A reference to a preconfigured JMS provider.
 *
 * Persistent.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsProvider extends NamedEntityImp implements Serializable {
    private String _initialContextFactoryClassname;
    private String _jndiUrl;
    private String _queueFactoryUrl;
    private String _topicFactoryUrl;
    private String _destinationFactoryUrl;
    private String _username;
    private String _password;
    private Set _destinations;

    public String getUsername() {
        return _username;
    }

    public void setUsername( String username ) {
        _username = username;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword( String password ) {
        _password = password;
    }

    public String getInitialContextFactoryClassname() {
        return _initialContextFactoryClassname;
    }

    public void setInitialContextFactoryClassname( String initialContextFactoryClassname ) {
        _initialContextFactoryClassname = initialContextFactoryClassname;
    }

    public String getJndiUrl() {
        return _jndiUrl;
    }

    public void setJndiUrl(String jndiUrl) {
        _jndiUrl = jndiUrl;
    }

    public String getQueueFactoryUrl() {
        return _queueFactoryUrl;
    }

    public void setQueueFactoryUrl(String queueFactoryUrl) {
        _queueFactoryUrl = queueFactoryUrl;
    }

    public String getTopicFactoryUrl() {
        return _topicFactoryUrl;
    }

    public void setTopicFactoryUrl(String topicFactoryUrl) {
        _topicFactoryUrl = topicFactoryUrl;
    }

    public String getDestinationFactoryUrl() {
        return _destinationFactoryUrl;
    }

    public void setDestinationFactoryUrl( String destinationFactoryUrl ) {
        _destinationFactoryUrl = destinationFactoryUrl;
    }

    public Set getDestinations() {
        return _destinations;
    }

    public void setDestinations( Set destinations ) {
        _destinations = destinations;
    }
}
