package com.l7tech.external.assertions.swagger.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.external.assertions.swagger.SwaggerUtil;
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
import io.swagger.models.*;
import io.swagger.models.auth.SecuritySchemeDefinition;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Server side implementation of the SwaggerAssertion.
 *
 * @see com.l7tech.external.assertions.swagger.SwaggerAssertion
 */
public class ServerSwaggerAssertion extends AbstractServerAssertion<SwaggerAssertion> {
    public static final String AUTHORIZATION_HEADER = "authorization";
    private final String[] variablesUsed;
    private final AtomicReference<SwaggerModelHolder> swaggerModelHolder = new AtomicReference<>(null);
    private Lock lock = new ReentrantLock();
    private Map<String,ValidateSecurity> securityTypeMap;

    public ServerSwaggerAssertion(final SwaggerAssertion assertion) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        securityTypeMap = new HashMap<>();
        securityTypeMap.put("basic", new ValidateBasicSecurity());
        securityTypeMap.put("apiKey", new ValidateApiKeySecurity());
        securityTypeMap.put("oauth2", new ValidateOauth2Security());
        swaggerModelHolder.set(new SwaggerModelHolder(null, null, 0));
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String doc = extractSwaggerDocFromContextVar(assertion.getSwaggerDoc(), variableMap, getAudit());

        if (doc == null) {
            logAndAudit(AssertionMessages.SWAGGER_INVALID_DOCUMENT,
                    (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
            return AssertionStatus.FAILED;
        }

        int docHashCode = doc.hashCode();
        int currentDocHashCode = swaggerModelHolder.get().getDocumentHash();

        // check the hash of the document value so we can avoid redundant parsing
        if (currentDocHashCode == 0 || currentDocHashCode != docHashCode) {
            // lock for writing
            lock.lock();

            try {
                currentDocHashCode = swaggerModelHolder.get().getDocumentHash();

                if (currentDocHashCode == 0 || currentDocHashCode != docHashCode) {
                    Swagger newModel = SwaggerUtil.parseSwaggerJson(doc);

                    //check if the document parsed properly
                    if (newModel != null) {
                        PathResolver newResolver = new PathResolver(newModel);
                        swaggerModelHolder.set(new SwaggerModelHolder(newModel, newResolver, docHashCode));
                    } else {
                        swaggerModelHolder.set(new SwaggerModelHolder(null, null, 0));
                        logAndAudit(AssertionMessages.SWAGGER_INVALID_DOCUMENT,
                                (String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
                        return AssertionStatus.FAILED;
                    }
                }
            } finally {
                //release the lock
                lock.unlock();
            }
        }

        SwaggerModelHolder currentSwaggerModelHolder = swaggerModelHolder.get();

        Swagger model = currentSwaggerModelHolder.getSwaggerModel();
        PathResolver resolver = currentSwaggerModelHolder.getPathResolver();

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
                        operation = requestPathModel.getHead();
                        break;
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
                    return validateRequestSecurity(httpRequestKnob, operation, requestPathDefinition.path, model);
                }
            }
        }

        return true;
    }

    private boolean validateRequestSecurity(HttpRequestKnob httpRequestKnob, Operation operation, String path, Swagger model) {
        final Map<String, SecuritySchemeDefinition> securityDefinitions = model.getSecurityDefinitions();

        if (securityDefinitions != null) {
            final List<Map<String, List<String>>> securityRequirementObjects = getSecurityRequirements(operation, model);

            if (securityRequirementObjects == null || securityRequirementObjects.isEmpty()) return true;  /* No Security Requirements Object = no security required */

            for (Map<String, List<String>> securityRequirementObject : securityRequirementObjects) {
                boolean conjunctValid = true;
                Set<String> securitySchemeObjects = securityRequirementObject.keySet();
                for(String securitySchemeObject : securitySchemeObjects) {
                    SecuritySchemeDefinition definition = securityDefinitions.get(securitySchemeObject);
                    if ( definition == null ) {
                        logAndAudit(AssertionMessages.SWAGGER_MISSING_SECURITY_DEFINITION,securitySchemeObject);
                        return false;
                    }
                    ValidateSecurity type = securityTypeMap.get(definition.getType());
                    if (type == null) {
                        logAndAudit(AssertionMessages.SWAGGER_INVALID_SECURITY_DEFINITION, definition.getType(), operation.getOperationId(), path);
                        return false;
                    }
                    conjunctValid &= type.checkSecurity(httpRequestKnob,definition);
                }

                if (conjunctValid) {
                    return true;
                }
            }

            // We've processed all the RequirementObjects and not satisfied any of them so we return false;
            logAndAudit(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED, operation.getOperationId(), path);
            return false;
        }
        else {
            //security is not required (no securityDefinitions)
            return true;
        }
    }

    /**
     * DE342980 : Validate against Swagger Assertion does not check for authentication when security
     * is specified at root level and not at operation level.
     * <p>
     * operation.getSecurity() is the security at operation level.
     * model.getSecurity() is the security at root level.
     * <p>
     * According to Swagger specification, operation level security(if specified) overrides the root level security.
     * But, when operation level security is not specified(i.e. null), root level security takes effect.
     */
    private List<Map<String, List<String>>> getSecurityRequirements(final Operation operation, final Swagger model) {
        List<Map<String, List<String>>> securityRequirementObjects = operation.getSecurity();

        if (securityRequirementObjects == null) {
            final List<SecurityRequirement> rootLevelSecurityRequirements = model.getSecurity();
            if (rootLevelSecurityRequirements != null) {
                securityRequirementObjects = rootLevelSecurityRequirements.stream().map(SecurityRequirement::getRequirements).collect(Collectors.toList());
            }
        }

        return securityRequirementObjects;
    }

    private String getServiceRoutingUri(PublishedService service) {
        String routingUri = service.getRoutingUri();

        if (routingUri == null || routingUri.length() < 1) {
            return SecureSpanConstants.SERVICE_FILE + service.getId(); // refer to service by its ID
        } else {
            return routingUri.replaceAll("/\\*.*$", "");
        }
    }

    private String extractSwaggerDocFromContextVar(String var, Map<String, Object> varMap, Audit audit) {
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

            final List<String> segments = Arrays.asList(requestUri.split("/"));

            PathDefinition closestMatch = null;

            for (PathDefinition p : pathDefinitions) {
                if (p.segments.size() == segments.size()) {
                    if (p.path.equals(requestUri) && p.segments.equals(segments)) {
                        return p;
                    }

                    if (p.matches(requestUri)) {
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

        public boolean matches(String path) {
            return uriTemplate.matches(path);
        }

        public List<String> getVariableNames() {
            return uriTemplate.getVariableNames();
        }
    }

    private static class SwaggerModelHolder {
        private final Swagger swaggerModel;
        private final PathResolver pathResolver;
        private final int documentHash;

        private SwaggerModelHolder(Swagger swaggerModel, PathResolver pathResolver, int documentHash) {
            this.swaggerModel = swaggerModel;
            this.pathResolver = pathResolver;
            this.documentHash = documentHash;
        }

        public Swagger getSwaggerModel() {
            return swaggerModel;
        }

        public PathResolver getPathResolver() {
            return pathResolver;
        }

        public int getDocumentHash() {
            return documentHash;
        }
    }
}
