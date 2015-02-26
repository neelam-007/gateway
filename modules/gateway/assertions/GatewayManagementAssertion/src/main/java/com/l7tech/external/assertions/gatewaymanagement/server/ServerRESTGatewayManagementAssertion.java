package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.rest.RestAgent;
import com.l7tech.gateway.rest.RestResponse;
import com.l7tech.identity.User;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the GatewayManagementAssertion.
 *
 * @see com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion
 */
public class ServerRESTGatewayManagementAssertion extends AbstractMessageTargetableServerAssertion<RESTGatewayManagementAssertion> {

    public static final String Version1_0_URI = "1.0/";

    @Inject
    private StashManagerFactory stashManagerFactory;

    private final RestAgent restAgent;
    //This is package private so that it is available to Tests
    ApplicationContext assertionContext;

    public ServerRESTGatewayManagementAssertion(final RESTGatewayManagementAssertion assertion,
                                                final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        this.assertionContext = GatewayManagementApplicationContext.getAssertionApplicationContext(applicationContext);
        restAgent = assertionContext.getBean("restAgent", RestAgent.class);
    }

    // for tests to provide custom assertion context, this context will not be shared
    ServerRESTGatewayManagementAssertion(final RESTGatewayManagementAssertion assertion,
                                         final ApplicationContext applicationContext,
                                         final String assertionContextResource) throws PolicyAssertionException {
        super(assertion);
        assertionContext = new ClassPathXmlApplicationContext(new String[]{assertionContextResource}, ServerRESTGatewayManagementAssertion.class, applicationContext);
        restAgent = assertionContext.getBean("restAgent", RestAgent.class);
    }

    protected ApplicationContext getAssertionContext(){return assertionContext;}

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        final Message response = context.getResponse();

        try {
            final User user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
            if ( null == user )
                throw new IllegalStateException( "An authenticated user is required but not present" );
            if ( null == context.getService() && !assertion.getTarget().equals(TargetMessageType.OTHER) )
                throw new IllegalStateException( "A resolved service is required unless a message variable is targeted" );

            Map<String, Object> varMap =  context.getVariableMap(assertion.getVariablesUsed(), getAudit());

            // get the uri
            final URI uri = getURI(context, varMap, message, assertion);
            final URI baseUri = getBaseURI(context, message, assertion);
            //The service ID should always be a constant.
            final String serviceId = context.getService() == null ? null : context.getService().getId();
            final HttpMethod action = getAction(varMap, message, assertion);
            final String contentType = getContentType( message);

            context.setRoutingStatus(RoutingStatus.ATTEMPTED);

            SecurityContext securityContext = new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return user;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public boolean isUserInRole(String role) {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public boolean isSecure() {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public String getAuthenticationScheme() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }
            };
            RestResponse managementResponse = restAgent.handleRequest(message.isHttpRequest()?message.getHttpRequestKnob().getRemoteHost():null,
                    baseUri, uri, action.getProtocolName(), contentType, message.getMimeKnob().getEntireMessageBodyAsInputStream(),
                    securityContext, CollectionUtils.MapBuilder.<String, Object>builder().put("ServiceId", serviceId).map(), message.getHeadersKnob().getHeaders(HeadersKnob.HEADER_TYPE_HTTP));
            response.initialize(stashManagerFactory.createStashManager(), managementResponse.getContentType()==null?ContentTypeHeader.NONE:ContentTypeHeader.parseValue(managementResponse.getContentType()), managementResponse.getInputStream());
            response.getMimeKnob().getContentLength();
            response.getHttpResponseKnob().setStatus(managementResponse.getStatus());
            for(String header : managementResponse.getHeaders().keySet()){
                //Don't set the content-length header. See bug: SSG-7824
                if(header.equals("Content-Length")) {
                    continue;
                }
                for(Object value : managementResponse.getHeaders().get(header)){
                    response.getHeadersKnob().addHeader(header, value.toString(), HeadersKnob.HEADER_TYPE_HTTP);
                }
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);

            // audit errors if any only when not testing
            if(!CollectionUtils.list(200, 201, 204).contains(managementResponse.getStatus()) && !uri.toString().matches(".*bundle\\?.*test=true.*")){
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), bout);
                String responseBody = bout.toString("UTF-8");
                getAudit().logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, responseBody);
            }

            return AssertionStatus.NONE;
        } catch (Exception e) {
            handleError(e);
        }
        return AssertionStatus.FAILED;
    }

    private String getContentType( Message message) throws IOException {
        if(assertion.getTarget().equals(TargetMessageType.OTHER)){
            return message.getMimeKnob().getOuterContentType().getFullValue();
        } else{
            return message.getHttpRequestKnob().getHeaderSingleValue(HttpHeaders.CONTENT_TYPE);
        }
    }

    protected static HttpMethod getAction(final Map<String, Object> varMap,
                                          final Message message,
                                          final RESTGatewayManagementAssertion assertion) throws IllegalArgumentException {
        if(assertion.getTarget().equals(TargetMessageType.OTHER)){
            Object var = varMap.get(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_ACTION);
            if(var == null){
                throw new IllegalArgumentException(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_ACTION+" context variable not found.");
            }
            return HttpMethod.valueOf(var.toString().toUpperCase());
        } else{
            return message.getHttpRequestKnob().getMethod();
        }
    }

    protected static URI getURI(final PolicyEnforcementContext context,
                                final Map<String, Object> varMap,
                                   final Message message,
                                   final RESTGatewayManagementAssertion assertion) throws IllegalArgumentException, URISyntaxException {

        if(assertion.getTarget().equals(TargetMessageType.OTHER)){
            Object var = varMap.get(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_URI);
            if(var == null){
                throw new IllegalArgumentException(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_URI + " context variable not found.");
            }
            String uri = var.toString();
            // handle extra "/" at beginning
            if(uri.startsWith("/")){
                uri = uri.substring(1);
            }

            return new URI(uri);
        }else{
            String uri;
            String requestURI = message.getHttpRequestKnob().getRequestUri();
            String baseURI = context.getService().getRoutingUri();
            Pattern pattern = Pattern.compile(baseURI.replace("*", "(.*)"));
            Matcher matcher = pattern.matcher(requestURI);
            if ( matcher.matches() && matcher.groupCount() > 0 )
                uri = matcher.group(1);
            else
                throw new IllegalArgumentException("Could not calculate uri");
            final String queryString = message.getHttpRequestKnob().getQueryString();
            if (queryString != null) {
                uri += "?" + queryString;
            }
            if (uri == null) {
                throw new IllegalArgumentException("The uri cannot be null");
            }
            return new URI(uri);
        }

    }

    protected static URI getBaseURI(final PolicyEnforcementContext context,
                                final Message message,
                                final RESTGatewayManagementAssertion assertion) throws IllegalArgumentException, URISyntaxException {
        if(assertion.getTarget().equals(TargetMessageType.OTHER)){
            return new URI("/");
        } else{
            String uri = message.getHttpRequestKnob().isSecure() ? "https://" : "http://";
            uri += message.getTcpKnob().getLocalHost();
            uri += ":" + message.getTcpKnob().getLocalPort();
            String baseURI = context.getService().getRoutingUri();
            if (baseURI.endsWith("*")) {
                uri += baseURI.substring(0, baseURI.length() - 1);
            } else {
                throw new IllegalArgumentException("Could not calculate base uri. The service needs to resolve with a wild card (*)");
            }
            return new URI(uri);
        }
    }

    private int handleError(Exception exception) {
        int errorRseponse = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if (exception instanceof UnsupportedOperationException) {
            errorRseponse = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
        }

        getAudit().logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[] { exception.getMessage() }, ExceptionUtils.getDebugException( exception ) );
        return errorRseponse;
    }
}
