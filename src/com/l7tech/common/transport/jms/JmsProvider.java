/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import java.io.Serializable;

/**
 * A reference to a preconfigured JMS provider.
 *
 * Persistent.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsProvider implements Serializable {
    private String _name;
    private String _initialContextFactoryClassname;
    private String _defaultQueueFactoryUrl;
    private String _defaultTopicFactoryUrl;
    private String _defaultDestinationFactoryUrl;

    public JmsProvider() {
    }

    public JmsProvider(String name, String initialContextFactoryClassname, String defaultQueueFactoryUrl) {
        _name = name;
        _initialContextFactoryClassname = initialContextFactoryClassname;
        _defaultQueueFactoryUrl = defaultQueueFactoryUrl;
    }

    /** Get the human-readable name of this JmsProvider, as displayed in the GUI. */
    public String getName() {
        return _name;
    }

    /** Set the human-readable name of this JmsProvider, as displayed in the GUI. */
    public void setName(String name) {
        _name = name;
    }

    public String getInitialContextFactoryClassname() {
        return _initialContextFactoryClassname;
    }

    public void setInitialContextFactoryClassname( String initialContextFactoryClassname ) {
        _initialContextFactoryClassname = initialContextFactoryClassname;
    }

    public String getDefaultQueueFactoryUrl() {
        return _defaultQueueFactoryUrl;
    }

    public void setDefaultQueueFactoryUrl(String queueFactoryUrl) {
        _defaultQueueFactoryUrl = queueFactoryUrl;
    }

    public String getDefaultTopicFactoryUrl() {
        return _defaultTopicFactoryUrl;
    }

    public void setDefaultTopicFactoryUrl(String topicFactoryUrl) {
        _defaultTopicFactoryUrl = topicFactoryUrl;
    }

    public String getDefaultDestinationFactoryUrl() {
        return _defaultDestinationFactoryUrl;
    }

    public void setDefaultDestinationFactoryUrl( String destinationFactoryUrl ) {
        _defaultDestinationFactoryUrl = destinationFactoryUrl;
    }
}
