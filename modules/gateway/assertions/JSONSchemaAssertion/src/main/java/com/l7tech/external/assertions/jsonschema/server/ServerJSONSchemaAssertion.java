package com.l7tech.external.assertions.jsonschema.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.json.*;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.JsonKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.url.AbstractUrlObjectCache.UserObjectSource;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.server.util.res.UrlFinder;
import com.l7tech.util.*;
import com.networknt.schema.JsonSchemaException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion.*;
import static com.l7tech.gateway.common.audit.AssertionMessages.JSON_SCHEMA_VALIDATION_VALIDATING;
import static com.l7tech.gateway.common.audit.AssertionMessages.JSON_VALIDATION_SUCCEEDED;
import static com.l7tech.json.JSONSchema.ATTRIBUTE_SCHEMA_VERSION;
import static com.l7tech.json.JsonSchemaVersion.DRAFT_V4;
import static com.l7tech.server.util.res.ResourceGetter.*;

/**
 * Server side implementation of the JSONSchemaAssertion.
 *
 * @see com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion
 */
public class ServerJSONSchemaAssertion extends AbstractServerAssertion<JSONSchemaAssertion> {

    private static final Logger logger = Logger.getLogger(ServerJSONSchemaAssertion.class.getName());

    /**
     * This pattern is to match values like:
     * <em>&lt;http://json.com/my-hyper-schema&gt; rel="describedby"</em>
     */
    static final String LINK_HEADER_PATTERN = "<(.*)>\\s*;\\s*rel=\"describedby\"";

    private static final JSONFactory JSON_FACTORY = JSONFactory.INSTANCE;
    private static final String HEADER_NAME_LINK = "Link";
    private static final String MESSAGE_UNKNOWN_VERSION = "Unknown JSON Schema version URI: {0}. Validating as " +
            "policy configured version {1}";
    private static final String MESSAGE_WRONG_VERSION = "JSON Schema version {0} configured in policy, " +
            "but schema specifies version {1}. Validating as {0}";
    private static final String MESSAGE_WRONG_VERSION_STRICT = "JSON Schema version %s configured in policy, " +
            "but schema specifies version %s. As strict mode is enabled, cannot validate with this JSON schema.";
    private static final String MESSAGE_UNKNOWN_SCHEMA_PROPERTY_MISMATCH = "that does not match the policy configured version";

    private static final ConcurrentMap<JsonSchemaVersion, HttpObjectCache<JSONSchema>> SCHEMA_OBJ_CACHE =
            new ConcurrentHashMap<>(JsonSchemaVersion.values().length);

    private static final String NO_JSON_SCHEMA_FOUND = "No JSON Schema found.";
    static final String MESSAGE_NOT_A_STRING = "Context variable '%s' is not of type String";

    private final String[] variablesUsed;
    private final Pattern pattern;
    private final ResourceGetter<JSONSchema, Message> schemaResourceGetter;

    public ServerJSONSchemaAssertion(final JSONSchemaAssertion assertion, final ApplicationContext context)
            throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        pattern = (assertion.getResourceInfo() instanceof MessageUrlResourceInfo)
                ? Pattern.compile(LINK_HEADER_PATTERN)
                : null;
        this.schemaResourceGetter = getResourceGetter(context);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {
        final Message message = tryFindMessage(context);
        if (message == null) {
            return AssertionStatus.FAILED;
        }

        final JSONSchema jsonSchema;
        try {
            jsonSchema = getJsonSchema(context, message);

        } catch (Exception e) {
            handleGetJsonSchemaException(context, e);
            return AssertionStatus.SERVER_ERROR;
        }

        final JSONData jsonData;
        try {
            jsonData = tryFindJsonData(message, context, assertion.getJsonSchemaVersion());
        } catch (AssertionStatusException e) {
            return AssertionStatus.SERVER_ERROR;

        } catch (InvalidJsonException e) {
            return AssertionStatus.FAILED;
        }

        // if jsonSchema is null, then it was configured to retrieve a URL from the Message
        //      and fall through when none found is allowed
        if (jsonSchema == null && isAllowNoSchemaThrough()) {
            logger.log(Level.FINE, "No JSON Schema found. Configuration allows for no validation.");
            return AssertionStatus.NONE;

        } else if (jsonSchema == null) {
            handleNullJsonSchema(context);
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            logAndAudit(JSON_SCHEMA_VALIDATION_VALIDATING, assertion.getTargetName() + " with " +
                    assertion.getJsonSchemaVersion().toString());
            final List<String> errorList = jsonSchema.validate(jsonData);
            if (!errorList.isEmpty()) {
                //validation failed
                return handleValidationErrors(context, assertion.getTarget(), errorList);
            }

            logAndAudit(JSON_VALIDATION_SUCCEEDED);
            return AssertionStatus.NONE;

        } catch (InvalidJsonException | JsonSchemaException e) {
            logAndAudit(AssertionMessages.JSON_SCHEMA_VALIDATION_IO_ERROR, ExceptionUtils.getMessage(e) +
                    ". Attempted to validate with " + assertion.getJsonSchemaVersion().getDisplayName());
            context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
    }

    private Message tryFindMessage(final PolicyEnforcementContext context) {
        try {
            //this message may be a context variable backed Message
            return context.getTargetMessage(assertion, true);

        } catch (NoSuchVariableException e) {
            final String msg = "Could not find variable '" + assertion.getOtherTargetMessageVariable() + "'";
            logger.log(Level.WARNING, msg);
            context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, msg);
            return null;
        }
    }

    private JSONData tryFindJsonData(final Message message, final PolicyEnforcementContext context,
                                     final JsonSchemaVersion configuredVersion)
            throws IOException, InvalidJsonException, AssertionStatusException {
        final String messageDesc = assertion.getTargetName();

        JSONData data;

        try {
            if (message.isJson()) {
                data = message.getKnob(JsonKnob.class).getJsonData(configuredVersion);
            } else {

                // either the Message is backed by a string context variable, or the Message (variable, request or response)
                // is not of content-type application/json. We will still work with this to be lax on what we accept
                final InputStream messageStream = message.getMimeKnob().getEntireMessageBodyAsInputStream();
                logger.log(Level.FINE, () -> "JSON data received from target '" + messageDesc
                        + "' is not of content-type application/json");

                final String jsonDataString = new String(IOUtils.slurpStream(messageStream));
                data = JSON_FACTORY.newJsonData(jsonDataString, configuredVersion);
            }

            // pre-parse to ensure json is valid
            data.getJsonNode();

            return data;

        } catch (InvalidJsonException e) {
            logAndAudit(AssertionMessages.JSON_INVALID_JSON, "Attempted to validate with " +
                    assertion.getJsonSchemaVersion().getDisplayName() + ". " + messageDesc);
            context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, ExceptionUtils.getMessage(e));
            throw e;

        } catch (Exception e) {
            final String msg = "Cannot get JSON data from message target '" + messageDesc +
                    "'. Details: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, msg);
            throw new AssertionStatusException(e);
        }
    }

    private JSONSchema getJsonSchema(final PolicyEnforcementContext context, final Message message)
            throws IOException, InvalidPolicyException, ResourceIOException, ResourceParseException {

        final Map<String, Object> varMap = context.getVariableMap(variablesUsed, getAudit());
        final AssertionResourceInfo resrcInfo = assertion.getResourceInfo();
        if (resrcInfo instanceof StaticResourceInfo) {
            // ResourceGetter only supports context variables of type string or type message via mainpart suffix
            for (String varName : Syntax.getReferencedNames(((StaticResourceInfo) resrcInfo).getDocument())) {
                Object var = varMap.get(varName);

                // if a context var is used directly, we only allow for String type
                if (var != null) {
                    if (!(varMap.get(varName) instanceof String)) {
                        throw new InvalidPolicyException(String.format(MESSAGE_NOT_A_STRING, varName));
                    }
                }
            }
        }

        try {
            return schemaResourceGetter.getResource(message, varMap);

        } catch (GeneralSecurityException e) {
            throw new InvalidPolicyException("Error accessing JSON Schema resource '" + ExceptionUtils.getMessage(e)
                    + "'.", ExceptionUtils.getDebugException(e));

        } catch (UrlNotFoundException | InvalidMessageException | MalformedResourceUrlException
                | UrlNotPermittedException e) { // should not happen since we are not examining the message
            throw new InvalidPolicyException("URL error accessing JSON Schema resource '"
                    + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        }
    }

    private void handleGetJsonSchemaException(final PolicyEnforcementContext context, final Exception e) {
        final boolean cantRetrieveException = e instanceof ResourceParseException || e instanceof ResourceIOException
                || e instanceof ParseException;

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Unable to retrieve JSON Schema");
        }

        if (cantRetrieveException) {
            logAndAudit(AssertionMessages.JSON_SCHEMA_VALIDATION_IO_ERROR,
                    new String[]{ExceptionUtils.getMessage(e) + " Attempted to validate with " +
                            assertion.getJsonSchemaVersion().getDisplayName()}, ExceptionUtils.getDebugException(e));
        } else {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
        }

        context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, "Cannot retrieve JSON Schema: "
                + ExceptionUtils.getMessage(e));
    }

    private boolean isAllowNoSchemaThrough() {
        final AssertionResourceInfo ari = assertion.getResourceInfo();
        return (ari instanceof MessageUrlResourceInfo) && ((MessageUrlResourceInfo) ari).isAllowMessagesWithoutUrl();
    }

    // this should never happen if resource getters were setup correctly
    private void handleNullJsonSchema(final PolicyEnforcementContext context) {
        logger.log(Level.WARNING, NO_JSON_SCHEMA_FOUND);
        context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, NO_JSON_SCHEMA_FOUND);
    }

    private AssertionStatus handleValidationErrors(final PolicyEnforcementContext context,
            final TargetMessageType target, final List<String> errorList) {
        errorList.forEach(error -> logAndAudit(AssertionMessages.JSON_SCHEMA_VALIDATION_FAILED, error +
                ": Attempted to validate with " + assertion.getJsonSchemaVersion().getDisplayName()));
        context.setVariable(JSON_SCHEMA_FAILURE_VARIABLE, errorList.toArray());
        switch (target) {
            case REQUEST:
                return AssertionStatus.BAD_REQUEST;
            case RESPONSE:
                return AssertionStatus.BAD_RESPONSE;
            case OTHER:
                return AssertionStatus.FAILED;
            default:
                return AssertionStatus.FAILED;
        }
    }

    private ResourceGetter<JSONSchema, Message> getResourceGetter(final ApplicationContext context)
            throws ServerPolicyException {

        final AssertionResourceInfo ri = assertion.getResourceInfo();

        final boolean singleUrlRes = ri instanceof SingleUrlResourceInfo;
        final boolean staticRes = ri instanceof StaticResourceInfo;
        final boolean messageRes = ri instanceof MessageUrlResourceInfo;

        return ResourceGetter.createResourceGetter(assertion,
                assertion.getResourceInfo(),
                (staticRes) ? getResourceObjectFactory() : null, //if not a static resource, we don't need to supply this factory
                (messageRes) ? getJsonMessageUrlFinder() : null, //only needed for message resources
                (singleUrlRes || messageRes) ? getCache(context, assertion.getJsonSchemaVersion()) : null, //not needed for static resources
                getAudit());
    }

    private static HttpObjectCache<JSONSchema> createCache(final BeanFactory spring,
                                                           final JsonSchemaVersion configuredVersion) {
        final GenericHttpClientFactory clientFactory = (GenericHttpClientFactory) spring.getBean("httpClientFactory");
        if (clientFactory == null) {
            throw new IllegalStateException("No httpClientFactory bean");
        }

        final Config config = validated(spring.getBean("serverConfig", Config.class));
        return new HttpObjectCache<>(
                "JSON Schema: " + configuredVersion,
                config.getIntProperty(PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES, 100),
                config.getIntProperty(PARAM_JSON_SCHEMA_CACHE_MAX_AGE, 300000),
                config.getIntProperty(PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE, -1),
                clientFactory,
                (u, responseSource) -> createJsonSchema(responseSource, configuredVersion),
                HttpObjectCache.WAIT_INITIAL,
                ClusterProperty.asServerConfigPropertyName(PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE));
    }

    private static HttpObjectCache<JSONSchema> getCache(final BeanFactory spring, final JsonSchemaVersion configuredVersion) {
        return SCHEMA_OBJ_CACHE.computeIfAbsent(configuredVersion, v -> createCache(spring, v));
    }

    private static Config validated(final Config config) {
        final ValidatedConfig vc = new ValidatedConfig(config, logger, key -> {
            //convert the internal name of the cluster property into the user visible name so that log messages
            //can be related back to the correct cluster property.

            if (PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES.equals(key)) {
                return CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES;
            }

            if (PARAM_JSON_SCHEMA_CACHE_MAX_AGE.equals(key)) {
                return CPROP_JSON_SCHEMA_CACHE_MAX_AGE;
            }

            if (PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE.equals(key)) {
                return CPROP_JSON_SCHEMA_CACHE_MAX_STALE_AGE;
            }

            if (PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE.equals(key)) {
                return CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE;
            }

            if (PARAM_JSON_SCHEMA_VERSION_STRICT.equals(key)) {
                return CPROP_JSON_SCHEMA_VERSION_STRICT;
            }

            return null;
        });

        vc.setMinimumValue(PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES, 0);
        vc.setMaximumValue(PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES, 1000000);

        vc.setMinimumValue(PARAM_JSON_SCHEMA_CACHE_MAX_AGE, 0);
        vc.setMinimumValue(PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE, -1);

        vc.setMinimumValue(PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE, 0);

        return vc;
    }

    private UrlFinder<Message> getJsonMessageUrlFinder() {
        return message -> {

            // message may be a request, response or Message variable
            final MimeKnob mimeKnob = message.getMimeKnob();
            final ContentTypeHeader typeHeader = mimeKnob.getOuterContentType();
            String urlString = null;
            if (typeHeader.isJson()) {
                urlString = typeHeader.getParam("profile");
            }

            if (urlString == null) {
                // try link header
                String linkHeaderValue = null;
                final HeadersKnob headersKnob = message.getHeadersKnob();
                final String[] headerValues = headersKnob.getHeaderValues(HEADER_NAME_LINK, HeadersKnob.HEADER_TYPE_HTTP);
                if (headerValues.length > 0) {
                    linkHeaderValue = headerValues[0];
                }

                if (linkHeaderValue != null) {
                    final Matcher matcher = pattern.matcher(linkHeaderValue);
                    if (matcher.matches() && matcher.groupCount() > 0) {
                        urlString = matcher.group(1);
                    }
                }
            }

            try {
                URL url = new URL(urlString);
            } catch (MalformedURLException e) {
                urlString = null;
            }

            final boolean isFineLoggable = logger.isLoggable(Level.FINE);
            if (urlString != null && isFineLoggable) {
                logger.log(Level.FINE, "URL retrieved from header: " + urlString);

            } else if (isFineLoggable) {
                logger.log(Level.FINE, "No URL found in header");
            }

            return urlString;
        };
    }

    private ResourceObjectFactory<JSONSchema> getResourceObjectFactory() {
        return new ResourceObjectFactory<JSONSchema>() {
            @Override
            public JSONSchema createResourceObject(final String resourceString) throws ParseException {
                Pair<String, JSONData> schemaUriAndParse = null;
                final String schemaUri;
                try {
                    schemaUriAndParse = findVersionString(resourceString);
                } catch (InvalidJsonException e) {
                    // We allow this to continue to support backwards compatibility for v2
                    logger.log(Level.INFO, "Unable to determine schema version due to invalid JSON schema: " + ExceptionUtils.getMessage(e));
                }

                schemaUri = (schemaUriAndParse == null) ? null : schemaUriAndParse.left;
                final JsonSchemaVersion configuredVersion = assertion.getJsonSchemaVersion();

                final ServerConfig serverConfig = ServerConfig.getInstance();
                final boolean strictJsonSchemaVersionEnabled =
                        serverConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false);

                if (strictJsonSchemaVersionEnabled && schemaUri != null && !configuredVersion.matchesSchemaUri(schemaUri)) {
                    return new ExceptionThrowingJsonSchema(() -> new InvalidJsonException(
                            String.format(MESSAGE_WRONG_VERSION_STRICT, configuredVersion, versionUriAsString(schemaUri))
                    ));
                }

                try {
                    return new DelegatingJsonSchema(JSON_FACTORY.newJsonSchema(resourceString, configuredVersion)) {
                        @Override
                        public void beforeValidate() throws InvalidJsonException {
                            logVersionMismatchWarnings(schemaUri, configuredVersion);
                        }
                    };

                } catch (InvalidJsonException | IOException | JsonSchemaException e) {
                    final String message = "Unable to parse JSON schema: " + ExceptionUtils.getMessage(e);

                    // Create an exception throwing Json Schema so that the error is logged during policy execution time
                    return new ExceptionThrowingJsonSchema(() -> new InvalidJsonException(message));
                }
            }

            @Override
            public void closeResourceObject(JSONSchema resourceObject) {
            }
        };
    }

    private static JSONSchema createJsonSchema(final UserObjectSource responseSource,
                                               final JsonSchemaVersion configuredVersion)
            throws IOException {
        final String response = responseSource.getString(false);
        try {
            return createJsonSchema(response, configuredVersion);

        } catch (InvalidJsonException pe) {
            //Create a ParseException so that the ResourceGetter.getResource() exception handling
            //can identify the case when the json data could not be parsed. Not including pe as too low level
            final ParseException parseException = new ParseException("Response contained invalid JSON. " + pe.getMessage(), 0);
            throw new CausedIOException(parseException.getMessage(), parseException);

        } catch (IOException pe) {
            throw new CausedIOException(ExceptionUtils.getMessage(pe), pe);
        }
    }

    private static JSONSchema createJsonSchema(final String jsonSchemaString, final JsonSchemaVersion configuredVersion)
            throws InvalidJsonException, IOException {
        try {
            final Pair<String, JSONData> schemaUriAndParse = findVersionString(jsonSchemaString);
            final String schemaUri = schemaUriAndParse.left;

            final ServerConfig serverConfig = ServerConfig.getInstance();
            final boolean strictJsonSchemaVersionEnabled =
                    serverConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false);

            if (strictJsonSchemaVersionEnabled && schemaUri != null && !configuredVersion.matchesSchemaUri(schemaUri)) {
                return new ExceptionThrowingJsonSchema(() -> new InvalidJsonException(
                        String.format(MESSAGE_WRONG_VERSION_STRICT, configuredVersion, versionUriAsString(schemaUri))
                ));
            }
            return new DelegatingJsonSchema(JSON_FACTORY.newJsonSchema(jsonSchemaString, configuredVersion)) {
                @Override
                public void beforeValidate() throws InvalidJsonException {
                    logVersionMismatchWarnings(schemaUri, configuredVersion);
                }
            };

        } catch (InvalidJsonException e) {
            if (logger.isLoggable(Level.FINE)) {
                // the actual message is usually not helpful. It's implementation specific and is low level.
                // all the user will normally need to know is that the json is invalid.
                logger.log(Level.FINE, "Invalid JSON. Details: " + ExceptionUtils.getMessage(e));
            }
            throw e;
        }
    }

    private static String versionUriAsString(String schemaUri) {
        final JsonSchemaVersion requestedVersion = JsonSchemaVersion.fromUri(schemaUri);
        final String requestedVersionString;
        if (requestedVersion == null) {
            requestedVersionString = MESSAGE_UNKNOWN_SCHEMA_PROPERTY_MISMATCH;
        } else {
            requestedVersionString = requestedVersion.toString();
        }
        return requestedVersionString;
    }

    @NotNull
    private static Pair<String, JSONData> findVersionString(String jsonSchemaString) throws InvalidJsonException {
        final JSONData schemaAsJSONData = JSON_FACTORY.newJsonData(jsonSchemaString, DRAFT_V4);
        final JsonNode schemaAsNode = schemaAsJSONData.getJsonNode();
        final JsonNode schemaUriNode = schemaAsNode.get(ATTRIBUTE_SCHEMA_VERSION);
        return schemaUriNode != null
                ? new Pair<>(schemaUriNode.asText(), schemaAsJSONData)
                : new Pair<>(null, schemaAsJSONData);
    }

    private static void logVersionMismatchWarnings(final String schemaUri, final JsonSchemaVersion configuredVersion)
            throws InvalidJsonException {
        // if a $schemaURI is present AND it's not the configured version, we need to log a warning...
        if (schemaUri != null && !configuredVersion.matchesSchemaUri(schemaUri)) {
            // if it's a known version, we log a message about the mismatch
            if (JsonSchemaVersion.isKnown(schemaUri)) {
                final JsonSchemaVersion requestedVersion = JsonSchemaVersion.fromUri(schemaUri);
                logger.log(Level.WARNING, MESSAGE_WRONG_VERSION, new Object[]{configuredVersion, requestedVersion});
            } else {
                // if it's an unknown version, we log a message about it being unknown
                logger.log(Level.WARNING, MESSAGE_UNKNOWN_VERSION, new Object[]{schemaUri, configuredVersion});
            }
        }
        // if no schema is present or it's present and matches policy configuration, proceed with validation as normal
    }

    private static class ExceptionThrowingJsonSchema implements JSONSchema {

        private final Supplier<InvalidJsonException> exception;

        ExceptionThrowingJsonSchema(@NotNull final Supplier<InvalidJsonException> exception) {
            this.exception = exception;
        }

        @Override
        public List<String> validate(JSONData jsonData) throws InvalidJsonException {
            throw exception.get();
        }
    }

    private static abstract class DelegatingJsonSchema implements JSONSchema {
        final JSONSchema jsonSchema;

        DelegatingJsonSchema(JSONSchema jsonSchema) {
            this.jsonSchema = jsonSchema;
        }

        @Override
        public List<String> validate(JSONData jsonData) throws InvalidJsonException {
            beforeValidate();
            return jsonSchema.validate(jsonData);
        }

        abstract void beforeValidate() throws InvalidJsonException;
    }
}
