package com.l7tech.identity.fed;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.SsgAdminSession;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedIdentityProviderTest extends TestCase {
    /**
     * test <code>FederatedIdentityProviderTest</code> constructor
     */
    public FederatedIdentityProviderTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the FederatedIdentityProviderTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( FederatedIdentityProviderTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        this.config = new FederatedIdentityProviderConfig();
        config.setX509Supported(true);
        config.setSamlSupported(true);
        config.setName("Example FIP");
        SamlConfig saml = config.getSamlConfig();
        saml.setNameQualifier("www.example.com");
        List configs = new ArrayList();
        SamlConfig.AttributeStatementConfig att = saml.new AttributeStatementConfig();
        att.setName("foo");
        att.setNamespaceUri("urn:example.com:foo");
        att.setValues(new String[] { "bar", "baz" });
        configs.add(att);
        saml.setAttributeStatementConfigs(configs);
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testCreateConfig() throws Exception {
        SamlConfig saml = config.getSamlConfig();
        assertNotNull(saml);
        System.err.println(saml);

        X509Config x509 = config.getX509Config();
        assertNotNull(x509);
        System.err.println(x509);
    }

    public void testSaveConfig() throws Exception {
        final long oid = ((Long)new SsgAdminSession() {
            protected Object doSomething() throws Exception {
                return new Long(getIdentityAdmin().saveIdentityProviderConfig(config));
            }
        }.doIt()).longValue();;

        System.err.println("Saved Federated IDPC #" + oid);

        FederatedIdentityProviderConfig conf = (FederatedIdentityProviderConfig)new SsgAdminSession() {
            protected Object doSomething() throws Exception {
                return getIdentityAdmin().findIdentityProviderConfigByPrimaryKey(oid);
            }
        }.doIt();

        assertNotNull(conf);
        System.err.println("Loaded FIPC " + conf);

        new SsgAdminSession() {
            protected Object doSomething() throws Exception {
                getIdentityAdmin().deleteIdentityProviderConfig(oid);
                return null;
            }
        }.doIt();
    }

    public void testSaveVirtualGroup() throws Exception {
        final IdentityProviderConfig config = (IdentityProviderConfig) new SsgAdminSession() {
            protected Object doSomething() throws Exception {
                EntityHeader[] configs = getIdentityAdmin().findAllIdentityProviderConfig();
                for ( int i = 0; i < configs.length; i++ ) {
                    EntityHeader entityHeader = configs[i];
                    if ( entityHeader.getType() == EntityType.ID_PROVIDER_CONFIG ) {
                        IdentityProviderConfig config = getIdentityAdmin().findIdentityProviderConfigByPrimaryKey(entityHeader.getOid());
                        if ( config.type() == IdentityProviderType.FEDERATED ) return config;
                    }
                }
                return null;
            }
        }.doIt();

        assertNotNull("There must already be a Federated Identity Provider", config);

        final VirtualGroup vg = new VirtualGroup();
        vg.setName("CN is anything");
        vg.setX509SubjectDnPattern("CN=*");
        String soid = (String)new SsgAdminSession() {
            protected Object doSomething() throws Exception {
                return getIdentityAdmin().saveGroup(config.getOid(), vg, null);
            }
        }.doIt();

        assertNotNull("Couldn't save virtual group",soid);
    }

    /**
     * Test <code>FederatedIdentityProviderTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }

    private FederatedIdentityProviderConfig config;
}