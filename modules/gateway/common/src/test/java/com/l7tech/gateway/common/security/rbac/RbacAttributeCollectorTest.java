package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.util.SyspropUtil;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RbacAttributeCollectorTest {
    private static final String ENABLED = "enabled";
    private static final String IS_ENABLED = "is enabled";
    private static final String DISABLED = "disabled";
    private static final String IS_DISABLED = "is disabled";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String TYPE = "type";
    private static final String SOAP = "soap";
    private static final String IS_SOAP = "is SOAP";
    private static final String USER_NAME_CAMEL_CASE = "userName";
    private static final String USERNAME = "username";

    @Test
    public void nameOnlyEntityTypes() {
        assertNameOnly(EntityType.ANY);
        assertNameOnly(EntityType.ASSERTION_ACCESS);
        assertNameOnly(EntityType.CUSTOM_KEY_VALUE_STORE);
        assertNameOnly(EntityType.CLUSTER_PROPERTY);
        assertNameOnly(EntityType.CLUSTER_INFO);
        assertNameOnly(EntityType.FOLDER);
        assertNameOnly(EntityType.SSG_KEY_ENTRY);
        assertNameOnly(EntityType.REVOCATION_CHECK_POLICY);
        assertNameOnly(EntityType.RESOLUTION_CONFIGURATION);
        assertNameOnly(EntityType.TRUSTED_ESM);
    }

    @Test
    public void noAttributes() {
        assertTrue(RbacAttributeCollector.collectAttributes(EntityType.PASSWORD_POLICY).isEmpty());
        assertTrue(RbacAttributeCollector.collectAttributes(EntityType.POLICY_ALIAS).isEmpty());
        assertTrue(RbacAttributeCollector.collectAttributes(EntityType.SSG_KEYSTORE).isEmpty());
        assertTrue(RbacAttributeCollector.collectAttributes(EntityType.METRICS_BIN).isEmpty());
        assertTrue(RbacAttributeCollector.collectAttributes(EntityType.SERVICE_USAGE).isEmpty());
        assertTrue(RbacAttributeCollector.collectAttributes(EntityType.SERVICE_ALIAS).isEmpty());
    }

    @Test
    public void activeConnector() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SSG_ACTIVE_CONNECTOR);
        assertEquals(3, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
        assertEquals(TYPE, attributes.get(TYPE));
    }

    @Test
    public void auditAdmin() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.AUDIT_ADMIN);
        assertEquals(6, attributes.size());
        assertEquals("IP address", attributes.get("ipAddress"));
        assertEquals("level", attributes.get("strLvl"));
        assertEquals("user id", attributes.get("userId"));
        assertEquals(USERNAME, attributes.get(USER_NAME_CAMEL_CASE));
        assertEquals("action", attributes.get("action"));
        assertEquals("entity class name", attributes.get("entityClassname"));
    }

    @Test
    public void auditMessage() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.AUDIT_MESSAGE);
        assertEquals(5, attributes.size());
        assertEquals("IP address", attributes.get("ipAddress"));
        assertEquals("level", attributes.get("strLvl"));
        assertEquals("user id", attributes.get("userId"));
        assertEquals(USERNAME, attributes.get(USER_NAME_CAMEL_CASE));
        assertEquals("operation", attributes.get("operationName"));
    }

    @Test
    public void auditRecord() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.AUDIT_RECORD);
        assertEquals(4, attributes.size());
        assertEquals("IP address", attributes.get("ipAddress"));
        assertEquals("level", attributes.get("strLvl"));
        assertEquals("user id", attributes.get("userId"));
        assertEquals(USERNAME, attributes.get(USER_NAME_CAMEL_CASE));
    }

    @Test
    public void auditSystem() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.AUDIT_SYSTEM);
        assertEquals(6, attributes.size());
        assertEquals("IP address", attributes.get("ipAddress"));
        assertEquals("level", attributes.get("strLvl"));
        assertEquals("user id", attributes.get("userId"));
        assertEquals(USERNAME, attributes.get(USER_NAME_CAMEL_CASE));
        assertEquals("action", attributes.get("action"));
        assertEquals("component id", attributes.get("componentId"));
    }

    @Test
    public void connector() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SSG_CONNECTOR);
        assertEquals(4, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("port", attributes.get("port"));
        assertEquals("scheme", attributes.get("scheme"));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void encapsulatedAssertion() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.ENCAPSULATED_ASSERTION);
        assertEquals(2, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("guid", attributes.get("guid"));
    }

    @Test
    public void emailListener() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.EMAIL_LISTENER);
        assertEquals(7, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("folder", attributes.get("folder"));
        assertEquals("host", attributes.get("host"));
        assertEquals("port", attributes.get("port"));
        assertEquals("server type", attributes.get("serverType"));
        assertEquals(USERNAME, attributes.get(USERNAME));
        assertEquals("active", attributes.get("active"));
    }

    @Test
    public void firewallRule() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.FIREWALL_RULE);
        assertEquals(2, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void generic() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.GENERIC);
        assertEquals(4, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
        assertEquals("entity class name", attributes.get("entityClassName"));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void group() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.GROUP);
        assertEquals(2, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
    }

    @Test
    public void httpConfiguration() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.HTTP_CONFIGURATION);
        assertEquals(7, attributes.size());
        assertEquals("host", attributes.get("host"));
        assertEquals("NTLM domain", attributes.get("ntlmDomain"));
        assertEquals("NTLM host", attributes.get("ntlmHost"));
        assertEquals("path", attributes.get("path"));
        assertEquals("port", attributes.get("port"));
        assertEquals("protocol", attributes.get("protocol"));
        assertEquals(USERNAME, attributes.get(USERNAME));
    }

    @Test
    public void identityProvider() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.ID_PROVIDER_CONFIG);
        assertEquals(4, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
        assertEquals(TYPE, attributes.get("typeVal"));
        assertEquals("administration enabled", attributes.get("adminEnabled"));
    }

    @Test
    public void jdbcConnection() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.JDBC_CONNECTION);
        assertEquals(5, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("driver class", attributes.get("driverClass"));
        assertEquals("jdbc URL", attributes.get("jdbcUrl"));
        assertEquals(USERNAME, attributes.get(USER_NAME_CAMEL_CASE));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void jmsConnection() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.JMS_CONNECTION);
        assertEquals(9, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("destination factory URL", attributes.get("destinationFactoryUrl"));
        assertEquals("initial context factory class name", attributes.get("initialContextFactoryClassname"));
        assertEquals("jndi URL", attributes.get("jndiUrl"));
        assertEquals("provider type", attributes.get("providerType"));
        assertEquals("queue factory URL", attributes.get("queueFactoryUrl"));
        assertEquals("topic factory URL", attributes.get("topicFactoryUrl"));
        assertEquals(USERNAME, attributes.get(USERNAME));
        assertEquals("is a template", attributes.get("template"));
    }

    @Test
    public void jmsEndpoint() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.JMS_ENDPOINT);
        assertEquals(9, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("destination name", attributes.get("destinationName"));
        assertEquals("failure destination name", attributes.get("failureDestinationName"));
        assertEquals("replyTo queue", attributes.get("replyToQueueName"));
        assertEquals(USERNAME, attributes.get(USERNAME));
        assertEquals(IS_DISABLED, attributes.get(DISABLED));
        assertEquals("is a message source", attributes.get("messageSource"));
        assertEquals("is a queue", attributes.get("queue"));
        assertEquals("is a template", attributes.get("template"));
    }

    @Test
    public void logSink() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.LOG_SINK);
        assertEquals(5, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
        assertEquals("severity", attributes.get("severity"));
        assertEquals(TYPE, attributes.get(TYPE));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void policy() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.POLICY);
        assertEquals(5, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("guid", attributes.get("guid"));
        assertEquals(TYPE, attributes.get(TYPE));
        assertEquals(IS_DISABLED, attributes.get(DISABLED));
        assertEquals(IS_SOAP, attributes.get(SOAP));
    }

    @Test
    public void resourceEntry() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.RESOURCE_ENTRY);
        assertEquals(3, attributes.size());
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
        assertEquals(TYPE, attributes.get(TYPE));
        assertEquals("uri", attributes.get("uri"));
    }

    @Test
    public void role() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.RBAC_ROLE);
        assertEquals(3, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
        assertEquals("entity type", attributes.get("entityType"));
    }

    @Test
    public void sampleMessage() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SAMPLE_MESSAGE);
        assertEquals(2, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("operation", attributes.get("operationName"));
    }

    @Test
    public void securePassword() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SECURE_PASSWORD);
        assertEquals(4, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
        assertEquals(TYPE, attributes.get(TYPE));
        assertEquals("can be referenced by a context variable", attributes.get("usageFromVariable"));
    }

    @Test
    public void securityZone() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SECURITY_ZONE);
        assertEquals(2, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(DESCRIPTION, attributes.get(DESCRIPTION));
    }

    @Test
    public void service() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SERVICE);
        assertEquals(8, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("base URI", attributes.get("baseURI"));
        assertEquals("default routing URL", attributes.get("defaultRoutingUrl"));
        assertEquals("routing URI", attributes.get("routingUri"));
        assertEquals("WSDL URL", attributes.get("wsdlUrl"));
        assertEquals(IS_DISABLED, attributes.get(DISABLED));
        assertEquals("is internal", attributes.get("internal"));
        assertEquals(IS_SOAP, attributes.get(SOAP));
    }

    @Test
    public void serviceTemplate() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SERVICE_TEMPLATE);
        assertEquals(3, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals(TYPE, attributes.get(TYPE));
        assertEquals(IS_SOAP, attributes.get(SOAP));
    }

    @Test
    public void siteminderConfiguration() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.SITEMINDER_CONFIGURATION);
        assertEquals(5, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("address", attributes.get("address"));
        assertEquals("hostname", attributes.get("hostname"));
        assertEquals(USERNAME, attributes.get(USER_NAME_CAMEL_CASE));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void trustedCertificate() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.TRUSTED_CERT);
        assertEquals(11, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("issuer DN", attributes.get("issuerDn"));
        assertEquals("serial", attributes.get("serial"));
        assertEquals("ski", attributes.get("ski"));
        assertEquals("subject DN", attributes.get("subjectDn"));
        assertEquals("is a trust anchor", attributes.get("trustAnchor"));
        assertEquals("is trusted as a SAML attesting entity", attributes.get("trustedAsSamlAttestingEntity"));
        assertEquals("is trusted as a SAML issuer", attributes.get("trustedAsSamlIssuer"));
        assertEquals("is trusted for signing client certificates", attributes.get("trustedForSigningClientCerts"));
        assertEquals("is trusted for signing server certificates", attributes.get("trustedForSigningServerCerts"));
        assertEquals("is trusted for SSL", attributes.get("trustedForSsl"));
    }

    @Test
    public void trustedEsmUser() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.TRUSTED_ESM_USER);
        assertEquals(4, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("ESM user display name", attributes.get("esmUserDisplayName"));
        assertEquals("ESM user id", attributes.get("esmUserId"));
        assertEquals("SSG user id", attributes.get("ssgUserId"));
    }

    @Test
    public void uddiProxiedServiceInfo() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.UDDI_PROXIED_SERVICE_INFO);
        assertEquals(3, attributes.size());
        assertEquals("publish type", attributes.get("publishType"));
        assertEquals("business key", attributes.get("uddiBusinessKey"));
        assertEquals("business name", attributes.get("uddiBusinessName"));
    }

    @Test
    public void uddiRegistry() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.UDDI_REGISTRY);
        assertEquals(9, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("base URL", attributes.get("baseUrl"));
        assertEquals("inquiry URL", attributes.get("inquiryUrl"));
        assertEquals("publish URL", attributes.get("publishUrl"));
        assertEquals("account username", attributes.get("registryAccountUserName"));
        assertEquals("security URL", attributes.get("securityUrl"));
        assertEquals("subscription URL", attributes.get("subscriptionUrl"));
        assertEquals("registry type", attributes.get("uddiRegistryType"));
        assertEquals(IS_ENABLED, attributes.get(ENABLED));
    }

    @Test
    public void uddiServiceControl() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.UDDI_SERVICE_CONTROL);
        assertEquals(5, attributes.size());
        assertEquals("business key", attributes.get("uddiBusinessKey"));
        assertEquals("business name", attributes.get("uddiBusinessName"));
        assertEquals("UDDI service key", attributes.get("uddiServiceKey"));
        assertEquals("UDDI service name", attributes.get("uddiServiceName"));
        assertEquals("WSDL service name", attributes.get("wsdlServiceName"));
    }

    @Test
    public void user() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(EntityType.USER);
        assertEquals(7, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
        assertEquals("department", attributes.get("department"));
        assertEquals("email", attributes.get("email"));
        assertEquals("first name", attributes.get("firstName"));
        assertEquals("last name", attributes.get("lastName"));
        assertEquals("login", attributes.get("login"));
        assertEquals("subject DN", attributes.get("subjectDn"));
    }

    @Test
    public void collectAttributes() {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(StubEntity.class);
        assertEquals(11, attributes.size());
        assertEquals("string", attributes.get("string"));
        assertEquals("wrapperLong", attributes.get("wrapperLong"));
        assertEquals("wrapperInt", attributes.get("wrapperInt"));
        assertEquals("wrapperShort", attributes.get("wrapperShort"));
        assertEquals("wrapperByte", attributes.get("wrapperByte"));
        assertEquals("goid", attributes.get("goid"));
        assertEquals("enumeration", attributes.get("enumeration"));
        assertEquals("primitiveLong", attributes.get("primitiveLong"));
        assertEquals("primitiveInt", attributes.get("primitiveInt"));
        assertEquals("primitiveShort", attributes.get("primitiveShort"));
        assertEquals("primitiveByte", attributes.get("primitiveByte"));
        assertFalse(attributes.containsKey("nonExposedAttribute"));
        assertFalse(attributes.containsKey("void"));
        assertFalse(attributes.containsKey("nonAnnotated"));
        assertFalse(attributes.containsKey("unsupportedReturnType"));
    }

    @Test
    public void allowAllAttributes() {
        SyspropUtil.setProperty(RbacAttributeCollector.ALLOW_ALL_PROP, "true");
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(StubEntity.class);
        assertEquals(13, attributes.size());
        // non-annotated methods should be valid
        assertEquals("nonAnnotated", attributes.get("nonAnnotated"));
        // non-annotated methods should get a display name if possible
        assertEquals(IS_SOAP, attributes.get(SOAP));
        SyspropUtil.clearProperty(RbacAttributeCollector.ALLOW_ALL_PROP);
    }

    private void assertNameOnly(final EntityType type) {
        final Map<String, String> attributes = RbacAttributeCollector.collectAttributes(type);
        assertEquals(1, attributes.size());
        assertEquals(NAME, attributes.get(NAME));
    }

    private class StubEntity {
        @RbacAttribute
        public String getString() {
            return "";
        }

        @RbacAttribute
        public Long getWrapperLong() {
            return new Long(1);
        }


        @RbacAttribute
        public Integer getWrapperInt() {
            return new Integer(1);
        }


        @RbacAttribute
        public Short getWrapperShort() {
            return new Short("1");
        }


        @RbacAttribute
        public Byte getWrapperByte() {
            return new Byte("1");
        }

        @RbacAttribute
        public Goid getGoid() {
            return new Goid(0, 1);
        }


        @RbacAttribute
        public EntityType getEnumeration() {
            return EntityType.ANY;
        }

        @RbacAttribute
        public long getPrimitiveLong() {
            return 1L;
        }

        @RbacAttribute
        public int getPrimitiveInt() {
            return 1;
        }

        @RbacAttribute
        public short getPrimitiveShort() {
            return 1;
        }


        @RbacAttribute
        public byte getPrimitiveByte() {
            return 1;
        }


        @RbacAttribute
        public void getVoid() {
            // should not be exposed as an attribute due to invalid return type
        }

        public String getNonAnnotated() {
            return "should not be exposed as an attribute because annotation is missing";
        }

        public boolean isSoap() {
            return false;
        }

        @RbacAttribute
        public Entity getUnsupportedReturnType() {
            // should not be exposed as an attribute due to invalid return type
            return null;
        }
    }
}
