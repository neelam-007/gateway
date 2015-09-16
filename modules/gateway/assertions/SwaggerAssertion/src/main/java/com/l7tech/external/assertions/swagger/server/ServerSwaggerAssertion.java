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
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.UriTemplate;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server side implementation of the SwaggerAssertion.
 *
 * @see com.l7tech.external.assertions.swagger.SwaggerAssertion
 */
public class ServerSwaggerAssertion extends AbstractServerAssertion<SwaggerAssertion> {
    public static final String AUTHORIZATION_HEADER = "authorization";
    private final String[] variablesUsed;
    private AtomicReference<Swagger> swaggerModel = new AtomicReference<>(null);
    private AtomicReference<PathResolver> pathResolver = new AtomicReference<>(null);
    private AtomicInteger swaggerDocumentHash = new AtomicInteger();
    private Lock lock = new ReentrantLock();
    private Map<String,ValidateSecurity> securityTypeMap;


    public ServerSwaggerAssertion( final SwaggerAssertion assertion ) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        securityTypeMap = new HashMap<>();
        securityTypeMap.put("basic", new ValidateBasicSecurity());
        securityTypeMap.put("apiKey", new ValidateApiKeySecurity());
        securityTypeMap.put("oauth2", new ValidateOauth2Security());
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String doc = extractContextVarValue(assertion.getSwaggerDoc(), variableMap, getAudit());

        if (doc == null) {
            logAndAudit(AssertionMessages.SWAGGER_INVALID_DOCUMENT,
                    (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
            return AssertionStatus.FAILED;
        }

        Swagger model;
        PathResolver resolver;

        lock.lock();

        try {
            // store a hash of the document value so we can avoid redundant parsing
            if (swaggerDocumentHash.get() == 0 || swaggerDocumentHash.get() != doc.hashCode()) {
                model = parseSwaggerJson(doc);

                //check if the document parsed properly
                if (model != null) {
                    resolver = new PathResolver(model);
                    pathResolver.set(resolver);
                    swaggerModel.set(model);
                    swaggerDocumentHash.set(doc.hashCode());
                } else {
                    swaggerModel.set(null);
                    pathResolver.set(null);
                    swaggerDocumentHash.set(0);
                    logAndAudit(AssertionMessages.SWAGGER_INVALID_DOCUMENT,
                            (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
                    return AssertionStatus.FAILED;
                }
            } else {
                model = swaggerModel.get();
                resolver = pathResolver.get();
            }
        } finally {
            //release the lock
            lock.unlock();
        }

        HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();
        // calculate the apiUri
        String apiUri = getApiUri(context, variableMap, httpRequestKnob);

        // set context variables regardless of the validation results
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_API_URI, apiUri);
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_HOST, model.getHost());
        context.setVariable(assertion.getPrefix() + SwaggerAssertion.SWAGGER_BASE_URI, model.getBasePath());

        // perform the validation

        return validate(model, resolver, httpRequestKnob, apiUri) ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
    }

    private String getApiUri(PolicyEnforcementContext context, Map<String, Object> variableMap, HttpRequestKnob httpRequestKnob) {
        String serviceBase;
        if(StringUtils.isNotBlank(assertion.getServiceBase())){
            serviceBase = ExpandVariables.process(assertion.getServiceBase(), variableMap, getAudit());
        }
        else {
            serviceBase = getServiceRoutingUri(context.getService());
        }

        String apiUri;

        String requestUri = httpRequestKnob.getRequestUri().trim();
        int serviceBaseIndex = requestUri.indexOf(serviceBase); // remove service from the request uri
        if(serviceBaseIndex == 0) {
            apiUri = requestUri.substring(serviceBase.length());
        }
        else {
            apiUri = requestUri;
        }
        return apiUri;
    }

    protected boolean validate(Swagger model, PathResolver resolver, HttpRequestKnob httpRequestKnob, String apiUri) {
        if (assertion.isValidatePath()) {
            PathDefinition requestPathDefinition = resolver.getPathForRequestUri(apiUri);

            if (null == requestPathDefinition) {  // no matching path template
                logAndAudit(AssertionMessages.SWAGGER_INVALID_PATH, apiUri);
                return false;
            }

            Path requestPathModel = model.getPath(requestPathDefinition.path);

            if (assertion.isValidateMethod()) {
                HttpMethod method = httpRequestKnob.getMethod();

                Operation operation = null;

                switch (method) {
                    case GET:
                        operation = requestPathModel.getGet();
                        break;
                    case POST:
                        operation = requestPathModel.getPost();
                        break;
                    case PUT:
                        operation = requestPathModel.getPut();
                        break;
                    case PATCH:
                        operation = requestPathModel.getPatch();
                        break;
                    case DELETE:
                        operation = requestPathModel.getDelete();
                        break;
                    case OPTIONS:
                        operation = requestPathModel.getOptions();
                        break;
                    case HEAD:
                    case OTHER:
                    default:
                        break;
                }

                if (null == operation) {    // no operation found for method; nothing defined
                    logAndAudit(AssertionMessages.SWAGGER_INVALID_METHOD, method.name(), requestPathDefinition.path);
                    return false;
                }

                if (assertion.isValidateScheme()) {
                    List<Scheme> schemes = operation.getSchemes();

                    if (null == schemes) {
                        schemes = model.getSchemes(); // use top-level schemes if the operation doesn't define its own
                    }

                    // N.B. We do not currently support WS/WSS request validation
                    Scheme requestScheme = httpRequestKnob.isSecure() ? Scheme.HTTPS : Scheme.HTTP;

                    if (null != schemes && !schemes.contains(requestScheme)) { // request scheme is not defined for operation
                        logAndAudit(AssertionMessages.SWAGGER_INVALID_SCHEME,
                                method.getProtocolName(), requestPathDefinition.path, requestScheme.toValue());
                        return false;
                    }
                }

                if (assertion.isRequireSecurityCredentials()) {
                    Map<String, SecuritySchemeDefinition> securityDefinitions = model.getSecurityDefinitions();
                    return validateRequestSecurity(httpRequestKnob, operation, securityDefinitions, requestPathDefinition.path);
                }
            }
        }

        return true;
    }

    private boolean validateRequestSecurity(HttpRequestKnob httpRequestKnob, Operation operation, Map<String, SecuritySchemeDefinition> securityDefinitions, String path) {
        if (securityDefinitions != null) {
            List<Map<String, List<String>>> securityRequirementObjects = operation.getSecurity();

            if ( securityRequirementObjects == null ) return true;  /* No Security Requirements Object = no security required */

            boolean disjunctValid = false;
            if ( !securityRequirementObjects.isEmpty() ) {
                for (Map<String, List<String>> securityRequirementObject : securityRequirementObjects) {
                    boolean conjunctValid = true;
                    Set<String> securitySchemeObjects = securityRequirementObject.keySet();
                    for(String securitySchemeObject : securitySchemeObjects) {
                        SecuritySchemeDefinition definition = securityDefinitions.get(securitySchemeObject);
                        ValidateSecurity type = securityTypeMap.get(definition.getType());
                        if (type == null) {
                            logAndAudit(AssertionMessages.SWAGGER_INVALID_SECURITY_DEFINITION, definition.getType(), operation.getOperationId(), path);
                            return false;
                        }
                        conjunctValid &= type.checkSecurity(httpRequestKnob,definition);
                    }

                    disjunctValid |= conjunctValid;
                    if(disjunctValid) break;// no need to validate other security requirements when we found valid one
                }
            }

            if (!disjunctValid) {
                logAndAudit(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED, operation.getOperationId(), path);
            }

            return disjunctValid;
        }
        else {
            //security is not required
            return true;
        }
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

    static class PathResolver {
        private final Swagger swaggerDefinition;
        private final ArrayList<PathDefinition> pathDefinitions;

        public PathResolver(Swagger definition) {
            this.swaggerDefinition = definition;
            pathDefinitions = new ArrayList<>();

            populatePathMap();
        }

        public PathDefinition getPathForRequestUri(String requestUri) {
            if (null == requestUri || requestUri.isEmpty())
                return null;

            PathDefinition requestPath = new PathDefinition(requestUri);

            PathDefinition closestMatch = null;

            for (PathDefinition p : pathDefinitions) {
                if (p.segments.size() == requestPath.segments.size()) {
                    if (p.equals(requestPath))
                        return p;

                    if (p.matches(requestPath)) {
                        if (null == closestMatch ||
                                closestMatch.getVariableNames().size() > p.getVariableNames().size()) {
                            closestMatch = p;
                        }
                    }
                }
            }

            return closestMatch;
        }

        private void populatePathMap() {
            for (String path : swaggerDefinition.getPaths().keySet()) {
                PathDefinition pathDefinition = new PathDefinition(path);
                pathDefinitions.add(pathDefinition);
            }
        }
    }

    static class PathDefinition {
        final String path;
        final UriTemplate uriTemplate;
        final List<String> segments = new ArrayList<>();

        public PathDefinition(String path) {
            this.path = path;
            this.uriTemplate = new UriTemplate(this.path);

            Collections.addAll(segments, path.split("/"));
        }

        public boolean matches(PathDefinition that) {
            return uriTemplate.matches(that.path);
        }

        public List<String> getVariableNames() {
            return uriTemplate.getVariableNames();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PathDefinition that = (PathDefinition) o;

            return this.path.equals(that.path) && this.segments.equals(that.segments);
        }
    }
}
