package com.l7tech.gateway.common.transport.jms;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jbufu
 */
public enum JmsProviderType {
    // - PUBLIC
    Tibco("TIBCO EMS", "com.tibco.tibjms.naming.TibjmsInitialContextFactory", "QueueConnectionFactory", null, null, "com.l7tech.console.panels.TibcoEmsJndiExtraPropertiesPanel", "com.l7tech.console.panels.TibcoEmsQueueExtraPropertiesPanel"),
    MQ("WebSphere MQ over LDAP", "com.sun.jndi.ldap.LdapCtxFactory", "QueueConnectionFactory", null, null, null, "com.l7tech.console.panels.MQSeriesQueueExtraPropertiesPanel"),
    Fiorano("FioranoMQ", "fiorano.jms.runtime.naming.FioranoInitialContextFactory", "QueueConnectionFactory", null, null, "com.l7tech.console.panels.FioranoJndiExtraPropertiesPanel", null);

    public static JmsProviderType fromName(String name) {
        return providerTypesByName.get(name);
    }

    public String getName() {
        return name;
    }

    public String getInitialContextFactoryClass() {
        return initialContextFactoryClass;
    }

    public String getDefaultQueueFactoryUrl() {
        return defaultQueueFactoryUrl;
    }

    public String getDefaultTopicFactoryUrl() {
        return defaultTopicFactoryUrl;
    }

    public String getDefaultDestinationFactoryUrl() {
        return defaultDestinationFactoryUrl;
    }

    public String getJndiExtraPropertiesClass() {
        return jndiExtraPropertiesClass;
    }

    public String getQueueExtraPropertiesClass() {
        return queueExtraPropertiesClass;
    }

    public JmsProvider createProvider() {
        return new JmsProvider(name, initialContextFactoryClass, defaultQueueFactoryUrl);
    }

    // - PRIVATE

    private JmsProviderType(String name, String initialCFClass,
                            String defaultQFUrl, String defaultTFUrl, String defaultDestinationFactoryUrl,
                            String jndiExtraPropertiesClass, String queueExtraPropertiesClass) {
        this.name = name;
        this.initialContextFactoryClass = initialCFClass;
        this.defaultQueueFactoryUrl = defaultQFUrl;
        this.defaultTopicFactoryUrl = defaultTFUrl;
        this.defaultDestinationFactoryUrl = defaultDestinationFactoryUrl;
        this.jndiExtraPropertiesClass = jndiExtraPropertiesClass;
        this.queueExtraPropertiesClass = queueExtraPropertiesClass;
    }

    private static Map<String,JmsProviderType> providerTypesByName = new HashMap<String, JmsProviderType>();
    static {
        for(JmsProviderType providerType : JmsProviderType.values()) {
            providerTypesByName.put(providerType.name(), providerType);
        }
    }

    private final String name;
    private final String initialContextFactoryClass;
    private final String defaultQueueFactoryUrl;
    private final String defaultTopicFactoryUrl;
    private final String defaultDestinationFactoryUrl;
    private String jndiExtraPropertiesClass;
    private String queueExtraPropertiesClass;
}
