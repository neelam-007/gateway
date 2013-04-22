/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */

package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to a preconfigured connection to a JMS provider.
 *
 * Persistent.
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="jms_connection")
public class JmsConnection extends ZoneableNamedEntityImp implements Serializable {
    private static final Logger logger = Logger.getLogger(JmsConnection.class.getName());
    private static final Charset ENCODING = Charsets.UTF8;

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
    public static final String PROP_JNDI_USE_CLIENT_AUTH = PREFIX + ".jndi.useClientAuth";
    public static final String PROP_JNDI_SSG_KEYSTORE_ID = PREFIX + ".jndi.ssgKeystoreId";
    public static final String PROP_JNDI_SSG_KEY_ALIAS = PREFIX + ".jndi.ssgKeyAlias";
    public static final String PROP_QUEUE_USE_CLIENT_AUTH = PREFIX + ".queue.useClientAuth";
    public static final String PROP_QUEUE_SSG_KEYSTORE_ID = PREFIX + ".queue.ssgKeystoreId";
    public static final String PROP_QUEUE_SSG_KEY_ALIAS = PREFIX + ".queue.ssgKeyAlias";
    public static final String PROP_CUSTOMIZER = PREFIX + ".customizer.class";
    public static final String PROP_IS_HARDWIRED_SERVICE = PREFIX + ".hardwired.service.bool";
    public static final String PROP_HARDWIRED_SERVICE_ID = PREFIX + ".hardwired.service.id";
    public static final String PROP_CONTENT_TYPE_SOURCE = PREFIX + ".contentType.source";
    public static final String PROP_CONTENT_TYPE_VAL = PREFIX + ".contentType.value";
    public static final String CONTENT_TYPE_SOURCE_FREEFORM = PREFIX + ".contentType.freeform";
    public static final String CONTENT_TYPE_SOURCE_HEADER = PREFIX + ".contentType.header";


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
    private boolean _template;
    private String _properties;
    private JmsProviderType _providerType;
    @Transient private Properties cachedProperties;


    public JmsConnection(){
    }

    public JmsConnection( final JmsConnection jmsConnection, final boolean readOnly ){
        copyFrom( jmsConnection );
        if (readOnly) lock();
    }

    public void copyFrom( JmsConnection other ) {
        setOid( other.getOid() );
        setVersion( other.getVersion() );
        setName( other.getName() );
        setTemplate( other.isTemplate() );
        setInitialContextFactoryClassname( other.getInitialContextFactoryClassname() );
        setJndiUrl( other.getJndiUrl() );
        setQueueFactoryUrl( other.getQueueFactoryUrl() );
        setTopicFactoryUrl( other.getTopicFactoryUrl() );
        setDestinationFactoryUrl( other.getDestinationFactoryUrl() );
        setUsername( other.getUsername() );
        setPassword( other.getPassword() );
        setProperties( other.getProperties() );
        setCachedProperties( other.properties() );
        setProviderType( other.getProviderType() );
        setSecurityZone(other.getSecurityZone());
    }

    @Override
    public String toString() {
        return "<JmsConnection oid=\"" + _oid + "\" name=\"" + _name + "\"/>";
    }

    @Size(min=1,max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    @Size(max=255)
    @Column(name="username", length=255)
    public String getUsername() {
        return _username;
    }

    public void setUsername( String username ) {
        checkLocked();
        _username = username;
    }

    @Size(max=255)
    @Column(name="password", length=255)
    @WspSensitive
    public String getPassword() {
        return _password;
    }

    public void setPassword( String password ) {
        checkLocked();
        _password = password;
    }

    @NotNull(groups=StandardValidationGroup.class)
    @Size(min=1,max=255)
    @Column(name="factory_classname", length=255)
    public String getInitialContextFactoryClassname() {
        return _initialContextFactoryClassname;
    }

    public void setInitialContextFactoryClassname( String initialContextFactoryClassname ) {
        checkLocked();
        _initialContextFactoryClassname = initialContextFactoryClassname;
    }

    @NotNull(groups=StandardValidationGroup.class)
    @Size(min=1,max=255)
    @Column(name="jndi_url", length=255)
    public String getJndiUrl() {
        return _jndiUrl;
    }

    public void setJndiUrl(String jndiUrl) {
        checkLocked();
        _jndiUrl = jndiUrl;
    }

    @Size(min=1,max=255)
    @Column(name="queue_factory_url", length=255)
    public String getQueueFactoryUrl() {
        return _queueFactoryUrl;
    }

    public void setQueueFactoryUrl(String queueFactoryUrl) {
        checkLocked();
        _queueFactoryUrl = queueFactoryUrl;
    }

    @Size(min=1,max=255)
    @Column(name="topic_factory_url", length=255)
    public String getTopicFactoryUrl() {
        return _topicFactoryUrl;
    }

    public void setTopicFactoryUrl(String topicFactoryUrl) {
        checkLocked();
        _topicFactoryUrl = topicFactoryUrl;
    }

    @Size(min=1,max=255)
    @Column(name="destination_factory_url", length=255)
    public String getDestinationFactoryUrl() {
        return _destinationFactoryUrl;
    }

    public void setDestinationFactoryUrl( String destinationFactoryUrl ) {
        checkLocked();
        _destinationFactoryUrl = destinationFactoryUrl;
    }

    @Column(name="is_template")
    public boolean isTemplate() {
        return _template;
    }

    public void setTemplate(final boolean template) {
        checkLocked();
        _template = template;
    }

    public Properties properties() {
        if (cachedProperties != null) {
            return cachedProperties;
        }

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

        cachedProperties = properties;
        return cachedProperties;
    }

    public void properties(Properties properties) {
        if (properties == null) {
            setProperties("");
        } else {
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
            try {
                properties.storeToXML(baos, null, ENCODING.name());
                setProperties(baos.toString(ENCODING));
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Error saving properties", e);
            } finally {
                baos.close();
            }
        }
    }

    @Size(max=131072)
    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    public String getProperties() {
        return _properties;
    }

    public void setProperties(String properties) {
        checkLocked();
        _properties = properties;
        cachedProperties = null;
    }

    @Column(name="provider_type")
    @Enumerated(EnumType.STRING)
    public JmsProviderType getProviderType() {
        return _providerType;
    }

    public void setProviderType(JmsProviderType providerType) {
        checkLocked();
        this._providerType = providerType;
    }

    @Transient
    private void setCachedProperties(Properties cachedProperties) {
        this.cachedProperties = cachedProperties;
    }

    /**
     * Standard validation group with additional constraints for non-templates.
     */
    public interface StandardValidationGroup {}    
    
}
