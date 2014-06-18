package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.custom.CustomEntitiesTestBase;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.entity.CustomEntityDescriptor;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.entity.CustomEntityType;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.test.BugId;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the policy exporter.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyExporterTest extends CustomEntitiesTestBase {
    private PolicyExporter exporter;
    @Mock
    private ExternalReferenceFinder finder;
    @Mock
    private EntityResolver resolver;
    @Mock
    private ExternalReferenceFactory factory;

    @Mock
    private KeyValueStore keyValueStore;

    @Before
    public void setup() {
        exporter = new PolicyExporter(finder, resolver);
    }

    static {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }
    
    @Test
    public void testExportToDocument() throws Exception  {
        Document export = exportToDocument();

        System.out.println( XmlUtil.nodeToFormattedString( export ) );

        Element exportEle = export.getDocumentElement();
        assertNotNull( "Export null", exportEle );
        Element referencesEle = XmlUtil.findExactlyOneChildElementByName( exportEle, "http://www.layer7tech.com/ws/policy/export", "References" );
        Element idProvRefEle = XmlUtil.findExactlyOneChildElementByName( referencesEle,"IDProviderReference" );
        assertEquals("ID provider reference type", "com.l7tech.console.policy.exporter.IdProviderReference", idProvRefEle.getAttribute( "RefType" ));
        Element jmsConnRefEle = XmlUtil.findExactlyOneChildElementByName( referencesEle, "JMSConnectionReference" );
        assertEquals("Jms reference type", "com.l7tech.console.policy.exporter.JMSEndpointReference", jmsConnRefEle.getAttribute( "RefType" ));
        Element encassRefEle = XmlUtil.findExactlyOneChildElementByName( referencesEle, "EncapsulatedAssertionReference" );
        assertEquals("Encass reference type", "com.l7tech.console.policy.exporter.EncapsulatedAssertionReference", encassRefEle.getAttribute( "RefType" ));
        List<Element> passwordRefElems = XmlUtil.findAllChildElementsByName(referencesEle, "StoredPasswordReference");
        Assert.assertEquals("Incorrect number of password references", 2, passwordRefElems.size());
        assertEquals("Stored Password reference type", "com.l7tech.console.policy.exporter.StoredPasswordReference", passwordRefElems.get(0).getAttribute("RefType"));
        assertEquals("Stored Password reference type", "com.l7tech.console.policy.exporter.StoredPasswordReference", passwordRefElems.get(1).getAttribute("RefType"));
    }

    @Test
    public void testParseReferences() throws Exception {
        Document exportedPolicy = exportToDocument();
        Element referencesEl = DomUtils.findOnlyOneChildElementByName(exportedPolicy.getDocumentElement(),
                                                                     ExporterConstants.EXPORTED_POL_NS,
                                                                     ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        final ExternalReferenceFinder finder = getExternalReferenceFinder();
        Collection<ExternalReference> refs = ExternalReference.parseReferences(finder, null, finder.findAllExternalReferenceFactories(), referencesEl);
        Set<String> referenceTypes = new HashSet<String>();
        for ( ExternalReference ref : refs) {
            System.out.println("Found ref of type " + ref.getRefType());
            referenceTypes.add(ref.getRefType());
        }
        
        assertTrue("Missing CustomAssertionReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.CustomAssertionReference" ));
        assertTrue("Missing ExternalSchemaReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.ExternalSchemaReference" ));
        assertTrue("Missing FederatedIdProviderReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.FederatedIdProviderReference" ));
        assertTrue("Missing IdProviderReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.IdProviderReference" ));
        assertTrue("Missing IncludedPolicyReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.IncludedPolicyReference" ));
        assertTrue("Missing JdbcConnectionReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.JdbcConnectionReference" ));
        assertTrue("Missing JMSEndpointReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.JMSEndpointReference" ));
        assertTrue("Missing PrivateKeyReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.PrivateKeyReference" ));
        assertTrue("Missing TrustedCertReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.TrustedCertReference" ));
        assertFalse("Found EncapsulatedAssertionReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.EncapsulatedAssertionReference" ));
        assertTrue("Missing StoredPasswordReference", referenceTypes.contains( "com.l7tech.console.policy.exporter.StoredPasswordReference" ));
        assertEquals("Reference count", 10, referenceTypes.size());
    }

    @BugId("SSG-7622")
    @Test
    public void exportToDocumentIncludeCannotReadPolicy() throws Exception {
        final String policyGuid = "abc123";
        final AllAssertion all = new AllAssertion();
        final Include include = new Include();
        include.setPolicyGuid(policyGuid);
        all.addChild(include);
        when(finder.findPolicyByGuid(policyGuid)).thenThrow(new PermissionDeniedException(OperationType.READ, EntityType.POLICY));

        final Document document = exporter.exportToDocument(all, Collections.singleton(factory));
        final NodeList nodes = document.getElementsByTagName("IncludedPolicyReference");
        assertEquals(1, nodes.getLength());
        final NamedNodeMap attributes = nodes.item(0).getAttributes();
        assertEquals(2, attributes.getLength());
        assertEquals("com.l7tech.console.policy.exporter.IncludedPolicyReference", attributes.getNamedItem("RefType").getTextContent());
        assertEquals(policyGuid, attributes.getNamedItem("guid").getTextContent());
        verify(finder).findPolicyByGuid(policyGuid);
    }

    private Document exportToDocument() throws Exception {
        final ExternalReferenceFinder finder = getExternalReferenceFinder();
        PolicyExporter exporter = new PolicyExporter(finder, null);
        Assertion testPolicy = createTestPolicy();
        return exporter.exportToDocument(testPolicy, finder.findAllExternalReferenceFactories());
    }

    private Assertion createTestPolicy() {
        AllAssertion root = new AllAssertion();

        // CustomAssertionReference
        CustomAssertionHolder cahass = new CustomAssertionHolder();
        cahass.setCategories(Category.MESSAGE);
        cahass.setCustomAssertion(new TestCustomAssertion());
        root.getChildren().add(cahass);

        // External Schema Reference
        SchemaValidation svass = new SchemaValidation();
        GlobalResourceInfo info = new GlobalResourceInfo();
        info.setId("test.xsd");
        svass.setResourceInfo( info );
        root.addChild( svass );

        // FederatedIdProviderReference
        SpecificUser suassf = new SpecificUser(new Goid(0,1234), "feduser", null, null);
        root.addChild(suassf);        

        // IdProviderReference
        SpecificUser suass = new SpecificUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "john", null, null);
        root.addChild(suass);

        // IncludedPolicyReference
        Include iass = new Include();
        iass.setPolicyGuid( UUID.nameUUIDFromBytes("policyname".getBytes()).toString() );
        root.addChild( iass );

        TestJDBCAssertion tjass = new TestJDBCAssertion();
        root.addChild(tjass);

        // JMSEndpointReference
        JmsRoutingAssertion jrass = new JmsRoutingAssertion();
        jrass.setEndpointName("blah");
        jrass.setEndpointOid((long) 25);
        jrass.setResponseTimeout("55");
        root.addChild(jrass);

        // PrivatekeyReference
        AddWssTimestamp awtass = new AddWssTimestamp();
        awtass.setUsesDefaultKeyStore( false );
        awtass.setNonDefaultKeystoreId( 12 );
        awtass.setKeyAlias( "key1" );
        root.addChild( awtass );

        // TrustedCertReference
        WsSecurity wsass = new WsSecurity();
        wsass.setRecipientTrustedCertificateGoid(new Goid(0, 1234L));
        root.addChild( wsass );

        // EncapsulatedAssertionReference
        EncapsulatedAssertion encass = new EncapsulatedAssertion();
        encass.setEncapsulatedAssertionConfigGuid("encass-guid");
        encass.setEncapsulatedAssertionConfigName("encass-name");
        root.addChild( encass );

        //SecurePasswordReference
        TestSecurePasswordAssertion securePasswordAssertion = new TestSecurePasswordAssertion();
        root.addChild( securePasswordAssertion );

        return root;
    }

    private ExternalReferenceFinder getExternalReferenceFinder() {
        return new ExternalReferenceFinderStub(){
            @Override
            public IdentityProviderConfig findIdentityProviderConfigByID( final Goid providerOid ) throws FindException {
                IdentityProviderConfig config;
                if ( providerOid.equals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID )){
                    config = new IdentityProviderConfig( IdentityProviderType.INTERNAL );
                    config.setName( "Internal Identity Provider" );
                } else {
                    config = new IdentityProviderConfig( IdentityProviderType.FEDERATED );
                    config.setName( "Federated Provider" );
                }
                return config;
            }

            @Override
            public Policy findPolicyByGuid( final String guid ) throws FindException {
                String xml = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"></wsp:All></wsp:Policy>";
                Policy policy = new Policy( PolicyType.INCLUDE_FRAGMENT, "fragment", xml, false );
                policy.setGuid( guid );
                return policy;
            }

            @Override
            public JdbcConnection getJdbcConnection( final String name ) throws FindException {
                JdbcConnection jdbcConnection = new JdbcConnection();
                jdbcConnection.setName( name );
                jdbcConnection.setDriverClass( "com.somesql.Driver" );
                jdbcConnection.setJdbcUrl( "jdbc:somesql:db" );
                return jdbcConnection;
            }

            @Override
            public SecurePassword findSecurePasswordById(Goid id) throws FindException {
                if(Goid.equals(new Goid(0,123), id)) {
                    SecurePassword securePassword = new SecurePassword("privateKey");
                    securePassword.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                    securePassword.setGoid(new Goid(0,123));
                    securePassword.setDescription("Private Key Description");
                    return securePassword;
                } else if(Goid.equals(new Goid(0,456), id)) {
                    SecurePassword securePassword = new SecurePassword("Password");
                    securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
                    securePassword.setGoid(new Goid(0,456));
                    securePassword.setDescription("My Password Description");
                    return securePassword;
                }
                return null;
            }
        };
    }

    private static final class TestCustomAssertion implements CustomAssertion {
        @Override
        public String getName() {
            return "Test Custom Assertion";
        }
    }

    public static final class TestSecurePasswordAssertion extends Assertion implements UsesEntities {
        private Goid privateKeyGoid = new Goid(0,123);
        private String privateKeyName = "privateKey";
        private String privateKeyDescription = "Private Key Description";
        private Goid passwordGoid = new Goid(0, 456);
        private String passwordName = "Password";
        private String passwordDescription = "My Password Description";

        @Override
        public EntityHeader[] getEntitiesUsed() {
            return new EntityHeader[]{
                    new SecurePasswordEntityHeader(privateKeyGoid, EntityType.SECURE_PASSWORD, privateKeyName, privateKeyDescription, SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY.name()),
                    new SecurePasswordEntityHeader(passwordGoid, EntityType.SECURE_PASSWORD, passwordName, passwordDescription, SecurePassword.SecurePasswordType.PASSWORD.name())};
        }

        @Override
        public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
                if (Goid.equals(oldEntityHeader.getGoid(), privateKeyGoid)) {
                    privateKeyGoid = newEntityHeader.getGoid();
                } else if (Goid.equals(oldEntityHeader.getGoid(), passwordGoid)) {
                    passwordGoid = newEntityHeader.getGoid();
                }
        }
    }

    public static final class TestJDBCAssertion extends Assertion implements JdbcConnectionable {
        private static final String META_INITIALIZED = TestJDBCAssertion.class.getName() + ".metadataInitialized";
        private String connectionName = "jdbcConnectionName";

        @Override
        public String getConnectionName() {
            return connectionName;
        }

        @Override
        public void setConnectionName( final String connectionName ) {
            this.connectionName = connectionName;
        }

        @Override
        public AssertionMetadata meta() {
            DefaultAssertionMetadata meta = super.defaultMeta();
            if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
                return meta;

            meta.put(AssertionMetadata.SHORT_NAME, "JdbcTest");
            meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
            meta.put(META_INITIALIZED, Boolean.TRUE);

            return meta;
        }

    }

    /**
     * Utility function for mocking common objects.
     */
    private void mockCustomExternalReferencesCommon() throws Exception {
        // mock findAllExternalReferenceFactories (not interested in this)
        Mockito.doAnswer(new Answer<Set<ExternalReferenceFactory>>() {
            @Override
            public Set<ExternalReferenceFactory> answer(final InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(finder).findAllExternalReferenceFactories();

        // mock our keyValueStore
        Mockito.doAnswer(new Answer<KeyValueStore>() {
            @Override
            public KeyValueStore answer(final InvocationOnMock invocation) throws Throwable {
                return keyValueStore;
            }
        }).when(finder).getCustomKeyValueStore();

        // mock our Serializer
        Mockito.doAnswer(new Answer<CustomEntitySerializer>() {
            @Override
            public CustomEntitySerializer answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String className = (String) param1;
                if (className.startsWith(CustomEntitiesTestBase.class.getName() + "$")) {
                    return createEntitySerializer();
                }
                fail("Unregistered entity serializer class: \"" + className + "\"");
                return null;
            }
        }).when(finder).getCustomKeyValueEntitySerializer(Matchers.anyString());

        // mock keyValueStore registry
        Mockito.doAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;

                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // KVS1        KVS2          KVS3         KVS4
                // |_SP1       |_SP2         |_PK1        |_SP1
                // |_KVS2      |_KVS3        |_KVS1       |_PK1
                // |_KVS3                                 |_KVS1
                //                                        |_SP4
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                if (keyValueStoreId(1).equals(key)) {
                    //noinspection serial
                    return createEntitySerializer().serialize(
                            new CustomEntitiesTestBase.TestCustomAssertionEntity() {{
                                addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1  -- Attr: SecurePassword1
                                addEntityReference(keyValueStoreId(2), CustomEntityType.KeyValueStore);      // KVS2 -- Attr: KeyValueStore2
                                addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);      // KVS3 -- Attr: KeyValueStore3
                            }}
                    );
                } else if (keyValueStoreId(2).equals(key)) {
                    //noinspection serial
                    return createEntitySerializer().serialize(
                            new TestEntityWithCustomEntityDescriptor() {{
                                addEntityReference(securePasswordId(2), CustomEntityType.SecurePassword);    // SP2  -- Attr: SecurePassword1
                                addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);      // KVS3 -- Attr: KeyValueStore2
                            }}
                    );
                } else if (keyValueStoreId(3).equals(key)) {
                    //noinspection serial
                    return createEntitySerializer().serialize(
                            new CustomEntitiesTestBase.TestCustomAssertionEntity() {{
                                addEntityReference(privateKeyId(1), CustomEntityType.PrivateKey);            // PK1  -- Attr: PrivateKey1
                                addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1 -- Attr: KeyValueStore2
                            }}
                    );
                } else if (keyValueStoreId(4).equals(key)) {
                    //noinspection serial
                    return createEntitySerializer().serialize(
                            new CustomEntitiesTestBase.TestCustomAssertionEntity() {{
                                addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1  -- Attr: PrivateKey1
                                addEntityReference(privateKeyId(1), CustomEntityType.PrivateKey);            // PK1  -- Attr: PrivateKey2
                                addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1 -- Attr: KeyValueStore3
                                addEntityReference(securePasswordId(4), CustomEntityType.SecurePassword);    // SP4  -- Attr: SecurePassword4
                            }}
                    );
                }
                return null;
            }
        }).when(keyValueStore).get(Matchers.anyString());

        // mock secure password registry
        Mockito.doAnswer(new Answer<SecurePassword>() {
            @Override
            public SecurePassword answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is Goid", param1 instanceof Goid);
                final Goid id = (Goid) param1;

                // mock our secure password id's
                if (id.toString().equals(securePasswordId(1))
                        || id.toString().equals(securePasswordId(2))
                        || id.toString().equals(securePasswordId(3))
                        || id.toString().equals(securePasswordId(4)))
                {
                    final SecurePassword password = new SecurePassword();
                    password.setGoid(id);
                    password.setDescription(
                            (id.toString().equals(securePasswordId(2)) // make the second GOID a PEM
                                    ? "Test PEM for id: \""
                                    : "Test Password for id: \""
                            ) + id.toString() + "\""
                    );
                    password.setType(
                            id.toString().equals(securePasswordId(2)) // make the second GOID a PEM
                                    ? SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY
                                    : SecurePassword.SecurePasswordType.PASSWORD
                    );
                    return password;
                }

                return null;
            }
        }).when(finder).findSecurePasswordById(Matchers.<Goid>any());

        // mock private-key registry
        Mockito.doAnswer(new Answer<SsgKeyEntry>() {
            @Override
            public SsgKeyEntry answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String alias = (String) param1;

                final Object param2 = invocation.getArguments()[1];
                assertTrue("Second Param is Goid", param2 instanceof Goid);
                final Goid keyId = (Goid) param2;

                if (getPrivateKeyGoidAliasPair(privateKeyId(1)).equals(Pair.pair(keyId, alias))
                        || getPrivateKeyGoidAliasPair(privateKeyId(2)).equals(Pair.pair(keyId, alias)))
                {
                    return SsgKeyEntry.createDummyEntityForAuditing(keyId, alias);
                }
                return null;
            }
        }).when(finder).findKeyEntry(Matchers.anyString(), Matchers.<Goid>any());

        // mock policy fragment
        Mockito.doAnswer(new Answer<Policy>() {
            @Override
            public Policy answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;

                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // PF1 (CA)     KVS4        KVS1         KVS2         KVS3
                // |_KVS4       |_SP1       |_SP1        |_SP2        |_PK1
                // |_SP3        |_PK1       |_KVS2       |_KVS3       |_KVS1
                // |_PK2        |_KVS1      |_KVS3
                //              |_SP4
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // Expected output:
                // PK1, PK2, SP1, SP2, SP3, SP4, KVS1, KVS2, KVS3, KVS4
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                if (policyFragmentId(1).equals(key)) {
                    final AllAssertion rootAssertion = new AllAssertion();
                    final CustomAssertionHolder holder = new CustomAssertionHolder();
                    holder.setCategories(Category.ACCESS_CONTROL);
                    holder.setDescriptionText("Test Custom Assertion in Fragment");
                    final CustomEntitiesTestBase.TestCustomAssertionWithEntities customAssertion = new CustomEntitiesTestBase.TestCustomAssertionWithEntities();
                    customAssertion.addEntityReference(keyValueStoreId(4), CustomEntityType.KeyValueStore);         // KVS4
                    customAssertion.addEntityReference(securePasswordId(3), CustomEntityType.SecurePassword);       // SP3
                    customAssertion.addEntityReference(privateKeyId(2), CustomEntityType.PrivateKey);               // PK2
                    holder.setCustomAssertion(customAssertion);
                    rootAssertion.getChildren().add(holder);
                    final Document policyDoc = WspWriter.getPolicyDocument(rootAssertion);
                    final String policyXml = XmlUtil.nodeToString(policyDoc);
                    final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "fragment1", policyXml, false);
                    policy.setGuid(key);
                    return policy;
                }
                return null;
            }
        }).when(finder).findPolicyByGuid(Matchers.anyString());
    }

    /**
     * Test CustomEntity implementing CustomEntityDescriptor
     */
    class TestEntityWithCustomEntityDescriptor extends CustomEntitiesTestBase.TestCustomAssertionEntity implements CustomEntityDescriptor {
        private static final long serialVersionUID = -541124618433438873L;
        @Override public <R> R getProperty(String name, Class<R> rClass) { return null; }
        @Override public <R> R getUiObject(String uiName, Class<R> uiClass) { return null; }
    }

    /**
     * Support class for comparing different supported entities.
     */
    class CustomEntityReferencesComparator {
        final ExternalReference reference;
        CustomEntityReferencesComparator(final ExternalReference reference) {
            this.reference = reference;
        }
        private boolean compare(final ExternalReference ref1, final ExternalReference ref2) {
            if (ref1 == ref2) { return true; }
            if (ref1 != null && ref2 != null && (ref1.getClass().isAssignableFrom(ref2.getClass()) || ref2.getClass().isAssignableFrom(ref1.getClass()))) {
                if (ref1 instanceof PrivateKeyReference || ref1 instanceof CustomAssertionReference) {
                    return ref1.equals(ref2);
                } else if (ref1 instanceof StoredPasswordReference) {
                    final StoredPasswordReference spRef1 = (StoredPasswordReference)ref1;
                    final StoredPasswordReference spRef2 = (StoredPasswordReference)ref2;
                    return (spRef1.getId() != null ? spRef1.getId().equals(spRef2.getId()) : spRef2.getId() == null) &&
                            (spRef1.getName() != null ? spRef1.getName().equals(spRef2.getName()) : spRef2.getName() == null) &&
                            (spRef1.getType() != null ? spRef1.getType().equals(spRef2.getType()) : spRef2.getType() == null) &&
                            (spRef1.getDescription() != null ? spRef1.getDescription().equals(spRef2.getDescription()) : spRef2.getDescription() == null);
                } else if (ref1 instanceof CustomKeyValueReference) {
                    final CustomKeyValueReference kvsRef1 = (CustomKeyValueReference)ref1;
                    final CustomKeyValueReference kvsRef2 = (CustomKeyValueReference)ref2;
                    final TestCustomAssertionEntity entity1 =
                            kvsRef1.getEntityBase64Value() != null
                                    ? createEntitySerializer().deserialize(HexUtils.decodeBase64(kvsRef1.getEntityBase64Value()))
                                    : null;
                    final TestCustomAssertionEntity entity2 =
                            kvsRef2.getEntityBase64Value() != null
                                    ? createEntitySerializer().deserialize(HexUtils.decodeBase64(kvsRef2.getEntityBase64Value()))
                                    : null;
                    return (kvsRef1.getEntityKey() != null ? kvsRef1.getEntityKey().equals(kvsRef2.getEntityKey()) : kvsRef2.getEntityKey() == null) &&
                            (kvsRef1.getEntityKeyPrefix() != null ? kvsRef1.getEntityKeyPrefix().equals(kvsRef2.getEntityKeyPrefix()) : kvsRef2.getEntityKeyPrefix() == null) &&
                            (kvsRef1.getEntitySerializer() != null ? kvsRef1.getEntitySerializer().equals(kvsRef2.getEntitySerializer()) : kvsRef2.getEntitySerializer() == null) &&
                            (entity1 != null ? entity1.equals(entity2) : entity2 == null);
                } else if (ref1 instanceof IncludedPolicyReference) {
                    final IncludedPolicyReference fragRef1 = (IncludedPolicyReference)ref1;
                    final IncludedPolicyReference fragRef2 = (IncludedPolicyReference)ref2;
                    final String comparableXml1 = getPolicyComparableXml(fragRef1);
                    final String comparableXml2 = getPolicyComparableXml(fragRef2);
                    return (fragRef1.getGuid() != null ? fragRef1.getGuid().equals(fragRef2.getGuid()) : fragRef2.getGuid() == null) &&
                            (fragRef1.isSoap() != null ? fragRef1.isSoap().equals(fragRef2.isSoap()) : fragRef2.isSoap() == null) &&
                            (fragRef1.getType() != null ? fragRef1.getType().equals(fragRef2.getType()) : fragRef2.getType() == null) &&
                            (fragRef1.getName() != null ? fragRef1.getName().equals(fragRef2.getName()) : fragRef2.getName() == null) &&
                            (fragRef1.getInternalTag() != null ? fragRef1.getInternalTag().equals(fragRef2.getInternalTag()) : fragRef2.getInternalTag() == null) &&
                            (fragRef1.getUseType() != null ? fragRef1.getUseType().equals(fragRef2.getUseType()) : fragRef2.getUseType() == null) &&
                            (comparableXml1 != null ? comparableXml1.equals(comparableXml2) : comparableXml2 == null);
                }
                throw new IllegalArgumentException("Unsupported reference type: " + reference.getClass().getName());
            }
            return false;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof CustomEntityReferencesComparator) {
                final CustomEntityReferencesComparator other = (CustomEntityReferencesComparator)obj;
                return compare(reference, other.reference);
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (reference == null) { return 0; }
            if (reference instanceof PrivateKeyReference || reference instanceof CustomAssertionReference) {
                return reference.hashCode();
            } else if (reference instanceof StoredPasswordReference) {
                final StoredPasswordReference passwordRef = (StoredPasswordReference)reference;
                int result = 0;
                result = 31 * result + (passwordRef.getId() != null ? passwordRef.getId().hashCode() : 0);
                result = 31 * result + (passwordRef.getName() != null ? passwordRef.getName().hashCode() : 0);
                result = 31 * result + (passwordRef.getType() != null ? passwordRef.getType().hashCode() : 0);
                result = 31 * result + (passwordRef.getDescription() != null ? passwordRef.getDescription().hashCode() : 0);
                return result;
            } else if (reference instanceof CustomKeyValueReference) {
                final CustomKeyValueReference kvsRef = (CustomKeyValueReference)reference;
                final TestCustomAssertionEntity entity =
                        kvsRef.getEntityBase64Value() != null
                                ? createEntitySerializer().deserialize(HexUtils.decodeBase64(kvsRef.getEntityBase64Value()))
                                : null;
                int result = 0;
                result = 31 * result + (kvsRef.getEntityKey() != null ? kvsRef.getEntityKey().hashCode() : 0);
                result = 31 * result + (kvsRef.getEntityKeyPrefix() != null ? kvsRef.getEntityKeyPrefix().hashCode() : 0);
                result = 31 * result + (entity != null ? entity.hashCode() : 0);
                result = 31 * result + (kvsRef.getEntitySerializer() != null ? kvsRef.getEntitySerializer().hashCode() : 0);
                return result;
            } else if (reference instanceof IncludedPolicyReference) {
                final IncludedPolicyReference fragRef = (IncludedPolicyReference)reference;
                final String comparableXml = getPolicyComparableXml(fragRef);
                int result = 0;
                result = 31 * result + (fragRef.getGuid() != null ? fragRef.getGuid().hashCode() : 0);
                result = 31 * result + (fragRef.isSoap() != null ? fragRef.isSoap().hashCode() : 0);
                result = 31 * result + (fragRef.getType() != null ? fragRef.getType().hashCode() : 0);
                result = 31 * result + (fragRef.getName() != null ? fragRef.getName().hashCode() : 0);
                result = 31 * result + (fragRef.getInternalTag() != null ? fragRef.getInternalTag().hashCode() : 0);
                result = 31 * result + (fragRef.getUseType() != null ? fragRef.getUseType().hashCode() : 0);
                result = 31 * result + (comparableXml != null ? comparableXml.hashCode() : 0);
                return result;
            }
            throw new IllegalArgumentException("Unsupported reference type: " + reference.getClass().getName());
        }

        private String getPolicyComparableXml(final IncludedPolicyReference policyReference) {
            final Policy policy = new Policy(policyReference.getType(), policyReference.getName(), policyReference.getXml(), policyReference.isSoap());
            try {
                return WspWriter.getPolicyXml(policy.getAssertion());
            } catch (final IOException e) {
                fail(e.getMessage());
            }
            return null;
        }
    }

    @Test
    public void testExportCustomAssertionExternalReferences() throws Exception {
        // mock common functionality
        mockCustomExternalReferencesCommon();

        // spy our policy exporter
        exporter = Mockito.spy(new PolicyExporter(finder, null));

        // create our test policy
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        // Policy       CA1           PF1           KVS4        KVS1         KVS2         KVS3
        // |_CA1        |_KVS1        |_KVS4        |_SP1       |_SP1        |_SP2        |_PK1
        // |_PF1                      |_SP3         |_PK1       |_KVS2       |_KVS3       |_KVS1
        //                            |_PK2         |_KVS1      |_KVS3
        //                                          |_SP4
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        // Expected output:
        // PK1, PK2, SP1, SP2, SP3, SP4, KVS1, KVS2, KVS3, KVS4
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        final AllAssertion testPolicy = new AllAssertion();
        final CustomAssertionHolder holder = new CustomAssertionHolder();
        holder.setCategories(Category.ACCESS_CONTROL);
        holder.setDescriptionText("Test Custom Assertion");
        final CustomEntitiesTestBase.TestCustomAssertionWithEntities customAssertion = new CustomEntitiesTestBase.TestCustomAssertionWithEntities();
        customAssertion.addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1
        holder.setCustomAssertion(customAssertion);
        testPolicy.getChildren().add(holder);
        testPolicy.getChildren().add(new Include(policyFragmentId(1)));        // PF1

        // export our test policy
        final Document exportDoc = exporter.exportToDocument(testPolicy, finder.findAllExternalReferenceFactories());

        System.out.println(XmlUtil.nodeToFormattedString(exportDoc));

        // Expected output:
        // PK1, PK2, SP1, SP2, SP3, SP4, KVS1, KVS2, KVS3, KVS4
        final Set<CustomEntityReferencesComparator> expectedReferences = new HashSet<>();
        expectedReferences.add(toPrivateKeyReference(privateKeyId(1)));
        expectedReferences.add(toPrivateKeyReference(privateKeyId(2)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(1)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(2)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(3)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(4)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(1)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(2)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(3)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(4)));
        // add custom assertion reference
        expectedReferences.add(new CustomEntityReferencesComparator(new CustomAssertionReference(finder, "Test Custom Assertion referencing entities")));
        // add policy fragment reference
        expectedReferences.add(new CustomEntityReferencesComparator(new IncludedPolicyReference(finder, new Include(policyFragmentId(1)))));

        final Element rootElement = exportDoc.getDocumentElement();
        assertNotNull("Root element cannot be null", rootElement);
        final Element referencesElement = XmlUtil.findExactlyOneChildElementByName(rootElement, "http://www.layer7tech.com/ws/policy/export", "References");
        assertEquals("References Count", expectedReferences.size(), referencesElement.getChildNodes().getLength());

        // verify that Custom Assertions are referenced
        final List<Element> customAssertionRefElements = XmlUtil.findAllChildElementsByName(referencesElement, "CustomAssertionReference");
        Assert.assertEquals("Incorrect number of custom assertion references", 1, customAssertionRefElements.size());
        for (final Element element : customAssertionRefElements) {
            final ExternalReference ref = CustomAssertionReference.parseFromElement(finder, element);
            assertEquals("Custom Assertion reference type", ExternalReference.getReferenceType(CustomAssertionReference.class), element.getAttribute("RefType"));
            assertTrue("Unexpected reference: " + toString(ref), expectedReferences.remove(new CustomEntityReferencesComparator(ref)));
        }

        // verify that Policy fragments are referenced
        final List<Element> policyFragRefElements = XmlUtil.findAllChildElementsByName(referencesElement, "IncludedPolicyReference");
        Assert.assertEquals("Incorrect number of policy fragment references", 1, policyFragRefElements.size());
        for (final Element element : policyFragRefElements) {
            final ExternalReference ref = IncludedPolicyReference.parseFromElement(finder, element);
            assertEquals("Policy Fragment reference type", ExternalReference.getReferenceType(IncludedPolicyReference.class), element.getAttribute("RefType"));
            assertTrue("Unexpected reference: " + toString(ref), expectedReferences.remove(new CustomEntityReferencesComparator(ref)));
        }

        // verify that PrivateKeyReference entities are referenced
        final List<Element> privateKeyRefElements = XmlUtil.findAllChildElementsByName(referencesElement, "PrivateKeyReference");
        Assert.assertEquals("Incorrect number of private-key references", 2, privateKeyRefElements.size());
        for (final Element element : privateKeyRefElements) {
            final ExternalReference ref = PrivateKeyReference.parseFromElement(finder, element);
            assertEquals("Private Key reference type", ExternalReference.getReferenceType(PrivateKeyReference.class), element.getAttribute("RefType"));
            assertTrue("Unexpected reference: " + toString(ref), expectedReferences.remove(new CustomEntityReferencesComparator(ref)));
        }

        // verify that SecurePasswords entities are referenced
        final List<Element> passwordRefElements = XmlUtil.findAllChildElementsByName(referencesElement, "StoredPasswordReference");
        Assert.assertEquals("Incorrect number of password references", 4, passwordRefElements.size());
        for (final Element element : passwordRefElements) {
            final ExternalReference ref = StoredPasswordReference.parseFromElement(finder, element);
            assertEquals("Stored Password reference type", ExternalReference.getReferenceType(StoredPasswordReference.class), element.getAttribute("RefType"));
            assertTrue("Unexpected reference: " + toString(ref), expectedReferences.remove(new CustomEntityReferencesComparator(ref)));
        }

        // verify that KeyValueStore entities are referenced
        final List<Element> keyValueStoreRefElements = XmlUtil.findAllChildElementsByName(referencesElement, "CustomKeyValueReference");
        Assert.assertEquals("Incorrect number of password references", 4, keyValueStoreRefElements.size());
        for (final Element element : keyValueStoreRefElements) {
            final ExternalReference extRef = CustomKeyValueReference.parseFromElement(finder, element);
            assertEquals("Key Value Store reference type", ExternalReference.getReferenceType(CustomKeyValueReference.class), element.getAttribute("RefType"));
            assertTrue("Unexpected reference: " + toString(extRef), expectedReferences.remove(new CustomEntityReferencesComparator(extRef)));
        }

        // make sure no elements are left unprocessed
        assertTrue(expectedReferences.isEmpty());
    }

    @Test
    public void testImportCustomAssertionExternalReferences() throws Exception {
        // mock common functionality
        mockCustomExternalReferencesCommon();

        /////////////////////////////////////////////////////////////////////////////////////////////////////
        // Policy       CA1          PF1           KVS4        KVS1         KVS2         KVS3
        // |_CA1        |_KVS1       |_KVS4        |_SP1       |_SP1        |_SP2        |_PK1
        // |_PF1                     |_SP3         |_PK1       |_KVS2       |_KVS3       |_KVS1
        //                           |_PK2         |_KVS1      |_KVS3
        //                                         |_SP4
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        final Document exportedPolicy = XmlUtil.parse(
                // this export corresponds with the above mockup key-value-store's, secure-passwords and private-keys
                "<exp:Export Version=\"3.0\" xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                        "  <exp:References>" +
                        "    <CustomAssertionReference RefType=\"com.l7tech.console.policy.exporter.CustomAssertionReference\">" +
                        "      <CustomAssertionName>Test Custom Assertion referencing entities</CustomAssertionName>" +
                        "    </CustomAssertionReference>" +
                        "    <PrivateKeyReference RefType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\">" +
                        "      <IsDefaultKey>false</IsDefaultKey>" +
                        "      <KeystoreOID>00000000000000020000000000000001</KeystoreOID>" +
                        "      <KeyAlias>alias1</KeyAlias>" +
                        "    </PrivateKeyReference>" +
                        "    <StoredPasswordReference RefType=\"com.l7tech.console.policy.exporter.StoredPasswordReference\">" +
                        "      <Id>00000000000000010000000000000002</Id>" +
                        "      <Type>PEM_PRIVATE_KEY</Type>" +
                        "      <Description>Test PEM for id: \"00000000000000010000000000000002\"</Description>" +
                        "    </StoredPasswordReference>" +
                        "    <StoredPasswordReference RefType=\"com.l7tech.console.policy.exporter.StoredPasswordReference\">" +
                        "      <Id>00000000000000010000000000000001</Id>" +
                        "      <Type>PASSWORD</Type>" +
                        "      <Description>Test Password for id: \"00000000000000010000000000000001\"</Description>" +
                        "    </StoredPasswordReference>" +
                        "    <CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">" +
                        "      <Config>" +
                        "        <KeyValueStoreName>internalTransactional</KeyValueStoreName>" +
                        "        <Key>00000000000000030000000000000001</Key>" +
                        "        <KeyPrefix>test-prefix</KeyPrefix>" +
                        "        <Base64Value>PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjwhRE9DVFlQRSBwcm9wZXJ0aWVzIFNZU1RFTSAiaHR0cDovL2phdmEuc3VuLmNvbS9kdGQvcHJvcGVydGllcy5kdGQiPg0KPHByb3BlcnRpZXM+DQo8Y29tbWVudD5jb20ubDd0ZWNoLnBvbGljeS5leHBvcnRlci5Qb2xpY3lFeHBvcnRlclRlc3QkNSQxPC9jb21tZW50Pg0KPGVudHJ5IGtleT0iX19QUk9QRVJUWV9FTlRJVFlfQ0xBU1NfTkFNRV9fMjYxQjgwMjdfMjQ4N180ODU0X0EwQzVfMEQ1NDkwMDNDQkVGX18iPmNvbS5sN3RlY2gucG9saWN5LmV4cG9ydGVyLlBvbGljeUV4cG9ydGVyVGVzdCQ1JDE8L2VudHJ5Pg0KPGVudHJ5IGtleT0iS2V5VmFsdWVTdG9yZTMiPjAwMDAwMDAwMDAwMDAwMDMwMDAwMDAwMDAwMDAwMDAzPC9lbnRyeT4NCjxlbnRyeSBrZXk9IktleVZhbHVlU3RvcmUyIj4wMDAwMDAwMDAwMDAwMDAzMDAwMDAwMDAwMDAwMDAwMjwvZW50cnk+DQo8ZW50cnkga2V5PSJTZWN1cmVQYXNzd29yZDEiPjAwMDAwMDAwMDAwMDAwMDEwMDAwMDAwMDAwMDAwMDAxPC9lbnRyeT4NCjwvcHJvcGVydGllcz4NCg==</Base64Value>" +
                        "      </Config>" +
                        "    </CustomKeyValueReference>" +
                        "    <CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">" +
                        "      <Serializer>com.l7tech.gateway.common.custom.CustomEntitiesTestBase$1</Serializer>" +
                        "      <Config>" +
                        "        <KeyValueStoreName>internalTransactional</KeyValueStoreName>" +
                        "        <Key>00000000000000030000000000000002</Key>" +
                        "        <KeyPrefix>test-prefix</KeyPrefix>" +
                        "        <Base64Value>PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjwhRE9DVFlQRSBwcm9wZXJ0aWVzIFNZU1RFTSAiaHR0cDovL2phdmEuc3VuLmNvbS9kdGQvcHJvcGVydGllcy5kdGQiPg0KPHByb3BlcnRpZXM+DQo8Y29tbWVudD5jb20ubDd0ZWNoLnBvbGljeS5leHBvcnRlci5Qb2xpY3lFeHBvcnRlclRlc3QkNSQyPC9jb21tZW50Pg0KPGVudHJ5IGtleT0iX19QUk9QRVJUWV9FTlRJVFlfQ0xBU1NfTkFNRV9fMjYxQjgwMjdfMjQ4N180ODU0X0EwQzVfMEQ1NDkwMDNDQkVGX18iPmNvbS5sN3RlY2gucG9saWN5LmV4cG9ydGVyLlBvbGljeUV4cG9ydGVyVGVzdCQ1JDI8L2VudHJ5Pg0KPGVudHJ5IGtleT0iS2V5VmFsdWVTdG9yZTIiPjAwMDAwMDAwMDAwMDAwMDMwMDAwMDAwMDAwMDAwMDAzPC9lbnRyeT4NCjxlbnRyeSBrZXk9IlNlY3VyZVBhc3N3b3JkMSI+MDAwMDAwMDAwMDAwMDAwMTAwMDAwMDAwMDAwMDAwMDI8L2VudHJ5Pg0KPC9wcm9wZXJ0aWVzPg0K</Base64Value>" +
                        "      </Config>" +
                        "    </CustomKeyValueReference>" +
                        "    <CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">" +
                        "      <Config>" +
                        "        <KeyValueStoreName>internalTransactional</KeyValueStoreName>" +
                        "        <Key>00000000000000030000000000000003</Key>" +
                        "        <KeyPrefix>test-prefix</KeyPrefix>" +
                        "        <Base64Value>PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjwhRE9DVFlQRSBwcm9wZXJ0aWVzIFNZU1RFTSAiaHR0cDovL2phdmEuc3VuLmNvbS9kdGQvcHJvcGVydGllcy5kdGQiPg0KPHByb3BlcnRpZXM+DQo8Y29tbWVudD5jb20ubDd0ZWNoLnBvbGljeS5leHBvcnRlci5Qb2xpY3lFeHBvcnRlclRlc3QkNSQzPC9jb21tZW50Pg0KPGVudHJ5IGtleT0iX19QUk9QRVJUWV9FTlRJVFlfQ0xBU1NfTkFNRV9fMjYxQjgwMjdfMjQ4N180ODU0X0EwQzVfMEQ1NDkwMDNDQkVGX18iPmNvbS5sN3RlY2gucG9saWN5LmV4cG9ydGVyLlBvbGljeUV4cG9ydGVyVGVzdCQ1JDM8L2VudHJ5Pg0KPGVudHJ5IGtleT0iUHJpdmF0ZUtleTEiPjAwMDAwMDAwMDAwMDAwMDIwMDAwMDAwMDAwMDAwMDAxOmFsaWFzMTwvZW50cnk+DQo8ZW50cnkga2V5PSJLZXlWYWx1ZVN0b3JlMiI+MDAwMDAwMDAwMDAwMDAwMzAwMDAwMDAwMDAwMDAwMDE8L2VudHJ5Pg0KPC9wcm9wZXJ0aWVzPg0K</Base64Value>" +
                        "      </Config>" +
                        "    </CustomKeyValueReference>" +
                        "    <IncludedPolicyReference RefType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\" guid=\"00000000000000040000000000000001\" included=\"true\" name=\"fragment1\" soap=\"false\" type=\"INCLUDE_FRAGMENT\">" +
                        "      <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                        "        <wsp:All wsp:Usage=\"Required\">" +
                        "          <L7p:CustomAssertion>" +
                        "            <L7p:base64SerializedValue>rO0ABXNyADFjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uSG9sZGVyZtcreFwddTICAAlaAAxpc1VpQXV0b09wZW5MAApjYXRlZ29yaWVzdAAPTGphdmEvdXRpbC9TZXQ7TAAIY2F0ZWdvcnl0ACpMY29tL2w3dGVjaC9wb2xpY3kvYXNzZXJ0aW9uL2V4dC9DYXRlZ29yeTtMAA9jdXN0b21Bc3NlcnRpb250ADFMY29tL2w3dGVjaC9wb2xpY3kvYXNzZXJ0aW9uL2V4dC9DdXN0b21Bc3NlcnRpb247TAAUY3VzdG9tTW9kdWxlRmlsZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAA9kZXNjcmlwdGlvblRleHRxAH4ABEwAD3BhbGV0dGVOb2RlTmFtZXEAfgAETAAOcG9saWN5Tm9kZU5hbWVxAH4ABEwAHnJlZ2lzdGVyZWRDdXN0b21GZWF0dXJlU2V0TmFtZXEAfgAEeHIAJWNvbS5sN3RlY2gucG9saWN5LmFzc2VydGlvbi5Bc3NlcnRpb27bX2OZPL2isQIAAloAB2VuYWJsZWRMABBhc3NlcnRpb25Db21tZW50dAAvTGNvbS9sN3RlY2gvcG9saWN5L2Fzc2VydGlvbi9Bc3NlcnRpb24kQ29tbWVudDt4cAFwAHNyABFqYXZhLnV0aWwuSGFzaFNldLpEhZWWuLc0AwAAeHB3DAAAABA/QAAAAAAAAXNyAChjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LkNhdGVnb3J5WrCcZaFE/jUCAAJJAAVteUtleUwABm15TmFtZXEAfgAEeHAAAAAAdAANQWNjZXNzQ29udHJvbHhwc3IAV2NvbS5sN3RlY2guZ2F0ZXdheS5jb21tb24uY3VzdG9tLkN1c3RvbUVudGl0aWVzVGVzdEJhc2UkVGVzdEN1c3RvbUFzc2VydGlvbldpdGhFbnRpdGllc00ED+1Xv9iyAgAAeHIAUWNvbS5sN3RlY2guZ2F0ZXdheS5jb21tb24uY3VzdG9tLkN1c3RvbUVudGl0aWVzVGVzdEJhc2UkVGVzdEN1c3RvbUFzc2VydGlvbkVudGl0ef0OBm72m1/HAgABTAAPZW50aXRpZXNTdXBwb3J0dABHTGNvbS9sN3RlY2gvcG9saWN5L2Fzc2VydGlvbi9leHQvZW50aXR5L0N1c3RvbVJlZmVyZW5jZUVudGl0aWVzU3VwcG9ydDt4cHNyAEVjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LmVudGl0eS5DdXN0b21SZWZlcmVuY2VFbnRpdGllc1N1cHBvcnTYlHneckWYyQIAAUwACnJlZmVyZW5jZXN0AA9MamF2YS91dGlsL01hcDt4cHNyABFqYXZhLnV0aWwuVHJlZU1hcAzB9j4tJWrmAwABTAAKY29tcGFyYXRvcnQAFkxqYXZhL3V0aWwvQ29tcGFyYXRvcjt4cHB3BAAAAAN0AA5LZXlWYWx1ZVN0b3JlMXNyAFZjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LmVudGl0eS5DdXN0b21SZWZlcmVuY2VFbnRpdGllc1N1cHBvcnQkUmVmZXJlbmNlRWxlbWVudOrozNdYjuW3AgAETAAZZW50aXR5U2VyaWFsaXplckNsYXNzTmFtZXEAfgAETAACaWRxAH4ABEwACWtleVByZWZpeHEAfgAETAAEdHlwZXEAfgAEeHB0ADljb20ubDd0ZWNoLmdhdGV3YXkuY29tbW9uLmN1c3RvbS5DdXN0b21FbnRpdGllc1Rlc3RCYXNlJDF0ACAwMDAwMDAwMDAwMDAwMDAzMDAwMDAwMDAwMDAwMDAwNHQAC3Rlc3QtcHJlZml4dAANS2V5VmFsdWVTdG9yZXQAC1ByaXZhdGVLZXkzc3EAfgAYcHQAJzAwMDAwMDAwMDAwMDAwMDIwMDAwMDAwMDAwMDAwMDAyOmFsaWFzMnB0AApQcml2YXRlS2V5dAAPU2VjdXJlUGFzc3dvcmQyc3EAfgAYcHQAIDAwMDAwMDAwMDAwMDAwMDEwMDAwMDAwMDAwMDAwMDAzcHQADlNlY3VyZVBhc3N3b3JkeHB0ACFUZXN0IEN1c3RvbSBBc3NlcnRpb24gaW4gRnJhZ21lbnRwcHA=</L7p:base64SerializedValue>" +
                        "          </L7p:CustomAssertion>" +
                        "        </wsp:All>" +
                        "      </wsp:Policy>" +
                        "    </IncludedPolicyReference>" +
                        "    <PrivateKeyReference RefType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\">" +
                        "      <IsDefaultKey>false</IsDefaultKey>" +
                        "      <KeystoreOID>00000000000000020000000000000002</KeystoreOID>" +
                        "      <KeyAlias>alias2</KeyAlias>" +
                        "    </PrivateKeyReference>" +
                        "    <StoredPasswordReference RefType=\"com.l7tech.console.policy.exporter.StoredPasswordReference\">" +
                        "      <Id>00000000000000010000000000000004</Id>" +
                        "      <Type>PASSWORD</Type>" +
                        "      <Description>Test Password for id: \"00000000000000010000000000000004\"</Description>" +
                        "    </StoredPasswordReference>" +
                        "    <StoredPasswordReference RefType=\"com.l7tech.console.policy.exporter.StoredPasswordReference\">" +
                        "      <Id>00000000000000010000000000000003</Id>" +
                        "      <Type>PASSWORD</Type>" +
                        "      <Description>Test Password for id: \"00000000000000010000000000000003\"</Description>" +
                        "    </StoredPasswordReference>" +
                        "    <CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">" +
                        "      <Config>" +
                        "        <KeyValueStoreName>internalTransactional</KeyValueStoreName>" +
                        "        <Key>00000000000000030000000000000004</Key>" +
                        "        <KeyPrefix>test-prefix</KeyPrefix>" +
                        "        <Base64Value>PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjwhRE9DVFlQRSBwcm9wZXJ0aWVzIFNZU1RFTSAiaHR0cDovL2phdmEuc3VuLmNvbS9kdGQvcHJvcGVydGllcy5kdGQiPg0KPHByb3BlcnRpZXM+DQo8Y29tbWVudD5jb20ubDd0ZWNoLnBvbGljeS5leHBvcnRlci5Qb2xpY3lFeHBvcnRlclRlc3QkNSQ0PC9jb21tZW50Pg0KPGVudHJ5IGtleT0iX19QUk9QRVJUWV9FTlRJVFlfQ0xBU1NfTkFNRV9fMjYxQjgwMjdfMjQ4N180ODU0X0EwQzVfMEQ1NDkwMDNDQkVGX18iPmNvbS5sN3RlY2gucG9saWN5LmV4cG9ydGVyLlBvbGljeUV4cG9ydGVyVGVzdCQ1JDQ8L2VudHJ5Pg0KPGVudHJ5IGtleT0iS2V5VmFsdWVTdG9yZTMiPjAwMDAwMDAwMDAwMDAwMDMwMDAwMDAwMDAwMDAwMDAxPC9lbnRyeT4NCjxlbnRyeSBrZXk9IlNlY3VyZVBhc3N3b3JkNCI+MDAwMDAwMDAwMDAwMDAwMTAwMDAwMDAwMDAwMDAwMDQ8L2VudHJ5Pg0KPGVudHJ5IGtleT0iU2VjdXJlUGFzc3dvcmQxIj4wMDAwMDAwMDAwMDAwMDAxMDAwMDAwMDAwMDAwMDAwMTwvZW50cnk+DQo8ZW50cnkga2V5PSJQcml2YXRlS2V5MiI+MDAwMDAwMDAwMDAwMDAwMjAwMDAwMDAwMDAwMDAwMDE6YWxpYXMxPC9lbnRyeT4NCjwvcHJvcGVydGllcz4NCg==</Base64Value>" +
                        "      </Config>" +
                        "    </CustomKeyValueReference>" +
                        "  </exp:References>" +
                        "  <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                        "    <wsp:All wsp:Usage=\"Required\">" +
                        "      <L7p:CustomAssertion>" +
                        "        <L7p:base64SerializedValue>rO0ABXNyADFjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uSG9sZGVyZtcreFwddTICAAlaAAxpc1VpQXV0b09wZW5MAApjYXRlZ29yaWVzdAAPTGphdmEvdXRpbC9TZXQ7TAAIY2F0ZWdvcnl0ACpMY29tL2w3dGVjaC9wb2xpY3kvYXNzZXJ0aW9uL2V4dC9DYXRlZ29yeTtMAA9jdXN0b21Bc3NlcnRpb250ADFMY29tL2w3dGVjaC9wb2xpY3kvYXNzZXJ0aW9uL2V4dC9DdXN0b21Bc3NlcnRpb247TAAUY3VzdG9tTW9kdWxlRmlsZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAA9kZXNjcmlwdGlvblRleHRxAH4ABEwAD3BhbGV0dGVOb2RlTmFtZXEAfgAETAAOcG9saWN5Tm9kZU5hbWVxAH4ABEwAHnJlZ2lzdGVyZWRDdXN0b21GZWF0dXJlU2V0TmFtZXEAfgAEeHIAJWNvbS5sN3RlY2gucG9saWN5LmFzc2VydGlvbi5Bc3NlcnRpb27bX2OZPL2isQIAAloAB2VuYWJsZWRMABBhc3NlcnRpb25Db21tZW50dAAvTGNvbS9sN3RlY2gvcG9saWN5L2Fzc2VydGlvbi9Bc3NlcnRpb24kQ29tbWVudDt4cAFwAHNyABFqYXZhLnV0aWwuSGFzaFNldLpEhZWWuLc0AwAAeHB3DAAAABA/QAAAAAAAAXNyAChjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LkNhdGVnb3J5WrCcZaFE/jUCAAJJAAVteUtleUwABm15TmFtZXEAfgAEeHAAAAAAdAANQWNjZXNzQ29udHJvbHhwc3IAV2NvbS5sN3RlY2guZ2F0ZXdheS5jb21tb24uY3VzdG9tLkN1c3RvbUVudGl0aWVzVGVzdEJhc2UkVGVzdEN1c3RvbUFzc2VydGlvbldpdGhFbnRpdGllc00ED+1Xv9iyAgAAeHIAUWNvbS5sN3RlY2guZ2F0ZXdheS5jb21tb24uY3VzdG9tLkN1c3RvbUVudGl0aWVzVGVzdEJhc2UkVGVzdEN1c3RvbUFzc2VydGlvbkVudGl0ef0OBm72m1/HAgABTAAPZW50aXRpZXNTdXBwb3J0dABHTGNvbS9sN3RlY2gvcG9saWN5L2Fzc2VydGlvbi9leHQvZW50aXR5L0N1c3RvbVJlZmVyZW5jZUVudGl0aWVzU3VwcG9ydDt4cHNyAEVjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LmVudGl0eS5DdXN0b21SZWZlcmVuY2VFbnRpdGllc1N1cHBvcnTYlHneckWYyQIAAUwACnJlZmVyZW5jZXN0AA9MamF2YS91dGlsL01hcDt4cHNyABFqYXZhLnV0aWwuVHJlZU1hcAzB9j4tJWrmAwABTAAKY29tcGFyYXRvcnQAFkxqYXZhL3V0aWwvQ29tcGFyYXRvcjt4cHB3BAAAAAF0AA5LZXlWYWx1ZVN0b3JlMXNyAFZjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LmVudGl0eS5DdXN0b21SZWZlcmVuY2VFbnRpdGllc1N1cHBvcnQkUmVmZXJlbmNlRWxlbWVudOrozNdYjuW3AgAETAAZZW50aXR5U2VyaWFsaXplckNsYXNzTmFtZXEAfgAETAACaWRxAH4ABEwACWtleVByZWZpeHEAfgAETAAEdHlwZXEAfgAEeHB0ADljb20ubDd0ZWNoLmdhdGV3YXkuY29tbW9uLmN1c3RvbS5DdXN0b21FbnRpdGllc1Rlc3RCYXNlJDF0ACAwMDAwMDAwMDAwMDAwMDAzMDAwMDAwMDAwMDAwMDAwMXQAC3Rlc3QtcHJlZml4dAANS2V5VmFsdWVTdG9yZXhwdAAVVGVzdCBDdXN0b20gQXNzZXJ0aW9ucHBw</L7p:base64SerializedValue>" +
                        "      </L7p:CustomAssertion>" +
                        "      <L7p:Include>" +
                        "        <L7p:PolicyGuid stringValue=\"00000000000000040000000000000001\"/>" +
                        "      </L7p:Include>" +
                        "    </wsp:All>" +
                        "  </wsp:Policy>" +
                        "</exp:Export>"
        );
        exportedPolicy.normalizeDocument();

        assertNotNull(exportedPolicy);
        final Element referencesElement = DomUtils.findOnlyOneChildElementByName(exportedPolicy.getDocumentElement(),
                ExporterConstants.EXPORTED_POL_NS,
                ExporterConstants.EXPORTED_REFERENCES_ELNAME
        );
        assertNotNull(referencesElement);

        /////////////////////////////////////////////////////////////////////////////////////////////////////
        // Expected output:
        // PK1, PK2, SP1, SP2, SP3, SP4, KVS1, KVS2, KVS3, KVS4
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        final Set<CustomEntityReferencesComparator> expectedReferences = new HashSet<>();
        expectedReferences.add(toPrivateKeyReference(privateKeyId(1)));
        expectedReferences.add(toPrivateKeyReference(privateKeyId(2)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(1)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(2)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(3)));
        expectedReferences.add(toStoredPasswordReference(securePasswordId(4)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(1)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(2)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(3)));
        expectedReferences.add(toKeyValueStoreReference(keyValueStoreId(4)));
        // add custom assertion reference
        expectedReferences.add(new CustomEntityReferencesComparator(new CustomAssertionReference(finder, "Test Custom Assertion referencing entities")));
        // add policy fragment reference
        expectedReferences.add(new CustomEntityReferencesComparator(new IncludedPolicyReference(finder, new Include(policyFragmentId(1)))));

        // parse references
        final Collection<ExternalReference> refs = ExternalReference.parseReferences(finder, null, finder.findAllExternalReferenceFactories(), referencesElement);
        assertEquals("References count", expectedReferences.size(), refs.size());

        // test each reference
        for (final ExternalReference ref : refs) {
            System.out.println("Found ref of type " + ref.getRefType());
            assertTrue("Unexpected reference: " + toString(ref), expectedReferences.remove(new CustomEntityReferencesComparator(ref)));
        }
        // make sure no elements are left unprocessed
        assertTrue(expectedReferences.isEmpty());
    }

    private String toString(final ExternalReference ref) {
        if (ref instanceof PrivateKeyReference) {
            final PrivateKeyReference pkRef = (PrivateKeyReference)ref;
            return "PrivateKeyReference [Alias: \"" + pkRef.getKeyAlias() + "\" Id: \"" + pkRef.getKeystoreGoid().toString() + "\"]";
        } else if (ref instanceof StoredPasswordReference) {
            final StoredPasswordReference spRef = (StoredPasswordReference)ref;
            return "StoredPasswordReference [Id: \"" + spRef.getId().toString() + "\" Name: \"" + spRef.getName()  + "\" Description: \"" + spRef.getDescription() + "\"]";
        } else if (ref instanceof CustomKeyValueReference) {
            final CustomKeyValueReference kvsRef = (CustomKeyValueReference)ref;
            return "CustomKeyValueReference [\nKey: \"" + kvsRef.getEntityKey() + "\"\nPrefix: \"" + kvsRef.getEntityKeyPrefix()  + "\"\nBase64: \"" + kvsRef.getEntityBase64Value() + "\"\nSerializer: \"" + kvsRef.getEntitySerializer() + "\"\n]";
        } else if (ref instanceof IncludedPolicyReference) {
            final IncludedPolicyReference fragRef = (IncludedPolicyReference)ref;
            return "IncludedPolicyReference [\nGoid: \"" + fragRef.getGuid() + "\"\nName: \"" + fragRef.getName()  + "\"\nType: \"" + fragRef.getType() + "\"\nisSoap: \"" + fragRef.isSoap() + "\"\ninternalTag: \"" + fragRef.getInternalTag() + "\"\nuseType: \"" + fragRef.getUseType() + "\"\nxml: \"" + fragRef.getXml() + "\"\n]";
        }
        return ref.toString();
    }

    private CustomEntityReferencesComparator toKeyValueStoreReference(final String keyValueStoreId) {
        if (keyValueStoreId == null) { throw new IllegalArgumentException("keyValueStoreId cannot be null"); }
        return new CustomEntityReferencesComparator(
                new CustomKeyValueReference(
                        finder,
                        new CustomKeyStoreEntityHeader(
                                keyValueStoreId,
                                KEY_VALUE_STORE_KEY_PREFIX,
                                keyValueStore.get(keyValueStoreId),
                                createEntitySerializer().deserialize(keyValueStore.get(keyValueStoreId)) instanceof CustomEntityDescriptor
                                        ? createEntitySerializer().getClass().getName()
                                        : null
                        )
                )
        );
    }

    private CustomEntityReferencesComparator toStoredPasswordReference(final String passwordId) {
        if (passwordId == null) { throw new IllegalArgumentException("passwordId cannot be null"); }
        return new CustomEntityReferencesComparator(
                new StoredPasswordReference(
                        finder,
                        new SecurePasswordEntityHeader(
                                Goid.parseGoid(passwordId),
                                EntityType.SECURE_PASSWORD,
                                null,
                                (passwordId.equals(securePasswordId(2)) ? "Test PEM for id: \"" : "Test Password for id: \"") + passwordId + "\"",
                                passwordId.equals(securePasswordId(2)) ? SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY.name() : SecurePassword.SecurePasswordType.PASSWORD.name()
                        )
                )
        );
    }

    private CustomEntityReferencesComparator toPrivateKeyReference(final String privateKeyId) {
        if (privateKeyId == null) { throw new IllegalArgumentException("privateKeyId cannot be null"); }
        return new CustomEntityReferencesComparator(
                new PrivateKeyReference(
                        finder,
                        false,  // we do not support default keys
                        getPrivateKeyGoidAliasPair(privateKeyId).left,
                        getPrivateKeyGoidAliasPair(privateKeyId).right
                )
        );
    }
}