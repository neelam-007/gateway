package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.Pair;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.mail.MethodNotSupportedException;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the GatewayManagementAssertion.
 *
 * @see com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion
 */
public class ServerRESTGatewayManagementAssertion extends AbstractMessageTargetableServerAssertion<RESTGatewayManagementAssertion> {

    private GatewayManagementHandler support;
    protected static final String defaultIdType = "name";
    //- PUBLIC

    public ServerRESTGatewayManagementAssertion(final RESTGatewayManagementAssertion assertion,
                                                final BeanFactory context) throws PolicyAssertionException {
        super(assertion);
        support = new GatewayManagementHandler();
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        final Message response = context.getResponse();

        try{
            final Principal user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();

            // get parameters
            // URI format: <resource>/<id>?<idType=[goid|guid|name]>
            final String URI = getURI(context, message,assertion);
            final Pair<String,String> resourceInputs  = getResourceInputs(URI);
            final String resourceType = resourceInputs.left;
            final String selectorValue = resourceInputs.right;
            String selectorType = message.getHttpRequestKnob().getParameter("idType");
            if(selectorType == null){
                selectorType = defaultIdType;
            }else if(selectorType.equals("goid")){
                selectorType = "id";
            }

            // build selector map
            Map<String,String> selectorMap = new HashMap<String,String>();
            selectorMap.put(selectorType,selectorValue);

            // build parameters
            Map<String, Object> params = new HashMap<String,Object>();
            params.put(GatewayManagementHandler.SELECTOR_MAP,selectorMap);
            params.put(GatewayManagementHandler.PAYLOAD,getBodyDocument(context,message,assertion));

            HttpMethod action = getAction(context,message,assertion);
            context.setRoutingStatus(RoutingStatus.ATTEMPTED);

            Document managementResponse;
            managementResponse = support.handle(resourceType, params, action.toString(), user);
            response.initialize(managementResponse);


            context.setRoutingStatus(RoutingStatus.ROUTED);
            return AssertionStatus.NONE;

        }catch(Exception e){
            handleError(e);
        }
        return AssertionStatus.FAILED;

    }

    // pair of resource type and selector value
    protected static Pair<String,String> getResourceInputs(String URI){
        final int divider = URI.contains("/") ? URI.indexOf("/") : URI.length();
        String resourceType = URI.substring(0, divider);
        String selectorValue = URI.contains("/") ? URI.substring(divider+1) : null;

        return new Pair<String, String>(resourceType,selectorValue);
    }

    protected static HttpMethod getAction(final PolicyEnforcementContext context,
                                   final Message message,
                                   final RESTGatewayManagementAssertion assertion) throws IllegalArgumentException{
        HttpMethod method = null;
        try {
            String actionVar = (String) context.getVariable(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_ACTION);
            method = HttpMethod.valueOf(actionVar.toUpperCase());
        } catch (NoSuchVariableException e) {
            method = message.getHttpRequestKnob().getMethod();
        }
        if (method == null) {
            throw new IllegalArgumentException();
        }

        if(method.equals(HttpMethod.GET)||
            method.equals(HttpMethod.DELETE)||
            method.equals(HttpMethod.POST)){
            return method;
        }
        throw new IllegalArgumentException();

    }

    protected static String getURI(final PolicyEnforcementContext context,
                            final Message message,
                            final RESTGatewayManagementAssertion assertion) throws IllegalArgumentException {

        String URI = null;
        try {
            URI = (String) context.getVariable(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_URI);
        } catch (NoSuchVariableException e) {

            String requestURI = message.getHttpRequestKnob().getRequestUri();
            String baseURI = context.getService().getRoutingUri();
            Pattern pattern =  Pattern.compile(baseURI.replace("*", "(.*)"));
            Matcher matcher = pattern.matcher(requestURI);
            if(matcher.matches())
                URI = matcher.group(1);
        }
        if (URI == null) {
            throw new IllegalArgumentException();
        }
        return URI;
    }

    protected static Document getBodyDocument(final PolicyEnforcementContext context,
                                       final Message message,
                                       final RESTGatewayManagementAssertion assertion) throws IllegalArgumentException {

        Document requestDocument = null;
        try {
            String docString = (String) context.getVariable(assertion.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_BODY);
            if (docString.isEmpty()) {
                return null;
            }
            requestDocument = XmlUtil.parse(docString);
            return requestDocument;

        } catch (NoSuchVariableException e) {
            // expected
        } catch (SAXException e) {
            throw new IllegalArgumentException();
        }

        try {
            InputStream stream = message.getMimeKnob().getEntireMessageBodyAsInputStream();
            requestDocument = XmlUtil.parse(stream);
            return requestDocument;
        } catch (SAXException e) {
            throw new IllegalArgumentException();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        } catch (NoSuchPartException e) {
            throw new IllegalArgumentException();
        }
    }

    private int handleError(Exception exception) {
        int errorRseponse = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if (exception instanceof UnsupportedOperationException) {
            errorRseponse = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
        }

        getAudit().logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{exception.getMessage()});
        return errorRseponse;
    }

    //- PRIVATE


}
