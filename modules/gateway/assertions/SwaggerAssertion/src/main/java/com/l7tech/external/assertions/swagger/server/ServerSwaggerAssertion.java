package com.l7tech.external.assertions.swagger.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import io.swagger.models.*;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;

/**
 * Server side implementation of the SwaggerAssertion.
 *
 * @see com.l7tech.external.assertions.swagger.SwaggerAssertion
 */
public class ServerSwaggerAssertion extends AbstractMessageTargetableServerAssertion<SwaggerAssertion> {
    private final String[] variablesUsed;
    private String swaggerDocument = null;
    private Swagger swaggerModel = null;
    private AtomicInteger swaggerDocumentHash = new AtomicInteger();
    private Lock lock = new ReentrantLock();

// DELETEME example for dependency injection
//    @Inject
//    @Named("foo") -- The name is not usually required and should be left out if possible
//    private Foo foo;

    public ServerSwaggerAssertion( final SwaggerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext )
            throws IOException, PolicyAssertionException {

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String doc = extractContextVarValue(assertion.getSwaggerDoc(), variableMap, getAudit());

        if(doc == null) {
            logAndAudit(AssertionMessages.SWAGGER_ASSERTION_INVALID_DOCUMENT, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
            return AssertionStatus.FALSIFIED;
        }

        //TODO: the document value must be hashed and stored as an atomic int so we can compare with another one
        if(swaggerDocumentHash.get() == 0 || swaggerDocumentHash.get() != doc.hashCode()) {
            lock.lock();
            //TODO: this block must be synchronized so we don't have concurrency issue
            try {
                SwaggerParser parser = new SwaggerParser();
                List<AuthorizationValue> authorizationValues = new ArrayList<>();
                authorizationValues.add(new AuthorizationValue());
                swaggerModel = parser.parse(doc, authorizationValues);
                //check if the document parsed properly
                if (swaggerModel != null) {
                    swaggerDocument = doc;
                    swaggerDocumentHash.set(swaggerDocument.hashCode());
                } else {
                    swaggerModel = null;
                    swaggerDocument = null;
                    swaggerDocumentHash.set(0);
                    logAndAudit(AssertionMessages.SWAGGER_ASSERTION_INVALID_DOCUMENT, (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
                    return AssertionStatus.FALSIFIED;
                }
            } finally {
                //release the lock
                lock.unlock();
            }
        }

        AssertionStatus status = AssertionStatus.NONE;

        if(message.isHttpRequest()) {
            HttpRequestKnob httpRequestKnob = message.getHttpRequestKnob();
            if (httpRequestKnob == null) {
                return AssertionStatus.FALSIFIED;
            }
            //TODO: do actual validation

        }
        else if(message.isHttpResponse()) {
            HttpResponseKnob httpResponseKnob = message.getHttpResponseKnob();
            if(httpResponseKnob == null) {
                return AssertionStatus.FALSIFIED;
            }

            // TODO: do actual validation on response
        }


        //TODO: set context variables regardless of the validation results
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_HOST, swaggerModel.getHost());
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_BASE_URI, swaggerModel.getBasePath());

        return status;
    }


    String getApiUriFromRequest(String requestUri, String serviceRoutingUri) {
        String patternString = serviceRoutingUri.replace("*/", "[^/]+/").replace("*","");

        if(patternString.isEmpty()) return requestUri;

        Matcher m = Pattern.compile(patternString).matcher(requestUri);
        if(m.find()) {
            String apiUri = requestUri.replace(m.group(0), "");
            return apiUri.startsWith("/")?apiUri : "/" + apiUri;
        }
        return requestUri;
    }

    private String getServiceRoutingUri(PublishedService service) {
        String routingUri = service.getRoutingUri();

        if (routingUri == null || routingUri.length() < 1) {
            return SecureSpanConstants.SERVICE_FILE + service.getId(); // refer to service by its ID
        } else {
            return routingUri;
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     *
     * DELETEME if not required.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }

    private String extractContextVarValue(String var, Map<String, Object> varMap, Audit audit) {
        if (StringUtils.isNotBlank(var)) {
            Object expanded = ExpandVariables.processSingleVariableAsObject(Syntax.SYNTAX_PREFIX + var + Syntax.SYNTAX_SUFFIX, varMap, audit);
            if(expanded instanceof Message) {
                final Message expandedMessage = (Message) expanded;
                final ContentTypeHeader contentType = expandedMessage.getMimeKnob().getOuterContentType();
                final MimeKnob mimeKnob = expandedMessage.getMimeKnob();

                try {
                    final byte[] bodyBytes = IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
                    return new String(bodyBytes, contentType.getEncoding());

                } catch (NoSuchPartException | IOException e) {
                    logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO,
                            new String[] {"swagger document"}, ExceptionUtils.getDebugException(e));
                }
            }
            else if(expanded instanceof String) {
                return (String)expanded;
            }
        }
        return null;
    }
}
