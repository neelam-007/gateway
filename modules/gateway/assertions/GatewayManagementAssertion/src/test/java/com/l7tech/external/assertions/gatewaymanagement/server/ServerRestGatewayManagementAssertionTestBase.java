package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.identity.TestIdentityProviderConfigManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapperTestUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Test the GatewayManagementAssertion for EmailListenerMO entity.
 */
@Ignore
public abstract class ServerRestGatewayManagementAssertionTestBase {

    protected static ClassPathXmlApplicationContext applicationContext;
    @InjectMocks
    protected ServerRESTGatewayManagementAssertion restManagementAssertion;
    //- PRIVATE
    protected static IdentityProviderConfig identityProviderConfig;


    @Spy
    StashManagerFactory stashManagerFactory = new StashManagerFactory() {
        @Override
        public StashManager createStashManager() {
            return new ByteArrayStashManager();
        }
    };

    @Before
    public void before() throws Exception {
        restManagementAssertion = new ServerRESTGatewayManagementAssertion(new RESTGatewayManagementAssertion(), applicationContext, "testGatewayManagementContext.xml", false );

        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    @SuppressWarnings({"serial"})
    public static void beforeClass() throws Exception {
        new AssertionRegistry(); // causes type mappings to be installed for assertions

        applicationContext = (ClassPathXmlApplicationContext) ApplicationContexts.getTestApplicationContext();

        GoidUpgradeMapperTestUtil.addPrefix("keystore_file", 0);

        TestIdentityProviderConfigManager identityProviderConfigManager = applicationContext.getBean("identityProviderConfigManager", TestIdentityProviderConfigManager.class);
        identityProviderConfig = TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG;
        identityProviderConfigManager.save(identityProviderConfig);
    }

    protected static IdentityProviderConfig provider( final Goid oid, final IdentityProviderType type, final String name, String... props ) {
        final IdentityProviderConfig provider = type == IdentityProviderType.LDAP ? new LdapIdentityProviderConfig() : new IdentityProviderConfig( type );
        provider.setGoid(oid);
        provider.setName( name );
        if (props != null && props.length > 0) {
            int numprops = props.length / 2;
            if (props.length != numprops * 2)
                throw new IllegalArgumentException("An even number of strings must be provided (to be interpreted as test property name,value pairs)");

            for (int i = 0; i < props.length; i+=2) {
                String prop = props[i];
                String val = props[i + 1];
                if ("userLookupByCertMode".equals(prop)) {
                    ((LdapIdentityProviderConfig)provider).setUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode.valueOf(val));
                } else {
                    throw new IllegalArgumentException("Unsupported test idp property: " + prop);
                }
            }
        }

        if(type == IdentityProviderType.LDAP){
            LdapIdentityProviderConfig ldap = (LdapIdentityProviderConfig)provider;
            Map<String,String> ntlmProps = new TreeMap<String,String>();
            ntlmProps.put("prop","val");
            ntlmProps.put("prop1","val1");
            ldap.setNtlmAuthenticationProviderProperties(ntlmProps);
        }
        return provider;
    }

    protected Response processRequest( String uri, HttpMethod method, String contentType, String body ) throws Exception {
        final ContentTypeHeader contentTypeHeader = contentType==null?ContentTypeHeader.OCTET_STREAM_DEFAULT:ContentTypeHeader.parseValue(contentType);
        final Message request = new Message();
        request.initialize( contentTypeHeader , body.getBytes( "utf-8" ));
        final Message response = new Message();

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
        final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        servletContext.setContextPath( "/" );

        httpServletRequest.setMethod(method.getProtocolName());
        if(contentType!=null){
            httpServletRequest.setContentType(contentType);
            httpServletRequest.addHeader("Content-Type", contentType);
        }
        httpServletRequest.setRemoteAddr("127.0.0.1");
        httpServletRequest.setServerName( "127.0.0.1" );
        httpServletRequest.setRequestURI("/restman/"+uri);
        httpServletRequest.setContent(body.getBytes("UTF-8"));

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

            final PublishedService service = new PublishedService();
            service.setRoutingUri("/restman*");
            context.setService(service);

            AssertionStatus assertionStatus = restManagementAssertion.checkRequest(context);

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), bout);
            String responseBody = bout.toString("UTF-8");
            HashMap<String, String[]> headers = new HashMap<>();
            for(String header : response.getHttpResponseKnob().getHeaderNames()){
                headers.put(header, response.getHttpResponseKnob().getHeaderValues(header));
            }
            return new Response(assertionStatus, responseBody, response.getHttpResponseKnob().getStatus(), headers);
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    @After
    public void after() throws Exception {}

    protected class Response {
        private String body;
        private int status;
        private Map<String,String[]> headers;
        private AssertionStatus assertionStatus;

        private Response(AssertionStatus assertionStatus, String body, int status, Map<String,String[]> headers){
            this.assertionStatus = assertionStatus;
            this.body = body;
            this.status = status;
            this.headers = headers;
        }

        public String getBody() {
            return body;
        }

        public int getStatus() {
            return status;
        }

        public String toString(){
            return "Status: " + status + " headers: " + printHeaders(headers) + " Body:\n" + body;
        }

        private String printHeaders(Map<String, String[]> headers) {
            return new StringBuilder(Functions.reduce(headers.entrySet(), "{", new Functions.Binary<String, String, Map.Entry<String, String[]>>() {
                @Override
                public String call(String s, Map.Entry<String, String[]> header) {
                    return s + header.getKey() + "=" + Arrays.asList(header.getValue()).toString() + ", ";
                }
            })) + "}";
        }

        public AssertionStatus getAssertionStatus() {
            return assertionStatus;
        }

        public Map<String, String[]> getHeaders() {
            return headers;
        }
    }

}
