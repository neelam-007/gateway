package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.UserBean;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.PolicyValidatorStub;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.AssertionAccessManagerStub;
import com.l7tech.server.EntityFinderStub;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.encass.EncapsulatedAssertionConfigManagerStub;
import com.l7tech.server.entity.GenericEntityManagerStub;
import com.l7tech.server.export.PolicyExporterImporterManagerStub;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.globalresources.HttpConfigurationManagerStub;
import com.l7tech.server.globalresources.ResourceEntryManagerStub;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.TestIdentityProviderConfigManager;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.jdbc.JdbcConnectionManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.policy.PolicyVersionManagerStub;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
import com.l7tech.server.security.rbac.MockRoleManager;
import com.l7tech.server.security.rbac.RbacServicesStub;
import com.l7tech.server.security.rbac.SecurityZoneManagerStub;
import com.l7tech.server.service.PolicyAliasManagerStub;
import com.l7tech.server.service.ServiceAliasManagerStub;
import com.l7tech.server.service.ServiceDocumentManagerStub;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import com.l7tech.server.store.CustomKeyValueStoreManagerStub;
import com.l7tech.server.transport.SsgActiveConnectorManagerStub;
import com.l7tech.server.transport.email.EmailListenerManagerStub;
import com.l7tech.server.transport.jms.JmsConnectionManagerStub;
import com.l7tech.server.transport.jms.JmsEndpointManagerStub;
import com.l7tech.server.uddi.ServiceWsdlUpdateChecker;
import com.l7tech.server.util.ResourceClassLoader;
import com.l7tech.util.*;
import com.l7tech.util.Functions.UnaryVoidThrows;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the GatewayManagementAssertion for EmailListenerMO entity.
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ServerGatewayManagementAssertionTestBase {

    protected final GenericApplicationContext applicationContext = new GenericApplicationContext();
    protected ServerGatewayManagementAssertion managementAssertion;
    protected ServerRESTGatewayManagementAssertion restManagementAssertion;
    protected static final PolicyValidatorStub policyValidator = new PolicyValidatorStub();
    protected RbacServicesStub rbacService;
    @Mock
    protected TestIdentityProviderConfigManager identityProviderConfigManager =  new TestIdentityProviderConfigManager();
    protected static final String NS_WS_TRANSFER = "http://schemas.xmlsoap.org/ws/2004/09/transfer";
    protected static final String NS_WS_ADDRESSING = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    protected static final String NS_WS_ENUMERATION = "http://schemas.xmlsoap.org/ws/2004/09/enumeration";
    protected static final String NS_WS_MANAGEMENT = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd";
    protected static final String NS_WS_MANAGEMENT_IDENTITY = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd";
    protected static final String NS_GATEWAY_MANAGEMENT = "http://ns.l7tech.com/2010/04/gateway-management";
    protected static final String NS_SOAP_ENVELOPE = "http://www.w3.org/2003/05/soap-envelope";
    protected static final String WSDL = "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>";
    protected static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";

    //- PRIVATE

    @Before
    @SuppressWarnings({"serial"})
    public void init() throws Exception {
        new AssertionRegistry(); // causes type mappings to be installed for assertions

        final ClusterPropertyManager clusterPropertyManager = new MockClusterPropertyManager();
        applicationContext.getBeanFactory().registerSingleton( "serverConfig", new MockConfig( new Properties() ) );
        applicationContext.getBeanFactory().registerSingleton( "trustedCertManager", new TestTrustedCertManager());
        applicationContext.getBeanFactory().registerSingleton( "clusterPropertyCache", new ClusterPropertyCache(){{ setClusterPropertyManager( clusterPropertyManager ); }});
        applicationContext.getBeanFactory().registerSingleton( "clusterPropertyManager", clusterPropertyManager);
        applicationContext.getBeanFactory().registerSingleton( "resourceEntryManager", new ResourceEntryManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "folderManager", new FolderManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "identityProviderConfigManager", identityProviderConfigManager );
        applicationContext.getBeanFactory().registerSingleton( "identityProviderFactory", new IdentityProviderFactory(identityProviderConfigManager));
        applicationContext.getBeanFactory().registerSingleton( "jmsConnectionManager",  new JmsConnectionManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "jmsEndpointManager",  new JmsEndpointManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "jdbcConnectionManager", new JdbcConnectionManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "ssgActiveConnectorManager", new SsgActiveConnectorManagerStub() );
        applicationContext.getBeanFactory().registerSingleton( "policyExporterImporterManager", new PolicyExporterImporterManagerStub() );
        applicationContext.getBeanFactory().registerSingleton( "policyManager",  new PolicyManagerStub());
        applicationContext.getBeanFactory().registerSingleton("ssgKeyStoreManager", new SsgKeyStoreManagerStub(new SsgKeyFinderStub()));
        rbacService = new RbacServicesStub();
        applicationContext.getBeanFactory().registerSingleton("rbacServices", rbacService);
        applicationContext.getBeanFactory().registerSingleton( "securityFilter", rbacService );
        applicationContext.getBeanFactory().registerSingleton( "serviceDocumentManager", new ServiceDocumentManagerStub() );
        applicationContext.getBeanFactory().registerSingleton( "serviceManager", new MockServiceManager(service( new Goid(0,1L), "Test Service 1", false, false, null, null)));
        applicationContext.getBeanFactory().registerSingleton( "policyValidator", policyValidator );
        applicationContext.getBeanFactory().registerSingleton( "uddiServiceWsdlUpdateChecker", new ServiceWsdlUpdateChecker(null, null){
            @Override
            public boolean isWsdlUpdatePermitted( final PublishedService service, final boolean resetWsdlXml ) throws UpdateException {
                return true;
            }
        } );
        applicationContext.getBeanFactory().registerSingleton( "securePasswordManager", new SecurePasswordManagerStub(
                securePassword(new Goid(0,1L), "test", "password", true, SecurePassword.SecurePasswordType.PASSWORD)
        ) );
        applicationContext.getBeanFactory().registerSingleton( "encapsulatedAssertionConfigManager", new EncapsulatedAssertionConfigManagerStub() );
        applicationContext.getBeanFactory().registerSingleton( "httpConfigurationManager", new HttpConfigurationManagerStub() );
        applicationContext.getBeanFactory().registerSingleton( "roleManager", new MockRoleManager(null) );
        applicationContext.getBeanFactory().registerSingleton("genericEntityManager",new GenericEntityManagerStub() );
        applicationContext.getBeanFactory().registerSingleton("customKeyValueStoreManager", new CustomKeyValueStoreManagerStub() );

        final SecurityZone securityZone1 = new SecurityZone();
        securityZone1.setGoid(new Goid(0,2));
        securityZone1.setName("Test Security Zone 0002");
        securityZone1.setDescription("Canned Testing Security Zone 0002");
        securityZone1.getPermittedEntityTypes().add(EntityType.ANY);
        applicationContext.getBeanFactory().registerSingleton("securityZoneManager", new SecurityZoneManagerStub(securityZone1));

        applicationContext.getBeanFactory().registerSingleton("siteMinderConfigurationManager", new SiteMinderConfigurationManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "entityCrud", new EntityFinderStub() );
        applicationContext.getBeanFactory().registerSingleton("assertionAccessManager", new AssertionAccessManagerStub());
        applicationContext.getBeanFactory().registerSingleton("policyAliasManager", new PolicyAliasManagerStub());
        applicationContext.getBeanFactory().registerSingleton("serviceAliasManager", new ServiceAliasManagerStub());
        applicationContext.getBeanFactory().registerSingleton("emailListenerManager", new EmailListenerManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "dependencyAnalyzer", new DependencyAnalyzerImpl());
        applicationContext.getBeanFactory().registerSingleton( "policyVersionManager", new PolicyVersionManagerStub());

        moreInit();

        applicationContext.refresh();

        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        final ResourceClassLoader resourceClassLoader = new ResourceClassLoader(
                new FilterClassLoader(systemClassLoader, systemClassLoader,
                        Arrays.asList("com.l7tech.gateway.common.service.PublishedService$DefaultWsdlStrategy"), true),
                Arrays.asList("com.l7tech.gateway.common.service.PublishedService$DefaultWsdlStrategy")){

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                final Enumeration<URL> resources = super.getResources(name);
                final List<URL> urls = new ArrayList<URL>();
                while (resources.hasMoreElements()) {
                    final URL url = resources.nextElement();
                    if (!url.toString().contains("console")) {
                         urls.add(url);
                    }
                }

                return new Enumeration<URL>() {
                    private int index = 0;
                    @Override
                    public boolean hasMoreElements() {
                        return index != urls.size();
                    }

                    @Override
                    public URL nextElement() {
                        return urls.get(index++);
                    }
                };
            }
        };

        Thread.currentThread().setContextClassLoader(resourceClassLoader);
        managementAssertion = new ServerGatewayManagementAssertion(
                new GatewayManagementAssertion(), applicationContext, "testGatewayManagementContext.xml", false );

        restManagementAssertion = new ServerRESTGatewayManagementAssertion(new RESTGatewayManagementAssertion(), applicationContext, "testGatewayManagementContext.xml", false );

        GoidUpgradeMapperTestUtil.addPrefix("keystore_file", 0);

    }

    protected void moreInit() throws SaveException {}

    protected static PublishedService service( final Goid goid, final String name, final boolean disabled, final boolean soap, final String wsdlUrl, final String wsdlXml ) {
        final PublishedService service = new PublishedService();
        service.setGoid(goid);
        service.setName( name );
        service.getPolicy().setName( "Policy for " + name + " (#" + goid + ")" );
        service.getPolicy().setGuid( UUID.randomUUID().toString() );
        service.getPolicy().setXml( POLICY );
        service.setDisabled( disabled );
        service.setSoap( soap );
        try {
            service.setWsdlUrl( wsdlUrl );
        } catch ( MalformedURLException e ) {
            throw ExceptionUtils.wrap( e );
        }
        service.setWsdlXml( wsdlXml );
        return service;
    }

    protected static SecurePassword securePassword( final Goid goid, final String name, final String password, final boolean fromVariable, final SecurePassword.SecurePasswordType type) {
        final SecurePassword securePassword = new SecurePassword();
        securePassword.setGoid(goid);
        securePassword.setName( name );
        securePassword.setEncodedPassword( password );
        securePassword.setUsageFromVariable( fromVariable );
        securePassword.setLastUpdate( System.currentTimeMillis() );
        securePassword.setType(type);
        return securePassword;
    }

    protected void doCreate( final String resourceUri,
                           final String payload,
                           final String... expectedIds ) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsman:OperationTimeout>PT600.000S</wsman:OperationTimeout><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element transferResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_TRANSFER, "ResourceCreated");
        final Element refParameters = XmlUtil.findExactlyOneChildElementByName(transferResponse, NS_WS_ADDRESSING, "ReferenceParameters");
        final Element selectorSet = XmlUtil.findExactlyOneChildElementByName(refParameters, NS_WS_MANAGEMENT, "SelectorSet");
        final Element selector = XmlUtil.findExactlyOneChildElementByName(selectorSet, NS_WS_MANAGEMENT, "Selector");

        assertTrue("Identifier in " + Arrays.asList(expectedIds) + ": " + XmlUtil.getTextValue(selector), ArrayUtils.containsIgnoreCase(expectedIds, XmlUtil.getTextValue(selector)));
    }

    protected void doCreateFail( final String resourceUri,
                               final String payload,
                               final String faultSubcode ) throws Exception {

        doCreateFail(resourceUri, payload, faultSubcode,null);
    }

    protected void doCreateFail( final String resourceUri,
                                  final String payload,
                                  final String faultSubcode,
                                  final String faultDetailTextContains) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element codeElement = XmlUtil.findExactlyOneChildElementByName(faultElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcodeElement = XmlUtil.findExactlyOneChildElementByName(codeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element valueElement = XmlUtil.findExactlyOneChildElementByName(subcodeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        final String code = XmlUtil.getTextValue( valueElement );

        assertEquals("SOAP Fault subcode", faultSubcode, code);

        if(faultDetailTextContains!=null){
            final Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Detail");
            final Element textElement = XmlUtil.findExactlyOneChildElementByName(detailElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Text");
            final String detailText = XmlUtil.getTextValue( textElement );
            assertTrue("SOAP Fault Detail Text contains" + faultDetailTextContains, detailText.contains(faultDetailTextContains));
        }
    }

    protected void putAndVerify( final String message,
                               final UnaryVoidThrows<Document,Exception> verifier,
                               final boolean removeVersionAndId ) throws Exception {
        final String messageToPut;
        if ( removeVersionAndId ) {
            // ensures that id and version attributes are optional for Puts
            messageToPut = message
                    .replaceAll( " id=\"[a-zA-Z0-9:]+\"", "" )
                    .replaceAll( " version=\"[0-9]+\"", "" );
        } else {
            messageToPut = message;
        }

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", messageToPut);
        verifier.call(result);
    }

    protected Document processRequest( final String action,
                                     final String message ) throws Exception {
        System.out.println( XmlUtil.nodeToFormattedString(XmlUtil.parse( message )) );

        final String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() + "; action=\""+action+"\"";
        final Message request = new Message();
        request.initialize( ContentTypeHeader.parseValue(contentType) , message.getBytes( "utf-8" ));
        final Message response = new Message();

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
        final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        servletContext.setContextPath( "/" );

        httpServletRequest.setMethod("POST");
        httpServletRequest.setContentType(contentType);
        httpServletRequest.addHeader("Content-Type", contentType);
        httpServletRequest.setRemoteAddr("127.0.0.1");
        httpServletRequest.setServerName( "127.0.0.1" );
        httpServletRequest.setRequestURI("/wsman");
        httpServletRequest.setContent(message.getBytes("UTF-8"));

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(httpServletResponse);
        response.attachHttpResponseKnob(respKnob);

        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

            // fake user authentication
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult(
                    new UserBean("admin"),
                    new HttpBasicToken("admin", "".toCharArray()), null, false)
            );

            managementAssertion.checkRequest( context );

            Document responseDoc = response.getXmlKnob().getDocumentReadOnly();

            System.out.println( XmlUtil.nodeToFormattedString(responseDoc) );

            return responseDoc;
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    protected String getPropertyValue( final Element propertiesElement,
                                     final String propertyName ) {
        String value = null;

        for ( final Element propertyElement : XmlUtil.findChildElementsByName( propertiesElement, NS_GATEWAY_MANAGEMENT, "Property" ) ) {
            if ( propertyName.equals(propertyElement.getAttributeNS( null, "key" )) ) {
                final Element valueElement = XmlUtil.findFirstChildElement( propertyElement );
                if ( valueElement != null ) {
                    value = XmlUtil.getTextValue( valueElement );
                }
                break;
            }
        }

        return value;
    }

    protected static class MockServiceManager extends EntityManagerStub<PublishedService,ServiceHeader> implements ServiceManager {
        MockServiceManager( final PublishedService... entitiesIn ) {
            super( entitiesIn );
        }

        @Override
        public String resolveWsdlTarget( final String url ) {
            return null;
        }

        @Override
        public void addManageServiceRole( final PublishedService service ) throws SaveException {
            throw new SaveException("Not implemented");
        }

        @Override
        public Collection<ServiceHeader> findAllHeaders( final boolean includeAliases ) throws FindException {
            throw new FindException("Not implemented");
        }

        @Override
        public Collection<PublishedService> findByRoutingUri(String routingUri) throws FindException {
            throw new FindException("Not implemented");
        }
    }

}
