/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */

package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependencies;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import org.hibernate.annotations.Proxy;

import javax.naming.Context;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
public class JmsConnection extends ZoneableNamedEntityImp implements UsesPrivateKeys, Serializable {
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

    public static final String PROP_IS_DEDICATED_CONSUMER = PREFIX + ".dedicated.consumer.bool";
    public static final String PROP_DEDICATED_CONSUMER_SIZE = PREFIX + ".dedicated.consumer.size";

    public static final String PROP_SESSION_POOL_SIZE = PREFIX + ".session.pool.size";
    public static final String PROP_MAX_SESSION_IDLE = PREFIX + ".max.session.idle";
    public static final String PROP_SESSION_POOL_MAX_WAIT = PREFIX + ".session.pool.max.wait";
    public static final int DEFAULT_SESSION_POOL_SIZE = 8;
    public static final long DEFAULT_SESSION_POOL_MAX_WAIT = 5000;
    public static final String PROP_CONNECTION_POOL_ENABLE = PREFIX + ".connection.pool.enable";
    public static final String PROP_CONNECTION_POOL_SIZE = PREFIX + ".connection.pool.size";
    public static final String PROP_CONNECTION_MIN_IDLE = PREFIX + ".connection.min.idle";
    public static final String PROP_CONNECTION_POOL_MAX_WAIT = PREFIX + ".connection.pool.max.wait";
    public static final String PROP_CONNECTION_EVICTABLE_TIME = PREFIX + ".connection.max.age";
    public static final String PROP_CONNECTION_IDLE_TIMEOUT = PREFIX + ".connection.pool.idle.timeout";
    public static final String PROP_CONNECTION_POOL_EVICT_INTERVAL = PREFIX + ".connection.pool.evict.interval";
    public static final String PROP_CONNECTION_POOL_EVICT_BATCH_SIZE = PREFIX + ".connection.pool.evict.batch.size";
    public static final int DEFAULT_CONNECTION_POOL_SIZE = 1;
    public static final int DEFAULT_CONNECTION_POOL_MIN_IDLE = 0;
    public static final long DEFAULT_CONNECTION_POOL_MAX_WAIT = 5000;
    public static final long DEFAULT_CONNECTION_POOL_EVICT_INTERVAL = 10000;
    public static final long DEFAULT_CONNECTION_MAX_AGE = 300000;

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
        setGoid(other.getGoid());
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
        return "<JmsConnection goid=\"" + getGoid() + "\" name=\"" + _name + "\"/>";
    }

    @RbacAttribute
    @Size(min=1,max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    @RbacAttribute
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
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.VARIABLE)
    public String getPassword() {
        return _password;
    }

    public void setPassword( String password ) {
        checkLocked();
        _password = password;
    }

    @RbacAttribute
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

    @RbacAttribute
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

    @RbacAttribute
    @Size(min=1,max=255)
    @Column(name="queue_factory_url", length=255)
    public String getQueueFactoryUrl() {
        return _queueFactoryUrl;
    }

    public void setQueueFactoryUrl(String queueFactoryUrl) {
        checkLocked();
        _queueFactoryUrl = queueFactoryUrl;
    }

    @RbacAttribute
    @Size(min=1,max=255)
    @Column(name="topic_factory_url", length=255)
    public String getTopicFactoryUrl() {
        return _topicFactoryUrl;
    }

    public void setTopicFactoryUrl(String topicFactoryUrl) {
        checkLocked();
        _topicFactoryUrl = topicFactoryUrl;
    }

    @RbacAttribute
    @Size(min=1,max=255)
    @Column(name="destination_factory_url", length=255)
    public String getDestinationFactoryUrl() {
        return _destinationFactoryUrl;
    }

    public void setDestinationFactoryUrl( String destinationFactoryUrl ) {
        checkLocked();
        _destinationFactoryUrl = destinationFactoryUrl;
    }

    @RbacAttribute(displayNameIdentifier = "jms.template")
    @Column(name="is_template")
    public boolean isTemplate() {
        return _template;
    }

    public void setTemplate(final boolean template) {
        checkLocked();
        _template = template;
    }

    @Dependencies({
            @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SERVICE, key = PROP_HARDWIRED_SERVICE_ID),
            @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD, key = Context.SECURITY_CREDENTIALS)
    })
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

    @RbacAttribute
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
     * This is used by the dependencyAnalyzer in order to find private key dependencies.
     * @return The private keys used by thing jms connection
     */
    @Override
    @Transient
    public SsgKeyHeader[] getPrivateKeysUsed() {
        ArrayList<SsgKeyHeader> headers = new ArrayList<>();
        if (TibcoEmsConstants.SSL.equals(properties().getProperty(TibcoEmsConstants.TibjmsContext.SECURITY_PROTOCOL)) && properties().getProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID) != null) {
            String alias = properties().getProperty(JmsConnection.PROP_JNDI_SSG_KEY_ALIAS);
            String keyStoreId = properties().getProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID);
            headers.add(new SsgKeyHeader(keyStoreId + ":" + alias, GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyStoreId), alias, alias));
        }
        if (("com.l7tech.server.transport.jms.prov.MQSeriesCustomizer".equals(properties().getProperty(JmsConnection.PROP_CUSTOMIZER)) || "com.l7tech.server.transport.jms.prov.TibcoConnectionFactoryCustomizer".equals(properties().getProperty(JmsConnection.PROP_CUSTOMIZER))) && properties().getProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID) != null) {
            String alias = properties().getProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS);
            String keyStoreId = properties().getProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID);
            headers.add(new SsgKeyHeader(keyStoreId + ":" + alias, GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyStoreId), alias, alias));
        }
        return headers.toArray(new SsgKeyHeader[headers.size()]);
    }

    @Override
    public void replacePrivateKeyUsed(@org.jetbrains.annotations.NotNull final SsgKeyHeader oldSSGKeyHeader, @org.jetbrains.annotations.NotNull final SsgKeyHeader newSSGKeyHeader) {
        final Properties properties = properties();
        if (TibcoEmsConstants.SSL.equals(properties.getProperty(TibcoEmsConstants.TibjmsContext.SECURITY_PROTOCOL)) && properties.getProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID) != null) {
            String alias = properties.getProperty(JmsConnection.PROP_JNDI_SSG_KEY_ALIAS);
            String keyStoreId = properties.getProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID);
            if(Goid.equals(GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyStoreId), oldSSGKeyHeader.getKeystoreId()) && alias.equals(oldSSGKeyHeader.getAlias())){
                properties.setProperty(JmsConnection.PROP_JNDI_SSG_KEY_ALIAS, newSSGKeyHeader.getAlias());
                properties.setProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID, newSSGKeyHeader.getKeystoreId().toString());
            }
        }
        if (("com.l7tech.server.transport.jms.prov.MQSeriesCustomizer".equals(properties.getProperty(JmsConnection.PROP_CUSTOMIZER)) || "com.l7tech.server.transport.jms.prov.TibcoConnectionFactoryCustomizer".equals(properties.getProperty(JmsConnection.PROP_CUSTOMIZER))) && properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID) != null) {
            String alias = properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS);
            String keyStoreId = properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID);
            if(Goid.equals(GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyStoreId), oldSSGKeyHeader.getKeystoreId()) && alias.equals(oldSSGKeyHeader.getAlias())){
                properties.setProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS, newSSGKeyHeader.getAlias());
                properties.setProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID, newSSGKeyHeader.getKeystoreId().toString());
            }
        }
        properties(properties);
    }

    /**
     * Standard validation group with additional constraints for non-templates.
     */
    public interface StandardValidationGroup {}

}
