package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.external.assertions.whichmodule.GenericEntityManagerDemoAssertion;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.http.DefaultHttpConnectors;
import com.l7tech.util.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;

/**
 * This will bring up an entire database backed rest management assertion environment.
 * In order to run any tests locally that use this Class you must first run
 * ./build.sh clean dev -Dproject.module.excludes=""
 * in order to populate the modules dir.
 */
public class DatabaseBasedRestManagementEnvironment {
    public static final String FATAL_EXCEPTION = "FatalException";
    public static final String SERVER_STARTED = "ServerStarted";
    public static final String EXIT = "EXIT";
    public static final String PROCESS = "PROCESS";
    private static final Logger logger = Logger.getLogger(DatabaseBasedRestManagementEnvironment.class.getName());
    private ClassPathXmlApplicationContext applicationContext;
    private ServerRESTGatewayManagementAssertion restManagementAssertion;

    public DatabaseBasedRestManagementEnvironment() throws PolicyAssertionException, SaveException, IOException {
        SyspropUtil.setProperty("com.l7tech.server.logDirectory", this.getClass().getResource("/gateway/logs").getPath());
        SyspropUtil.setProperty("com.l7tech.server.configDirectory", this.getClass().getResource("/gateway/config").getPath());
        SyspropUtil.setProperty("com.l7tech.server.varDirectory", this.getClass().getResource("/gateway/var").getPath());
        SyspropUtil.setProperty("com.l7tech.server.attachmentDirectory", this.getClass().getResource("/gateway/var").getPath());
        SyspropUtil.setProperty("com.l7tech.server.modularAssertionsDirectory", "modules/skunkworks/build/modules");
        SyspropUtil.setProperty("com.l7tech.server.dbScriptsDirectory", "etc/db/liquibase");

        applicationContext = new ClassPathXmlApplicationContext(
                "/com/l7tech/server/resources/testEmbeddedDbContext.xml",
                "testManagerContext.xml",
                "/com/l7tech/server/resources/dataAccessContext.xml");
        restManagementAssertion = new ServerRESTGatewayManagementAssertion(new RESTGatewayManagementAssertion(), applicationContext);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(restManagementAssertion);

        AssertionRegistry assertionRegistry = applicationContext.getBean("assertionRegistry", AssertionRegistry.class);
        //need to register these modular assertions manually because they are not automatically registered since they are also available in the class path and clash with the modass
        assertionRegistry.registerAssertion(JdbcQueryAssertion.class);
        assertionRegistry.registerAssertion(GatewayManagementAssertion.class);
        assertionRegistry.registerAssertion(RESTGatewayManagementAssertion.class);
        assertionRegistry.registerAssertion(GenericEntityManagerDemoAssertion.class);

        applicationContext.start();
        applicationContext.publishEvent(new Started(this, Component.GW_SERVER, "127.0.0.1"));
        applicationContext.publishEvent(new ReadyForMessages(this, Component.GW_SERVER, "127.0.0.1"));
        applicationContext.publishEvent(new LicenseChangeEvent(this, Level.FINE, "LicenseEvent", "Event Message"));

        //need to create the default http connectors
        SsgConnectorManager ssgConnectorManager = applicationContext.getBean(SsgConnectorManager.class);
        Collection<SsgConnector> defaultConnectors = DefaultHttpConnectors.getDefaultConnectors();
        for (SsgConnector connector : defaultConnectors) {
            ssgConnectorManager.save(connector);
        }

        //This will force the default ssl key to be created
        DefaultKey defaultKey = applicationContext.getBean(DefaultKey.class);
        defaultKey.getSslInfo();
    }

    /**
     * This is used to start an instance of the DatabaseBasedRestManagementEnvironment within a separate JVM for testing purposes
     * @throws PolicyAssertionException
     */
    public static void main(String args[]) throws PolicyAssertionException {
        final DatabaseBasedRestManagementEnvironment databaseBasedRestManagementEnvironment;
        try {
            databaseBasedRestManagementEnvironment = new DatabaseBasedRestManagementEnvironment();
        } catch (Throwable t) {
            System.out.println(FATAL_EXCEPTION + ": " + t.getMessage());
            System.exit(1);
            return;
        }
        System.out.println(SERVER_STARTED);
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            try {
                String line = sc.nextLine();
                if (line.equals(EXIT)) {
                    System.exit(0);
                } else if (line.equals(PROCESS)) {
                    String restRequestSerialized = sc.nextLine();
                    List<Byte> requestBytes = Functions.map(Arrays.asList(restRequestSerialized.substring(1, restRequestSerialized.length() - 1).split(",")), new Functions.Unary<Byte, String>() {
                        @Override
                        public Byte call(String s) {
                            return Byte.parseByte(s.trim());
                        }
                    });
                    RestRequest request = (RestRequest) SerializationUtils.deserialize(ArrayUtils.toPrimitive(requestBytes.toArray(new Byte[requestBytes.size()])));
                    try {
                        RestResponse restResponse = databaseBasedRestManagementEnvironment.processRequest(request.getUri(), request.getQueryString(), request.getMethod(), request.getContentType(), request.getBody(), request.getHeaders());
                        byte[] bytes = SerializationUtils.serialize(restResponse);
                        System.out.println(Arrays.toString(bytes));
                    } catch (Exception e) {
                        System.out.println(FATAL_EXCEPTION + ": " + e.getMessage());
                    }
                } else {
                    System.err.println("Unknown command:" + line);
                    System.out.println(FATAL_EXCEPTION + ": " + "Unknown command:" + line);
                }
            } catch(Throwable t){
                System.err.println(t.getMessage());
                t.printStackTrace();
                System.out.println(FATAL_EXCEPTION + ": " + "Exception Thrown:" + t.toString());
                break;
            }
        }
        System.exit(1);
    }

    public RestResponse processRequest(String uri, HttpMethod method, @Nullable String contentType, String body) throws Exception {
        return processRequest(uri, null, method, contentType, body, null);
    }

    public RestResponse processRequest(@NotNull String uri, @Nullable String queryString, @NotNull HttpMethod method, @Nullable String contentType, @Nullable String body)  throws Exception {
        return processRequest(uri,queryString,method,contentType,body,null);
    }

    public RestResponse processRequest(@NotNull String uri, @Nullable String queryString, @NotNull HttpMethod method, @Nullable String contentType, @Nullable String body, @Nullable Map<String, String> headers)  throws Exception {
        // fake user authentication
        UserBean admin = new UserBean("admin");
        admin.setUniqueIdentifier(new Goid(0, 3).toString());
        admin.setProviderId(new Goid(0, -2));
        return processRequest(uri, queryString, method, contentType, body, headers, admin);
    }

    public RestResponse processRequest(@NotNull String uri, @Nullable String queryString, @NotNull HttpMethod method, @Nullable String contentType, @Nullable String body, @Nullable Map<String, String> requestHeaders, @NotNull User user) throws Exception {
        final ContentTypeHeader contentTypeHeader = contentType == null ? ContentTypeHeader.OCTET_STREAM_DEFAULT : ContentTypeHeader.parseValue(contentType);
        final Message request = new Message();
        request.initialize(contentTypeHeader, body == null ? new byte[0] : body.getBytes("utf-8"));
        final Message response = new Message();

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
        final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        servletContext.setContextPath("/");

        httpServletRequest.setMethod(method.getProtocolName());
        if (contentType != null) {
            httpServletRequest.setContentType(contentType);
            httpServletRequest.addHeader("Content-Type", contentType);
        }
        httpServletRequest.setRemoteAddr("127.0.0.1");
        httpServletRequest.setServerName("127.0.0.1");
        httpServletRequest.setRequestURI("/restman/1.0/" + uri);
        httpServletRequest.setQueryString(queryString);
        httpServletRequest.setContent(body == null ? new byte[0] : body.getBytes("utf-8"));


        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
        request.attachHttpRequestKnob(reqKnob);
        if(requestHeaders!=null) {
            HeadersKnob headersKnob = new HeadersKnobSupport();
            for(String headerKey: requestHeaders.keySet()){
                headersKnob.addHeader(headerKey, requestHeaders.get(headerKey), HeadersKnob.HEADER_TYPE_HTTP);
            }
            request.attachKnob(HeadersKnob.class, headersKnob);
        }

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(httpServletResponse);
        response.attachHttpResponseKnob(respKnob);

        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

            context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(
                    user,
                    new HttpBasicToken(user.getLogin(), "".toCharArray()), null, false)
            );

            final PublishedService service = new PublishedService();
            service.setRoutingUri("/restman*");
            context.setService(service);

            AssertionStatus assertionStatus = restManagementAssertion.checkRequest(context);

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), bout);
            String responseBody = bout.toString("UTF-8");
            HashMap<String, String[]> headers = new HashMap<>();
            for (String header : response.getHeadersKnob().getHeaderNames(HEADER_TYPE_HTTP)) {
                headers.put(header, response.getHeadersKnob().getHeaderValues(header, HEADER_TYPE_HTTP));
            }
            RestResponse restResponse = new RestResponse(assertionStatus, responseBody, response.getHttpResponseKnob().getStatus(), headers);
            logger.log(Level.INFO, restResponse.toString());
            return restResponse;
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    public String getUriStart() {
        return "http://localhost:80/restman/1.0/";
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public ServerRESTGatewayManagementAssertion getRestManagementAssertion() {
        return restManagementAssertion;
    }
}
