package com.l7tech.identity.fed;

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

    /**
     * Test <code>FederatedIdentityProviderTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }

    private FederatedIdentityProviderConfig config;
}