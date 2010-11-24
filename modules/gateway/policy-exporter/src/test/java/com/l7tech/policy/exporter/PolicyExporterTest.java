package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.util.DomUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import org.junit.Test;

import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Test class for the policy exporter.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 */
public class PolicyExporterTest {

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
        assertEquals("ID provider reference type", "com.l7tech.console.policy.exporter.JMSEndpointReference", jmsConnRefEle.getAttribute( "RefType" ));
    }

    @Test
    public void testParseReferences() throws Exception {
        Document exportedPolicy = exportToDocument();
        Element referencesEl = DomUtils.findOnlyOneChildElementByName(exportedPolicy.getDocumentElement(),
                                                                     ExporterConstants.EXPORTED_POL_NS,
                                                                     ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        Collection<ExternalReference> refs = ExternalReference.parseReferences(getExternalReferenceFinder(), null, referencesEl);
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
        assertEquals("Reference count", 9, referenceTypes.size());
    }

    private Document exportToDocument() throws Exception {
        PolicyExporter exporter = new PolicyExporter(getExternalReferenceFinder(), null);
        Assertion testPolicy = createTestPolicy();
        return exporter.exportToDocument(testPolicy);
    }

    private Assertion createTestPolicy() {
        AllAssertion root = new AllAssertion();

        // CustomAssertionReference
        CustomAssertionHolder cahass = new CustomAssertionHolder();
        cahass.setCategory(Category.MESSAGE);
        cahass.setCustomAssertion(new TestCustomAssertion());
        root.getChildren().add(cahass);

        // External Schema Reference
        SchemaValidation svass = new SchemaValidation();
        GlobalResourceInfo info = new GlobalResourceInfo();
        info.setId("test.xsd");
        svass.setResourceInfo( info );
        root.addChild( svass );

        // FederatedIdProviderReference
        SpecificUser suassf = new SpecificUser(1234, "feduser", null, null);
        root.addChild(suassf);        

        // IdProviderReference
        SpecificUser suass = new SpecificUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "john", null, null);
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
        wsass.setRecipientTrustedCertificateOid( 1234 );
        root.addChild( wsass );

        return root;
    }

    private ExternalReferenceFinder getExternalReferenceFinder() {
        return new ExternalReferenceFinderStub(){
            @Override
            public IdentityProviderConfig findIdentityProviderConfigByID( final long providerOid ) throws FindException {
                IdentityProviderConfig config;
                if ( providerOid == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID ) {
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
        };
    }

    private static final class TestCustomAssertion implements CustomAssertion {
        @Override
        public String getName() {
            return "Test Custom Assertion";
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
}
