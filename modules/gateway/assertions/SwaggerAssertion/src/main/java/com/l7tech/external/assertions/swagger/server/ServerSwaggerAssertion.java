package com.l7tech.external.assertions.swagger.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server side implementation of the SwaggerAssertion.
 *
 * @see com.l7tech.external.assertions.swagger.SwaggerAssertion
 */
public class ServerSwaggerAssertion extends AbstractServerAssertion<SwaggerAssertion> {
    private final String[] variablesUsed;
    private Swagger swaggerModel = null;
    private PathDefinitionResolver pathDefinitionResolver = null;
    private AtomicInteger swaggerDocumentHash = new AtomicInteger();
    private Lock lock = new ReentrantLock();

    public ServerSwaggerAssertion( final SwaggerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String doc = extractContextVarValue(assertion.getSwaggerDoc(), variableMap, getAudit());

        if (doc == null) {
            logAndAudit(AssertionMessages.SWAGGER_ASSERTION_INVALID_DOCUMENT,
                    (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
            return AssertionStatus.FALSIFIED;
        }

        // store a hash of the document value so we can avoid redundant parsing
        if (swaggerDocumentHash.get() == 0 || swaggerDocumentHash.get() != doc.hashCode()) {
            lock.lock();
            //TODO: this block must be synchronized so we don't have concurrency issue
            try {
                swaggerModel = parseSwaggerJson(doc);

                //check if the document parsed properly
                if (swaggerModel != null) {
                    pathDefinitionResolver = new PathDefinitionResolver(swaggerModel);
                    swaggerDocumentHash.set(doc.hashCode());
                } else {
                    swaggerModel = null;
                    pathDefinitionResolver = null;
                    swaggerDocumentHash.set(0);
                    logAndAudit(AssertionMessages.SWAGGER_ASSERTION_INVALID_DOCUMENT,
                            (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
                    return AssertionStatus.FALSIFIED;
                }
            } finally {
                //release the lock
                lock.unlock();
            }
        }

        final Message message = context.getRequest();

        HttpRequestKnob httpRequestKnob = message.getHttpRequestKnob();

        if (message.getHttpRequestKnob() == null) {
            // TODO jwilliams: audit failure
            return AssertionStatus.FALSIFIED;
        }

        String serviceBase = StringUtils.isNotBlank(assertion.getServiceBase()) ? assertion.getServiceBase() : getServiceRoutingUri(context.getService());
        String apiUri = httpRequestKnob.getRequestUri().replaceFirst(serviceBase, ""); // remove service from the request uri

        // set context variables regardless of the validation results
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_API_URI, apiUri);
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_HOST, swaggerModel.getHost());
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_BASE_URI, swaggerModel.getBasePath());

        // perform the validation

        if (assertion.isValidatePath()) {
            Path requestPath = pathDefinitionResolver.getPathForRequestUri(apiUri);

            if (null == requestPath) {  // no matching path template
                // TODO jwilliams: audit
                return AssertionStatus.FALSIFIED;
            }

            if (assertion.isValidateMethod()) {
                HttpMethod method = httpRequestKnob.getMethod();

                Operation operation;

                switch (method) {
                    case GET:
                        operation = requestPath.getGet();
                        break;
                    case POST:
                        operation = requestPath.getPost();
                        break;
                    case PUT:
                        operation = requestPath.getPut();
                        break;
                    case PATCH:
                        operation = requestPath.getPatch();
                        break;
                    case DELETE:
                        operation = requestPath.getDelete();
                        break;
                    case OPTIONS:
                        operation = requestPath.getOptions();
                        break;
                    case HEAD:
                    case OTHER:
                    default:
                        // TODO jwilliams: invalid operation - audit
                        return AssertionStatus.FALSIFIED;
                }

                if (null == operation) {    // no operation found for method; nothing defined
                    // TODO jwilliams: audit
                    return AssertionStatus.FALSIFIED;
                }

                if (assertion.isValidateScheme()) {
                    List<Scheme> schemes = operation.getSchemes();

                    if (null == schemes) {
                        schemes = swaggerModel.getSchemes();
                        // TODO jwilliams: this uses top level scheme definition - must take into account overriding at operation level
                    }

                    Scheme requestScheme = httpRequestKnob.isSecure() ? Scheme.HTTPS : Scheme.HTTP; // TODO jwilliams: what about WS & WSS? they are possible schemes

                    if (null != schemes && !schemes.contains(requestScheme)) { // request scheme is not defined for operation
                        // TODO jwilliams: audit
                        return AssertionStatus.FALSIFIED;
                        // TODO jwilliams: check specification and decide on behaviour for default scheme validation
                    }
                }
            }
        }

        return AssertionStatus.NONE;
    }

    protected Swagger parseSwaggerJson(String doc) {
        SwaggerParser parser = new SwaggerParser();
        List<AuthorizationValue> authorizationValues = new ArrayList<>();
        authorizationValues.add(new AuthorizationValue());
        return parser.parse(doc, authorizationValues);
    }

    private String getServiceRoutingUri(PublishedService service) {
        String routingUri = service.getRoutingUri();

        if (routingUri == null || routingUri.length() < 1) {
            return SecureSpanConstants.SERVICE_FILE + service.getId(); // refer to service by its ID
        } else {
            return routingUri.replaceAll("/\\*.*$", "");
        }
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

    static class PathDefinitionResolver {
        private final Swagger swaggerDefinition;
        private final List<Pair<UriTemplate, Path>> paths;

        public PathDefinitionResolver(Swagger definition) {
            this.swaggerDefinition = definition;
            paths = new ArrayList<>();

            populatePathMap();
        }

        public Path getPathForRequestUri(String requestUri) {
            for (Pair<UriTemplate, Path> templatePathPair : paths) {
                if (templatePathPair.left.matches(requestUri)) {
                    return templatePathPair.right;   // TODO jwilliams: find a more efficient way of doing this - seems kludgy and slow
                }
            }

            return null;
        }

        private void populatePathMap() {
            List<String> definitionPaths = new ArrayList<>(swaggerDefinition.getPaths().keySet());
            Collections.sort(definitionPaths, Collections.reverseOrder());

            for (String path : definitionPaths) {
                paths.add(new Pair<>(new UriTemplate(path), swaggerDefinition.getPath(path)));
            }
        }
    }
}
