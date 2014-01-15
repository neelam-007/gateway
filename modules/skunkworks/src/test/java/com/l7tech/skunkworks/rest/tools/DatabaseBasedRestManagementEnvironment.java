package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.UserBean;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.SerializationUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This will bring up an entire database backed rest management assertion environment.
 */
public class DatabaseBasedRestManagementEnvironment {
    private static final Logger logger = Logger.getLogger(DatabaseBasedRestManagementEnvironment.class.getName());

    public static final String FATAL_EXCEPTION = "FatalException";
    public static final String SERVER_STARTED = "ServerStarted";
    public static final String EXIT = "EXIT";
    public static final String PROCESS = "PROCESS";

    private ApplicationContext applicationContext;
    private ServerRESTGatewayManagementAssertion restManagementAssertion;

    public DatabaseBasedRestManagementEnvironment() throws PolicyAssertionException {
        SyspropUtil.setProperty("com.l7tech.server.logDirectory", this.getClass().getResource("/gateway/logs").getPath());
        SyspropUtil.setProperty("com.l7tech.server.varDirectory", this.getClass().getResource("/gateway/var").getPath());
        SyspropUtil.setProperty("com.l7tech.server.attachmentDirectory", this.getClass().getResource("/gateway/var").getPath());
        SyspropUtil.setProperty("com.l7tech.server.modularAssertionsDirectory", "build/modules");

        applicationContext = new ClassPathXmlApplicationContext(
                "/com/l7tech/server/resources/testEmbeddedDbContext.xml",
                "testManagerContext.xml",
                "/com/l7tech/server/resources/dataAccessContext.xml");
        restManagementAssertion = new ServerRESTGatewayManagementAssertion(new RESTGatewayManagementAssertion(), applicationContext);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(restManagementAssertion);
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
            String line = sc.nextLine();
            if (line.equals(EXIT)) {
                System.exit(0);
            } else if (line.equals(PROCESS)) {
                String uri = sc.nextLine();
                String queryString = sc.nextLine();
                HttpMethod method = HttpMethod.valueOf(sc.nextLine());
                String contentType = sc.nextLine();
                contentType = "null".equals(contentType) ? null : contentType;
                String body = sc.nextLine();
                logger.log(Level.INFO, body);
                try {
                    RestResponse restResponse = databaseBasedRestManagementEnvironment.processRequest(uri, queryString, method, contentType, body);
                    byte[] bytes = SerializationUtils.serialize(restResponse);
                    System.out.println(Arrays.toString(bytes));
                } catch (Exception e) {
                    System.out.println(FATAL_EXCEPTION + ": " + e.getMessage());
                }
            } else {
                System.err.println(line);
            }
        }
        System.exit(1);
    }

    protected RestResponse processRequest(String uri, HttpMethod method, @Nullable String contentType, String body) throws Exception {
        return processRequest(uri, null, method, contentType, body);
    }

    protected RestResponse processRequest(String uri, String queryString, HttpMethod method, @Nullable String contentType, String body) throws Exception {
        final ContentTypeHeader contentTypeHeader = contentType == null ? ContentTypeHeader.OCTET_STREAM_DEFAULT : ContentTypeHeader.parseValue(contentType);
        final Message request = new Message();
        request.initialize(contentTypeHeader, body.getBytes("utf-8"));
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
        httpServletRequest.setContent(body.getBytes("UTF-8"));

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(httpServletResponse);
        response.attachHttpResponseKnob(respKnob);

        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

            // fake user authentication
            UserBean admin = new UserBean("admin");
            admin.setUniqueIdentifier(new Goid(0, 3).toString());
            admin.setProviderId(new Goid(0, -2));
            context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(
                    admin,
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
            for (String header : response.getHeadersKnob().getHeaderNames()) {
                headers.put(header, response.getHeadersKnob().getHeaderValues(header));
            }
            return new RestResponse(assertionStatus, responseBody, response.getHttpResponseKnob().getStatus(), headers);
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
