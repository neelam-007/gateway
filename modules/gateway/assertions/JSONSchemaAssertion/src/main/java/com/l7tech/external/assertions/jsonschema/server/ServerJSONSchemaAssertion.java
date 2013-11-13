package com.l7tech.external.assertions.jsonschema.server;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.json.JSONSchema;
import com.l7tech.message.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.server.util.res.UrlFinder;
import com.l7tech.util.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the JSONSchemaAssertion.
 *
 * @see com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion
 */
public class ServerJSONSchemaAssertion extends AbstractServerAssertion<JSONSchemaAssertion> {

    public ServerJSONSchemaAssertion(JSONSchemaAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        pattern = (assertion.getResourceInfo() instanceof MessageUrlResourceInfo)? Pattern.compile(linkHeaderPattern): null;
        this.resourceGetter = getResourceGetter(context);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final TargetMessageType target = assertion.getTarget();
        final String messageDesc = assertion.getTargetName();
        final Message message;
        try {
            message = context.getTargetMessage(assertion, true);
            //this message may be a context variable backed Message
        } catch (NoSuchVariableException e) {
            final String msg = "Could not find variable '" + assertion.getOtherTargetMessageVariable() + "'";
            logger.log(Level.WARNING, msg);
            context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, msg);
            return AssertionStatus.FAILED;
        }

        final JSONData jsonInstanceNode;
        try {
            if(message.isJson()){
                jsonInstanceNode = message.getKnob(JsonKnob.class).getJsonData();
            } else {
                //either the Message is backed by a string context variable, or the Message (variable, request or response)
                //is not of content-type application/json. We will still work with this to be lax on what we accept
                final String jsonInstance;
                try {
                    final PartInfo firstPart = message.getMimeKnob().getFirstPart();
                    jsonInstance = new String(IOUtils.slurpStream(firstPart.getInputStream(false)), firstPart.getContentType().getEncoding());
                    if(logger.isLoggable(Level.FINE)){
                        logger.log(Level.FINE, "JSON data received from target '" + messageDesc+"' is not of content-type application/json");
                    }
                } catch (Exception e) {
                    final String msg = "Cannot get JSON data from message target '" + messageDesc +
                            "'. Details: " + ExceptionUtils.getMessage(e);
                    logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                    context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, msg);
                    return AssertionStatus.SERVER_ERROR;
                }
                
                jsonInstanceNode = jsonFactory.newJsonData(jsonInstance);
            }
        } catch (InvalidJsonException e) {
            logAndAudit(AssertionMessages.JSON_INVALID_JSON, messageDesc);
            context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, messageDesc+" data is not well-formed JSON.");
            return AssertionStatus.FAILED;
        }

        final JSONSchema jsonSchema;
        try {
            jsonSchema = getJsonSchema(context, message);
        } catch (Exception e){
            boolean cantRetrieveException = e instanceof ResourceGetter.ResourceParseException ||
                    e instanceof ResourceGetter.ResourceIOException ||
                    e instanceof ParseException;

            if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Unable to retrieve JSON Schema");

            if (cantRetrieveException) {
                logAndAudit(AssertionMessages.JSON_SCHEMA_VALIDATION_IO_ERROR,
                        new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            } else {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            }
            context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, "Cannot retrieve JSON Schema: " + ExceptionUtils.getMessage(e));
            return AssertionStatus.SERVER_ERROR;
        }

        final AssertionResourceInfo ari = assertion.getResourceInfo();
        boolean allowNoSchemaThrough = false;
        if(ari instanceof MessageUrlResourceInfo){
            MessageUrlResourceInfo resourceInfo = (MessageUrlResourceInfo) ari;
            allowNoSchemaThrough = resourceInfo.isAllowMessagesWithoutUrl();
        }
        if(jsonSchema == null && allowNoSchemaThrough){
            //if jsonSchema is null, then it was configured to retrieve a URL from the Message and fall through when none found is allowed
            // If the option to allow this is removed from the UI, then this if statement can be removed
            logger.log(Level.FINE, "No JSON Schema found. Configuration allows for no validation.");
            return AssertionStatus.NONE;
        } else if(jsonSchema == null){
            //this should never happen if resource getters were setup correctly
            final String msg = "No JSON Schema found.";
            logger.log(Level.WARNING, msg);
            context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, msg);
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            logAndAudit(AssertionMessages.JSON_SCHEMA_VALIDATION_VALIDATING, messageDesc);
            final List<String> errorList = jsonSchema.validate(jsonInstanceNode);
            if(!errorList.isEmpty()){
                //validation failed
                for (String error : errorList) {
                    logAndAudit(AssertionMessages.JSON_SCHEMA_VALIDATION_FAILED, error);
                }

                context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, errorList.toArray());
                
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

            logAndAudit(AssertionMessages.JSON_VALIDATION_SUCCEEDED);
            return AssertionStatus.NONE;
        } catch (InvalidJsonException e) {
            logger.log(Level.INFO, "JSON Schema validation failed: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            context.setVariable(JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE, ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
    }

    private JSONSchema getJsonSchema(PolicyEnforcementContext context, Message message)
            throws IOException, InvalidPolicyException, ResourceGetter.ResourceIOException, ResourceGetter.ResourceParseException {
        try {
            return resourceGetter.getResource(message, context.getVariableMap(variablesUsed, getAudit()));
            //don't catch UrlResourceException as it will stop non caught subclasses from propagating out
        } catch (GeneralSecurityException e) {
            throw new InvalidPolicyException("Error accessing JSON Schema resource '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        } catch (ResourceGetter.UrlNotFoundException e) { // should not happen since we are not examining the message
            throw new InvalidPolicyException("URL error accessing JSON Schema resource '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        } catch (ResourceGetter.InvalidMessageException e) { // should not happen since we are not examining the message
            throw new InvalidPolicyException("URL error accessing JSON Schema resource '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        } catch (ResourceGetter.UrlNotPermittedException e) {
            throw new InvalidPolicyException("URL error accessing JSON Schema resource '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            throw new InvalidPolicyException("URL error accessing JSON Schema resource '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        }
    }

    // - PRIVATE

    private ResourceGetter<JSONSchema, Message> getResourceGetter(ApplicationContext context) throws ServerPolicyException {

        final AssertionResourceInfo ri = assertion.getResourceInfo();

        final boolean singleUrlRes = ri instanceof SingleUrlResourceInfo;
        final boolean staticRes = ri instanceof StaticResourceInfo;
        final boolean messageRes = ri instanceof MessageUrlResourceInfo;

        return ResourceGetter.createResourceGetter(assertion,
                assertion.getResourceInfo(),
                (staticRes) ? getResourceObjectFactory() : null,  //if not a static resource, we don't need to supply this factory
                (messageRes) ? getJsonMessageUrlFinder() : null, //only needed for message resources
                (singleUrlRes || messageRes) ? getCache(context) : null, //not needed for static resources
                getAudit());
    }

    private static synchronized HttpObjectCache<JSONSchema> getCache(BeanFactory spring) {
        if (httpObjectCache != null)
            return httpObjectCache;

        GenericHttpClientFactory clientFactory = (GenericHttpClientFactory)spring.getBean("httpClientFactory");
        if (clientFactory == null) throw new IllegalStateException("No httpClientFactory bean");

        Config config = validated( spring.getBean( "serverConfig", Config.class ) );
        httpObjectCache = new HttpObjectCache<JSONSchema>(
                "JSON Schema",
                config.getIntProperty(JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES, 100),
                config.getIntProperty(JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_AGE, 300000),
                config.getIntProperty(JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE, -1),
                clientFactory,
                jsonSchemaObjectFactory,
                HttpObjectCache.WAIT_INITIAL,
                ClusterProperty.asServerConfigPropertyName(JSONSchemaAssertion.PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE));

        return httpObjectCache;
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                //convert the internal name of the cluster property into the user visible name so that log messages
                //can be related back to the correct cluster property.

                if(JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES.equals(key)){
                    return JSONSchemaAssertion.CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES;
                }

                if(JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_AGE.equals(key)){
                    return JSONSchemaAssertion.CPROP_JSON_SCHEMA_CACHE_MAX_AGE;
                }

                if(JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE.equals(key)){
                    return JSONSchemaAssertion.CPROP_JSON_SCHEMA_CACHE_MAX_STALE_AGE;
                }

                if(JSONSchemaAssertion.PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE.equals(key)){
                    return JSONSchemaAssertion.CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE;
                }

                return null;
            }
        } );

        vc.setMinimumValue( JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES, 0 );
        vc.setMaximumValue( JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES, 1000000 );

        vc.setMinimumValue( JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_AGE, 0 );
        vc.setMinimumValue( JSONSchemaAssertion.PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE, -1 );

        vc.setMinimumValue( JSONSchemaAssertion.PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE, 0 );

        return vc;
    }

    private UrlFinder<Message> getJsonMessageUrlFinder() {

        return new UrlFinder<Message>() {

            @Override
            public String findUrl(Message message) throws IOException, ResourceGetter.InvalidMessageException {

                //message may be a request, response or Message variable
                final MimeKnob mimeKnob = message.getMimeKnob();
                final ContentTypeHeader typeHeader = mimeKnob.getOuterContentType();
                String urlString = null;
                if (typeHeader.isJson()) {
                    urlString = typeHeader.getParam("profile");
                }

                if (urlString == null) {
                    //try link header
                    String linkHeaderValue = null;
                    final String link = "Link";
                    final HeadersKnob headersKnob = message.getHeadersKnob();
                    if (headersKnob != null) {
                        final String[] headerValues = headersKnob.getHeaderValues(link);
                        if(headerValues.length > 0){
                            linkHeaderValue = headerValues[0];
                        }
                    }

                    if(linkHeaderValue != null){
                        final Matcher matcher = pattern.matcher(linkHeaderValue);
                        if(matcher.matches() && matcher.groupCount() > 0){
                            urlString = matcher.group(1);
                        }
                    }
                }

                try{
                    URL url = new URL(urlString);
                }
                catch(MalformedURLException e){
                    urlString = null;
                }

                final boolean isFineLoggable = logger.isLoggable(Level.FINE);
                if(urlString != null && isFineLoggable){
                    logger.log(Level.FINE, "URL retrieved from header: " + urlString);
                } else if (isFineLoggable){
                    logger.log(Level.FINE, "No URL found in header");
                }

                return urlString;
            }
        };
    }

    private ResourceObjectFactory<JSONSchema> getResourceObjectFactory(){

        return new ResourceObjectFactory<JSONSchema>() {
            @Override
            public JSONSchema createResourceObject(final String resourceString) throws ParseException {
                try {
                    return createJsonSchema(resourceString);
                } catch (InvalidJsonException e) {
                    throw (ParseException)new ParseException("Unable to parse: " +
                            ExceptionUtils.getMessage(e), 0).initCause(e);
                } catch (IOException e) {
                    throw (ParseException)new ParseException("Unable to parse: " +
                            ExceptionUtils.getMessage(e), 0).initCause(e);
                }
            }

            @Override
            public void closeResourceObject(JSONSchema resourceObject) {
            }
        };
    }

    private static final HttpObjectCache.UserObjectFactory<JSONSchema> jsonSchemaObjectFactory =
            new HttpObjectCache.UserObjectFactory<JSONSchema>() {
                @Override
                public JSONSchema createUserObject(String urlNotUsed, AbstractUrlObjectCache.UserObjectSource responseSource)
                        throws IOException {
                    String response = responseSource.getString(false);
                    try {
                        return createJsonSchema(response);
                    } catch (InvalidJsonException pe) {
                        //Create a ParseException so that the ResourceGetter.getResource() exception handling
                        //can identify the case when the json data could not be parsed. Not including pe as too low level
                        final ParseException parseException = new ParseException("Response contained invalid JSON.", 0);
                        throw new CausedIOException(parseException.getMessage(), parseException);
                    } catch (IOException pe) {
                        throw new CausedIOException(ExceptionUtils.getMessage(pe), pe);
                    }
                }
            };

    private static JSONSchema createJsonSchema(String jsonSchemaString) throws InvalidJsonException, IOException{
        try {
            return jsonFactory.newJsonSchema(jsonSchemaString);
        } catch (InvalidJsonException e) {
            if(logger.isLoggable(Level.FINE)){
                //the actual message is usually not helpful. It's implementation specific and is low level.
                //all the user will normally need to know is that the json is invalid.
                logger.log(Level.FINE, "Invalid JSON. Details: " + ExceptionUtils.getMessage(e));
            }
            throw new InvalidJsonException("JSON schema is invalid");//no full stop due to AssertionMessage #4 formatting.
        }
    }

    private final String[] variablesUsed;
    private static final JSONFactory jsonFactory = JSONFactory.getInstance();
    private ResourceGetter<JSONSchema, Message> resourceGetter;
    private final Pattern pattern;

    /**
     * This pattern is to match values like:
     * <em>&lt;http://json.com/my-hyper-schema&gt; rel="describedby"</em>
     *
     */
    static final String linkHeaderPattern = "<(.*)>\\s*;\\s*rel=\"describedby\"";


    private static HttpObjectCache<JSONSchema> httpObjectCache = null;
    private static final Logger logger = Logger.getLogger(ServerJSONSchemaAssertion.class.getName());
}
