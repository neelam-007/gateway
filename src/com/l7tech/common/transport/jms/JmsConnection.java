/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */

package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to a preconfigured connection to a JMS provider.
 *
 * Persistent.
 */
public class JmsConnection extends NamedEntityImp implements Serializable {
    private static final Logger logger = Logger.getLogger(JmsConnection.class.getName());
    private static final String ENCODING = "UTF-8";

    // Constants used in property mapping/substitution.
    //
    // Constants starting with VALUE_ are used as stand-in strings saved as
    // property values during configuration. And then during connection, they
    // are replaced by their corresponding objects. Most stand-in values
    // contains just a single string token. But some, e.g. VALUE_KEYSTORE,
    // VALUE_KEYSTORE_BYTES and VALUE_KEYSTORE_PASSWORD, have additional
    // parameter tokens separated by tab characters.
    //
    // IMPORTANT: For backward compatibility, never change these string values.
    public static final String PREFIX = "com.l7tech.server.jms.prop";
    public static final String VALUE_KEYSTORE = PREFIX + ".keystore";
    public static final String VALUE_KEYSTORE_BYTES = PREFIX + ".keystore.bytes";
    public static final String VALUE_KEYSTORE_PASSWORD = PREFIX + ".keystore.password";
    public static final String VALUE_TRUSTED_LIST = PREFIX + ".trustedcert.listx509"; // actually a Vector
    public static final String VALUE_BOOLEAN_TRUE = PREFIX + ".boolean.true";
    public static final String VALUE_BOOLEAN_FALSE = PREFIX + ".boolean.false";
    public static final String PROP_KEYSTORE_ALIAS = PREFIX + ".keystore.alias";
    public static final String PROP_JNDI_SSG_KEYSTORE_ID = PREFIX + ".jndi.ssgKeystoreId";
    public static final String PROP_JNDI_SSG_KEY_ALIAS = PREFIX + ".jndi.ssgKeyAlias";
    public static final String PROP_QUEUE_USE_CLIENT_AUTH = PREFIX + ".queue.useClientAuth";
    public static final String PROP_QUEUE_SSG_KEYSTORE_ID = PREFIX + ".queue.ssgKeystoreId";
    public static final String PROP_QUEUE_SSG_KEY_ALIAS = PREFIX + ".queue.ssgKeyAlias";
    public static final String PROP_CUSTOMIZER = PREFIX + ".customizer.class";
    public static final String PROP_IS_HARDWIRED_SERVICE = PREFIX + ".hardwired.service.bool";
    public static final String PROP_HARDWIRED_SERVICE_ID = PREFIX + ".hardwired.service.id";

    /** Name of String property (in returned value of {@link #properties()}) that
        contains the name of JMS message property to be used as SOAPAction value
        during service resolution. */
    public static final String JMS_MSG_PROP_WITH_SOAPACTION = "com.l7tech.server.jms.soapAction.msgPropName";

    private String _initialContextFactoryClassname;
    private String _jndiUrl;
    private String _queueFactoryUrl;
    private String _topicFactoryUrl;
    private String _destinationFactoryUrl;
    private String _username;
    private String _password;
    private String _properties;

    public void copyFrom( JmsConnection other ) {
        setOid( other.getOid() );
        setVersion( other.getVersion() );
        setName( other.getName() );
        setInitialContextFactoryClassname( other.getInitialContextFactoryClassname() );
        setJndiUrl( other.getJndiUrl() );
        setQueueFactoryUrl( other.getQueueFactoryUrl() );
        setTopicFactoryUrl( other.getTopicFactoryUrl() );
        setDestinationFactoryUrl( other.getDestinationFactoryUrl() );
        setUsername( other.getUsername() );
        setPassword( other.getPassword() );
        setProperties( other.getProperties() );
    }

    public String toString() {
        return "<JmsConnection oid=\"" + _oid + "\" name=\"" + _name + "\"/>";
    }

    public EntityHeader toEntityHeader() {
        return new EntityHeader(Long.toString(getOid()), EntityType.JMS_CONNECTION, getName(), null);        
    }

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

    public Properties properties() {
        Properties properties = new Properties();
        try {
            String propertiesStr = getProperties();
            if (propertiesStr != null && propertiesStr.trim().length()>0) {
                properties.loadFromXML(new ByteArrayInputStream(propertiesStr.getBytes(ENCODING)));
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error loading properties", e);
        }
        return properties;
    }

    public void properties(Properties properties) {
        if (properties == null) {
            setProperties("");
        } else {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                properties.storeToXML(baos, null, ENCODING);
                setProperties(baos.toString(ENCODING));
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Error saving properties", e);
            }
        }
    }

    public String getProperties() {
        return _properties;
    }

    public void setProperties(String properties) {
        _properties = properties;
    }
}
