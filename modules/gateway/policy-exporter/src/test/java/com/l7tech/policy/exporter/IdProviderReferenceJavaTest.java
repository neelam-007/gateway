package com.l7tech.policy.exporter;

import com.l7tech.objectmodel.Goid;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class IdProviderReferenceJavaTest {

    private static final Goid TEST_GOID = new Goid(0, 0);

    private static final String ENABLED_PROPERTY = "enabled";
    private static final String SERVER_DNS_NAME_PROPERTY = "server.dns.name";
    private static final String SERVICE_ACCOUNT_PROPERTY = "service.account";
    private static final String DOMAIN_NETBIOS_NAME_PROPERTY = "domain.netbios.name";
    private static final String HOST_NETBIOS_NAME_PROPERTY = "host.netbios.name";

    private static final String SERVER_DNS_NAME = "server";
    private static final String SERVICE_ACCOUNT = "ntlmUser";
    private static final String DOMAIN_NETBIOS_NAME = "DOMAIN";
    private static final String HOST_NETBIOS_NAME = "host";

    @Mock
    private ExternalReferenceFinder externalReferenceFinder;

    private IdProviderReference idProviderReference;

    private Map<String, String> localProperties;
    private Map<String, String> importedProperties;

    @Before
    public void setUp() {
        idProviderReference = new IdProviderReference(externalReferenceFinder, TEST_GOID);

        localProperties = new HashMap<>();
        importedProperties = new HashMap<>();
    }

    /**
     * Imported properties are disabled - no verification performed.
     */
    @Test
    public void testVerifyNtlmProperties_LocalAndImportedPropertiesNull_ReturnsTrue() {
        boolean verified = idProviderReference.verifyNtlmProperties(null, null);

        assertTrue(verified);
    }

    /**
     * Imported properties are disabled - no verification performed.
     */
    @Test
    public void testVerifyNtlmProperties_ImportedPropertiesDisabled_ReturnsTrue() {
        importedProperties.put(ENABLED_PROPERTY, "false");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertTrue(verified);
    }

    /**
     * Imported properties enabled, but local are not - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_ImportedEnabledButLocalPropertiesDisabled_ReturnsFalse() {
        localProperties.put(ENABLED_PROPERTY, "false");
        importedProperties.put(ENABLED_PROPERTY, "true");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, but both sets missing "server.dns.name" - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_BothSetsMissingDNSName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, but local properties missing "server.dns.name" - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_LocalPropertiesMissingDNSName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");

        importedProperties.put(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, but imported properties missing "server.dns.name" - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_ImportedPropertiesMissingDNSName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");

        localProperties.put(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, but "server.dns.name" properties do not match - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_DNSNamePropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");

        localProperties.put(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        importedProperties.put(SERVER_DNS_NAME_PROPERTY, "differentServer");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" properties match, but both sets missing
     * "service.account" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_BothSetsMissingServiceAccount_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" properties match, but local missing
     * "service.account" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_LocalPropertiesMissingServiceAccount_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);

        importedProperties.put(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" properties match, but imported missing
     * "service.account" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_ImportedPropertiesMissingServiceAccount_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);

        localProperties.put(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" properties match, but "service.account" properties
     * do not match - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_ServiceAccountPropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);

        localProperties.put(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        importedProperties.put(SERVICE_ACCOUNT_PROPERTY, "differentUser");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" and "service.account" properties match, but
     * both sets missing "domain.netbios.name" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_BothSetsMissingDomainNetbiosName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" and "service.account" properties match, but
     * local missing "domain.netbios.name" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_LocalPropertiesMissingDomainNetbiosName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);

        importedProperties.put(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" and "service.account" properties match, but
     * imported missing "domain.netbios.name" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_ImportedPropertiesMissingDomainNetbiosName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);

        localProperties.put(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name" and "service.account" properties match, but
     * "domain.netbios.name" properties do not match - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_DomainNetbiosNamePropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);

        localProperties.put(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);
        importedProperties.put(DOMAIN_NETBIOS_NAME_PROPERTY, "differentDomain");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name", "service.account", and "domain.netbios.name" properties
     * match, but both sets missing "host.netbios.name" property - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_BothSetsMissingHostNetbiosName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name", "service.account", and "domain.netbios.name" properties
     * match, but local properties missing "host.netbios.name" - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_LocalPropertiesMissingHostNetbiosName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        importedProperties.put(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name", "service.account", and "domain.netbios.name" properties
     * match, but imported properties missing "host.netbios.name" - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_ImportedPropertiesMissingHostNetbiosName_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        localProperties.put(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, "server.dns.name", "service.account", and "domain.netbios.name" properties
     * match, but "host.netbios.name" properties do not match - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_HostNetbiosNamePropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        localProperties.put(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);
        importedProperties.put(HOST_NETBIOS_NAME_PROPERTY, "differentHost");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, all properties present and matching - verification succeeds.
     */
    @Test
    public void testVerifyNtlmProperties_AllPropertiesMatch_ReturnsTrue() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);
        addToBothPropertyMaps(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertTrue(verified);
    }

    /**
     * Properties enabled, all properties present but "server,dns.name" not matching - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_AllPropertiedGiveButDNSNamePropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);
        addToBothPropertyMaps(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);

        localProperties.put(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        importedProperties.put(SERVER_DNS_NAME_PROPERTY, "differentServer");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, all properties present but "service.account" not matching - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_AllPropertiedGiveButServiceAccountPropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);
        addToBothPropertyMaps(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);

        localProperties.put(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        importedProperties.put(SERVICE_ACCOUNT_PROPERTY, "differentUser");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, all properties present but "domain.netbios.name" not matching - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_AllPropertiedGiveButDomainNetbiosNamePropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);

        localProperties.put(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);
        importedProperties.put(DOMAIN_NETBIOS_NAME_PROPERTY, "differentDomain");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    /**
     * Properties enabled, all properties present but "host.netbios.name" not matching - verification fails.
     */
    @Test
    public void testVerifyNtlmProperties_AllPropertiedGiveButHostNetbiosNamePropertiesNotMatching_ReturnsFalse() {
        addToBothPropertyMaps(ENABLED_PROPERTY, "true");
        addToBothPropertyMaps(SERVER_DNS_NAME_PROPERTY, SERVER_DNS_NAME);
        addToBothPropertyMaps(SERVICE_ACCOUNT_PROPERTY, SERVICE_ACCOUNT);
        addToBothPropertyMaps(DOMAIN_NETBIOS_NAME_PROPERTY, DOMAIN_NETBIOS_NAME);

        localProperties.put(HOST_NETBIOS_NAME_PROPERTY, HOST_NETBIOS_NAME);
        importedProperties.put(HOST_NETBIOS_NAME_PROPERTY, "differentHost");

        boolean verified = idProviderReference.verifyNtlmProperties(localProperties, importedProperties);

        assertFalse(verified);
    }

    private void addToBothPropertyMaps(String name, String value) {
        localProperties.put(name, value);
        importedProperties.put(name, value);
    }
}
