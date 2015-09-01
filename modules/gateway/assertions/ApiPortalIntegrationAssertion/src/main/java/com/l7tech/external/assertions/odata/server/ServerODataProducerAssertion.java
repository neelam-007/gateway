package com.l7tech.external.assertions.odata.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.odata.ODataProducerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.helpers.IOUtils;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.exceptions.MethodNotAllowedException;
import org.odata4j.exceptions.NotImplementedException;
import org.odata4j.exceptions.ServerErrorException;
import org.odata4j.producer.resources.ODataEntitiesRequestResource;
import org.odata4j.producer.resources.TransValueHolder;
import org.springframework.context.ApplicationContext;

/**
 * Server side implementation of the ODataProducerAssertion.
 *
 * @see com.l7tech.external.assertions.odata.ODataProducerAssertion
 */
public class ServerODataProducerAssertion extends AbstractServerAssertion<ODataProducerAssertion> {
    private final String[] variablesUsed;
    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private final JdbcConnectionManager jdbcConnectionManager;
    private final ODataEntitiesRequestResource entitiesRequestResource;
    private final ODataEntityRequestResource entityRequestResource;
    private final ODataMetadataResource metadataResource;
    private final ODataServiceDocumentResource serviceDocumentResource;
    private final ODataExceptionMappingProvider exceptionMappingProvider;
    private final JdbcModelCache modelCache;

    public ServerODataProducerAssertion(ODataProducerAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        this(assertion, context, ODataEntitiesRequestResource.getInstance(), ODataEntityRequestResource.getInstance(),
                ODataMetadataResource.getInstance(), ODataServiceDocumentResource.getInstance(), ODataExceptionMappingProvider.getInstance(), JdbcModelCache.getInstance());
    }

    public ServerODataProducerAssertion(ODataProducerAssertion assertion, ApplicationContext context,
                                        ODataEntitiesRequestResource entitiesRequestResource,
                                        ODataEntityRequestResource entityRequestResource,
                                        ODataMetadataResource metadataResource,
                                        ODataServiceDocumentResource serviceDocumentResource,
                                        ODataExceptionMappingProvider exceptionMappingProvider, JdbcModelCache modelCache) throws PolicyAssertionException {
        super(assertion);

        if (context == null) {
            throw new IllegalStateException("Application context cannot be null.");
        }

        //variablesUsed = assertion.getVariablesUsed();
        variablesUsed = Syntax.getReferencedNames(assertion.getConnectionName(), ODataProducerAssertion.ODATA_ROOT_URI, ODataProducerAssertion.ODATA_RESOURCE_PATH, ODataProducerAssertion.ODATA_SERVICE_PATH_INDEX, ODataProducerAssertion.ODATA_QUERY_OPTIONS, ODataProducerAssertion.ODATA_SHOW_INLINE_ERROR,
                ODataProducerAssertion.ODATA_PARAM_INLINECOUNT, ODataProducerAssertion.ODATA_PARAM_TOP, ODataProducerAssertion.ODATA_PARAM_SKIP, ODataProducerAssertion.ODATA_PARAM_FILTER, ODataProducerAssertion.ODATA_PARAM_ORDERBY,
                ODataProducerAssertion.ODATA_PARAM_FORMAT, ODataProducerAssertion.ODATA_PARAM_CALLBACK, ODataProducerAssertion.ODATA_PARAM_SKIPTOKEN, ODataProducerAssertion.ODATA_PARAM_EXPAND, ODataProducerAssertion.ODATA_PARAM_SELECT,
                ODataProducerAssertion.ODATA_ALLOW_ANY_REQUEST_BODY_FOR_BATCH, ODataProducerAssertion.ODATA_CUSTOM_ENTITIES, ODataProducerAssertion.ODATA_BATCH_TRANSACTION, ODataProducerAssertion.ODATA_BATCH_FAST_FAIL);
        jdbcConnectionPoolManager = context.getBean("jdbcConnectionPoolManager", JdbcConnectionPoolManager.class);
        jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
        this.entitiesRequestResource = entitiesRequestResource;
        this.entityRequestResource = entityRequestResource;
        this.metadataResource = metadataResource;
        this.serviceDocumentResource = serviceDocumentResource;
        this.exceptionMappingProvider = exceptionMappingProvider;
        this.modelCache = modelCache;

        if (assertion.getConnectionName() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        if (context == null) {
            throw new IllegalStateException("Policy Enforcement Context cannot be null.");
        }

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        final String connName = ExpandVariables.process(assertion.getConnectionName(), variableMap, getAudit());
        //validate that the connection exists.
        final JdbcConnection connection;
        try {
            connection = jdbcConnectionManager.getJdbcConnection(connName);
            if (connection == null) {
                throw new FindException();
            }
        } catch (FindException e) {
            String errorMsg = "Could not find JDBC connection: " + connName;
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, errorMsg);
            return AssertionStatus.FAILED;
        }

        DataSource dataSource;
        try {
            dataSource = jdbcConnectionPoolManager.getDataSource(connName);
            if (dataSource == null) {
                throw new FindException();
            }
        } catch (Exception e) {
            String errorMsg1 = "Count not get a DataSource from the pool: " + connName;
            String errorMsg2 = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg1, errorMsg2});
            return AssertionStatus.FAILED;
        }

        return process(context, variableMap, dataSource);
    }


    //TODO: rraquepo - eventually i would want this method to not have any reference to PEX, so i can move it elsewhere
    protected AssertionStatus process(PolicyEnforcementContext context, Map<String, Object> variableMap, DataSource dataSource) throws IOException, PolicyAssertionException {
        String rootUri, resourcePath, queryOptions, servicePathIndex, httpMethod;
        String inlineCount, top, skip, filter, orderBy, format, callback, skipToken, expand, select;
        String allowAnyRequestBodyForBatch, customEntities = null, batchTransaction, batchFastFail;
        boolean allowAnyRequestBodyForBatchFlag = true;
        boolean batchTransactionFlag = true, batchFastFailFlag = true;
        rootUri = ExpandVariables.process(ODataProducerAssertion.ODATA_ROOT_URI, variableMap, getAudit());
        servicePathIndex = ExpandVariables.process(ODataProducerAssertion.ODATA_SERVICE_PATH_INDEX, variableMap, getAudit());
        queryOptions = ExpandVariables.process(ODataProducerAssertion.ODATA_QUERY_OPTIONS, variableMap, getAudit());
        resourcePath = ExpandVariables.process(ODataProducerAssertion.ODATA_RESOURCE_PATH, variableMap, getAudit());
        httpMethod = ExpandVariables.process(ODataProducerAssertion.ODATA_HTTP_METHOD, variableMap, getAudit());
        inlineCount = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_INLINECOUNT, variableMap, getAudit());
        top = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_TOP, variableMap, getAudit());
        skip = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_SKIP, variableMap, getAudit());
        filter = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_FILTER, variableMap, getAudit());
        orderBy = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_ORDERBY, variableMap, getAudit());
        format = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_FORMAT, variableMap, getAudit());
        callback = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_CALLBACK, variableMap, getAudit());
        skipToken = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_SKIPTOKEN, variableMap, getAudit());
        expand = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_EXPAND, variableMap, getAudit());
        select = ExpandVariables.process(ODataProducerAssertion.ODATA_PARAM_SELECT, variableMap, getAudit());
        allowAnyRequestBodyForBatch = ExpandVariables.process(ODataProducerAssertion.ODATA_ALLOW_ANY_REQUEST_BODY_FOR_BATCH, variableMap, getAudit());
        batchTransaction = ExpandVariables.process(ODataProducerAssertion.ODATA_BATCH_TRANSACTION, variableMap, getAudit());
        batchFastFail = ExpandVariables.process(ODataProducerAssertion.ODATA_BATCH_FAST_FAIL, variableMap, getAudit());
        try {
            Object obj = ExpandVariables.processSingleVariableAsObject(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES, variableMap, getAudit());
            if (obj != null) {
                if (obj instanceof String) {
                    customEntities = (String) obj;
                } else {
                    customEntities = IOUtils.toString(getPayload((Message) obj));
                }
            }
        } catch (Exception e) {
            logger.fine(ExceptionUtils.getMessageWithCause(e));
            return AssertionStatus.FALSIFIED;
        }

        if ("false".equalsIgnoreCase(allowAnyRequestBodyForBatch)) {
            allowAnyRequestBodyForBatchFlag = false;
        }
        if ("false".equalsIgnoreCase(batchTransaction)) {
            batchTransactionFlag = false;
            if ("false".equalsIgnoreCase(batchFastFail)) {
                batchFastFailFlag = false;
            }
        } else {
            batchTransactionFlag = true;
            batchFastFailFlag = true;
        }

        //final ODataProducer producer = contextResolver.getContext(ODataProducer.class);
        final Message request = context.getRequest();
        final Message response = context.getResponse();
        final HttpRequestKnob httpRequestKnob = getHttpRequestKnob(request);
        final HttpHeaders httpHeaders = new JaxRsHttpHeaders(httpRequestKnob);//TODO: properly build this http header
        final JaxRsUriInfo uriInfo;
        javax.ws.rs.core.Response odataResponse = null;

        boolean indexRequest = false;
        try {
            int expected_service_path_index = 1;
            if (servicePathIndex != null && servicePathIndex.length() >= 1 && Integer.parseInt(servicePathIndex) > 1) {
                expected_service_path_index = Integer.parseInt(servicePathIndex);
            }
            if (rootUri != null && rootUri.length() > 1) {
                uriInfo = new JaxRsUriInfo(rootUri, queryOptions);
            } else {
                URI uri = httpRequestKnob.getRequestURL().toURI();//http://chardportal25.l7tech.com:8080/myodata(1)
                String uriStr = uri.toString();
                String uriHost = uri.getHost();
                int slashLocation = uriStr.indexOf("/", uriHost.length() + 8);//8 is to account for https://
                int ctr = 0;
                while (ctr < expected_service_path_index) {
                    slashLocation = uriStr.indexOf("/", slashLocation + 1);
                    if (slashLocation == -1) {
                        indexRequest = true;
                        slashLocation = uriHost.length() + 8;//8 is to account for https://
                    }
                    ctr++;
                }
                //check if it just ends with /
                if (uriStr.endsWith("/") && slashLocation + 1 == uriStr.length()) {
                    indexRequest = true;
                }
                uriInfo = new JaxRsUriInfo(uriStr.substring(0, slashLocation) + "/", queryOptions);
            }
            if (isEmpty(resourcePath)) {
                resourcePath = URLDecoder.decode(httpRequestKnob.getRequestURL().toURI().toString().replace(uriInfo.getBaseUri().toString(), ""), "UTF-8");
            } else if (resourcePath != null && resourcePath.trim().equals("/")) { //should only be a case where assertion is being scripted in the policy
                resourcePath = "";
                indexRequest = true;
            }
            if (isEmpty(queryOptions)) {
                queryOptions = httpRequestKnob.getQueryString();
                if (isEmpty(queryOptions)) {//this cannot be null, it's just so easier later if it's an empty string
                    queryOptions = "";
                }
                if (!isEmpty(queryOptions)) {
                    String queryOptionsDecoded = URLDecoder.decode(queryOptions, "UTF-8");
                    if (!queryOptionsDecoded.equals(queryOptions) || isEmpty(uriInfo.getQueryOptions())) {
                        //if we found some encoded values we need to update our custom uri object or it's empty
                        uriInfo.setQueryOptions(queryOptionsDecoded);
                    }
                }
            }
            if (isEmpty(httpMethod)) {
                httpMethod = httpRequestKnob.getMethod().toString();
            }
            //TODO: validate httpMethod should only be GET, PUT, DELETE, POST???
        } catch (Exception e) {
            String errorMsg = "Unable to extract odata parameters " + e.getMessage();
            logAndAudit(AssertionMessages.EXCEPTION_INFO, errorMsg);
            return AssertionStatus.FAILED;
        }


        //process queryOptions  and parameters
        while (queryOptions.startsWith("?")) {
            queryOptions = queryOptions.substring(1);
        }
        final String[] options = queryOptions.split("&");
        inlineCount = processParam("$inlinecount", inlineCount, options);
        top = processParam("$top", top, options);
        skip = processParam("$skip", skip, options);
        filter = processParam("$filter", filter, options);
        orderBy = processParam("$orderby", orderBy, options);
        format = processParam("$format", format, options);
        callback = processParam("$callback", callback, options);
        skipToken = processParam("$skiptoken", skipToken, options);
        expand = processParam("$expand", expand, options);
        select = processParam("$select", select, options);


        //since we can't rely on jax-rs annotation for processing incoming request, we need to handle the actual call to correct odata4j resources
        //EntitiesRequestResource @Path("{entitySetName: [^/()]+?}{ignoreParens: (?:\\(\\))?}")
        //EntityRequestResource @Path("{entitySetName: [^/()]+?}{id: \\([^/()]+?\\)}")
        //MetadataResource @Path("{first: \\$}metadata")
        //and a lot more... see packages org.odata4j.producer.resources for supported resource mappings
        String entityId = null;
        final String[] resourcePathSplit = resourcePath.split("/");
        String entitySetName = resourcePathSplit[0];

        final JaxRsContextResolver contextResolver;
        boolean isBatch = false;
        if (httpMethod.equals("POST") && resourcePathSplit.length > 0 && "$batch".equals(resourcePathSplit[resourcePathSplit.length - 1])) {
            isBatch = true;
        }
        if (batchTransactionFlag && isBatch) {
            contextResolver = new JaxRsContextResolver(dataSource, modelCache, customEntities, true);
        } else {
            contextResolver = new JaxRsContextResolver(dataSource, modelCache, customEntities, false);
        }


        if ("$metadata".equals(resourcePath)) {
            odataResponse = metadataResource.getMetadata(httpHeaders, uriInfo, contextResolver, format);
            processResponse(odataResponse, response, format, null);
            return AssertionStatus.NONE;
        } else if (indexRequest && isEmpty(resourcePath)) {
            odataResponse = serviceDocumentResource.getServiceDocument(httpHeaders, uriInfo, contextResolver, format, callback);
            processResponse(odataResponse, response, format, null);
            return AssertionStatus.NONE;
        }


        //TODO we can probably find a valid regex for this one
        //EntitiesRequestResource_ignoreParens_Pattern doesn't seem to work in some cases
        int x = entitySetName.indexOf("(");
        String checkString = null;
        if (x > 0) {
            checkString = entitySetName.substring(0, x) + entitySetName.substring(x).replaceAll(" ", "");
        }
        if (entitySetName.endsWith("()")) {
            entitySetName = entitySetName.replace("()", "");
        } else if (!isEmpty(checkString) && checkString.endsWith("()")) {
            entitySetName = checkString.replace("()", "");
        } else {
            //extract id
            Matcher entityMatcher = EntityRequestResource_id_Pattern.matcher(entitySetName);
            if (entityMatcher.find()) {
                entityId = entityMatcher.group();
                entitySetName = entitySetName.replace(entityId, "");
            }
        }
//TODO: commenting this block just in case we want to validate the entitySetName ourselves???
//        EdmEntitySet entitySet = producer.getMetadata().findEdmEntitySet(entitySetName);
//        if (entitySet == null) {
//            throw new NotFoundException();
//        }

        Map<String, String> headers = new HashMap<>();//extra headers need to be set, initially implemented for batch related output

        //begin processing
        try {
            if (!isEmpty(skipToken)) {
                //we don't even support skipToken downstream
                throw new ServerErrorException("A skip token can only be provided in a query request against an entity set when the entity set has a paging limit set.");
            }
            if (resourcePathSplit.length > 2) {
                throw new BadRequestException();
            }
            if (entityId != null) {
                uriInfo.setPath(entitySetName + entityId);
                //TODO: add support for the ff: they will have they're own GET,PUT,POST,etc
                //@Path("{first: \\$}links/{targetNavProp:.+?}{targetId: (\\(.+?\\))?}")
                //@Path("{first: \\$}value")
                //@Path("{navProp: .+}")
                //@Path("{navProp: .+?}{optionalParens: ((\\(\\)))}")
                if (httpMethod.equals("GET")) {
                    if (resourcePathSplit.length > 1) {
                        if ("$metadata".equals(resourcePathSplit[1])) {
                            odataResponse = metadataResource.getMetadataEntity(httpHeaders, uriInfo, contextResolver, entitySetName, entityId, format, callback, expand, select);
                        } else if ("$links".equals(resourcePathSplit[1])) {
                            throw new NotImplementedException();
                        } else {
                            throw new NotImplementedException();
                        }
                    } else {
                        odataResponse = entityRequestResource.getEntity(httpHeaders, uriInfo, contextResolver, entitySetName, entityId, format, callback, expand, select);
                    }
                } else if (httpMethod.equals("DELETE")) {
                            /*
                            (@javax.ws.rs.core.Context javax.ws.rs.core.HttpHeaders httpHeaders,
                            @javax.ws.rs.core.Context javax.ws.rs.core.UriInfo uriInfo,
                            @javax.ws.rs.core.Context javax.ws.rs.ext.ContextResolver<org.odata4j.producer.ODataProducer> producerResolver,
                            @javax.ws.rs.QueryParam("$format") java.lang.String format,
                            @javax.ws.rs.QueryParam("$callback") java.lang.String callback,
                            @javax.ws.rs.PathParam("entitySetName") java.lang.String entitySetName,
                            @javax.ws.rs.PathParam("id") java.lang.String id)
                            */
                    odataResponse = entityRequestResource.deleteEntity(httpHeaders, uriInfo, contextResolver, format, callback, entitySetName, entityId);
                } else if (httpMethod.equals("PUT")) {
                    final InputStream payload = getPayload(request);
                    odataResponse = entityRequestResource.updateEntity(httpHeaders, uriInfo, contextResolver, entitySetName, entityId, payload);
                } else if (httpMethod.equals("POST")) {
                    final InputStream payload = getPayload(request);
                    final String payloadAsString = IOUtils.toString(payload, "UTF-8");
                    //TODO: merge expects X-HTTP-METHOD to be set
                    odataResponse = entityRequestResource.mergeEntity(httpHeaders, uriInfo, contextResolver, entitySetName, entityId, payloadAsString);
                } else {
                    throw new MethodNotAllowedException();
                }
            } else { // Entities related [start]
                uriInfo.setPath(entitySetName);
                if (httpMethod.equals("GET")) {
                    if (resourcePathSplit.length > 1 && "$count".equals(resourcePathSplit[1])) {
                        odataResponse = entitiesRequestResource.getEntitiesCount(httpHeaders, uriInfo, contextResolver, entitySetName, resourcePathSplit[1], inlineCount, top, skip, filter, orderBy, format, callback, skipToken, expand, select);
                    } else if (resourcePathSplit.length > 1 && "$metadata".equals(resourcePathSplit[1])) {
                        //TODO: although the assertion allows EntitySet/$metadata uri, the underlying producer will actually fail, as not supported
                        odataResponse = metadataResource.getMetadataEntity(httpHeaders, uriInfo, contextResolver, entitySetName, entityId, format, callback, expand, select);
                    } else if (resourcePathSplit.length > 1) {
                        //TODO:everything else after the EntitySet name, we'll just consider BadRequest or NotImplementedException???
                        throw new BadRequestException();
                    } else {
                        odataResponse = entitiesRequestResource.getEntities(httpHeaders, uriInfo, contextResolver, entitySetName, inlineCount, top, skip, filter, orderBy, format, callback, skipToken, expand, select);
                    }
                } else if (httpMethod.equals("POST")) {
                    final InputStream payload = getPayload(request);
                    final InputStream payloadCheck = getPayload(request);//we need a separate one since we need to check the size
                    if ("$batch".equals(resourcePathSplit[resourcePathSplit.length - 1])) {
                        if ((!allowAnyRequestBodyForBatchFlag && !request.getMimeKnob().isMultipart()) || IOUtils.toString(payloadCheck).length() == 0) {
                            throw new BadRequestException("Expecting a valid multipart request and payload");
                        }
                        final TransValueHolder transObj = new TransValueHolder(batchFastFailFlag);
                        odataResponse = entitiesRequestResource.processBatch(contextResolver, httpHeaders, uriInfo, null, format, callback, payload, transObj);
                        if (batchTransactionFlag) {
                            if (!transObj.isHasError()) {
                                contextResolver.commit();
                            } else {
                                contextResolver.rollback();
                            }
                        }
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_NAME, transObj.getLastOperationEntityName());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_ID, transObj.getLastOperationEntityId());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_METHOD, transObj.getLastOperationMethod());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_STATUS, transObj.getLastOperationStatus());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_BODY, transObj.getLastOperationBody());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_PAYLOAD, transObj.getLastOperationPayload());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_HAS_ERROR, transObj.isHasError());
                        context.setVariable(ODataProducerAssertion.ODATA_BATCH_REQUEST_COUNT, transObj.getBatchCount());
                    } else if (resourcePathSplit.length > 1) {
                        //TODO:everything else after the EntitySet name, we'll just consider BadRequest or NotImplementedException???
                        throw new BadRequestException();
                    } else {
                        odataResponse = entitiesRequestResource.createEntity(httpHeaders, uriInfo, contextResolver, format, callback, entitySetName, payload);
                    }
                    try {
                        if (payload != null) {
                            payload.close();
                        }
                        if (payloadCheck != null) {
                            payloadCheck.close();
                        }
                    } catch (Exception e) {
                        logger.fine(ExceptionUtils.getMessageWithCause(e));
                    }
                } else if (httpMethod.equals("PUT")) {
                    final InputStream payload = getPayload(request);
                    odataResponse = entitiesRequestResource.functionCallPut(httpHeaders, uriInfo, contextResolver, format, callback, entitySetName, payload);
                } else if (httpMethod.equals("DELETE")) {
                    final InputStream payload = getPayload(request);
                    odataResponse = entitiesRequestResource.functionCallDelete(httpHeaders, uriInfo, contextResolver, format, callback, entitySetName, payload);
                } else {
                    throw new MethodNotAllowedException();
                }
            }// Entities related [end]
            //end of processing
        } catch (Exception e) {
            logger.fine(ExceptionUtils.getMessageWithCause(e));
            String errorMsg = ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg});
            if (batchTransactionFlag) {
                contextResolver.rollback();
            }
            if (isBatch) {
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_HAS_ERROR, true);
            }
            if (e instanceof RuntimeException) {
                String showInlineError = ExpandVariables.process(ODataProducerAssertion.ODATA_SHOW_INLINE_ERROR, variableMap, getAudit());
                odataResponse = exceptionMappingProvider.toResponse(httpHeaders, uriInfo, (RuntimeException) e, format, callback, "true".equalsIgnoreCase(showInlineError) ? true : false);
                processResponse(odataResponse, response, format, headers);
                return AssertionStatus.FALSIFIED;
            } else {
                throw new CausedIOException(e.getMessage());
            }
        } finally {
            contextResolver.close();
            if (!isBatch) {
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_NAME, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_ID, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_METHOD, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_STATUS, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_BODY, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_LAST_PAYLOAD, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_HAS_ERROR, null);
                context.setVariable(ODataProducerAssertion.ODATA_BATCH_REQUEST_COUNT, null);
            }
        }
        processResponse(odataResponse, response, format, headers);
        return AssertionStatus.NONE;
    }

    protected void processResponse(final javax.ws.rs.core.Response odataResponse, final Message response, final String format, final Map<String, String> headers) throws IOException {
        final HttpResponseKnob hrk = getHttpResponseKnob(response);
        hrk.setStatus(odataResponse.getStatus());
      final HeadersKnob headersKnob = response.getHeadersKnob();
      if (odataResponse.getMetadata().get("DataServiceVersion") != null) {
            String dataServiceVersion = (String) odataResponse.getMetadata().get("DataServiceVersion").get(0);
        headersKnob.addHeader("DataServiceVersion", dataServiceVersion, HeadersKnob.HEADER_TYPE_HTTP);
        }
        if (odataResponse.getMetadata().get("Content-Type") != null) {
            MediaType contentType = (MediaType) odataResponse.getMetadata().get("Content-Type").get(0);
          headersKnob.addHeader("Content-Type", contentType.toString(), HeadersKnob.HEADER_TYPE_HTTP);
        }
        if (headers != null && headers.size() > 0) {
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
              headersKnob.addHeader((String) pairs.getKey(), (String) pairs.getValue(), HeadersKnob.HEADER_TYPE_HTTP);
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
        response.close();
        byte[] content = "".getBytes();
        if (odataResponse.getEntity() != null) {
            content = odataResponse.getEntity().toString().getBytes();
        }
        response.initialize(getContentType(format), content);
        response.attachHttpResponseKnob(hrk);
    }

    protected InputStream getPayload(final Message request) throws IOException {
        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);
        InputStream payload;
        try {
            if (mimeKnob != null) {
                payload = mimeKnob.getEntireMessageBodyAsInputStream();
            } else {
                payload = new ByteArrayInputStream("".getBytes());
            }
        } catch (NoSuchPartException nspe) {
            throw new CausedIOException("Unable copy request as stream.", nspe);
        }
        return payload;
    }

    protected HttpResponseKnob getHttpResponseKnob(final Message response) {
        HttpResponseKnob hrk = response.getKnob(HttpResponseKnob.class);
        if (hrk == null) {
            hrk = new AbstractHttpResponseKnob() {};
        }
        return hrk;
    }

    protected HttpRequestKnob getHttpRequestKnob(final Message request) {
        HttpRequestKnob hrk = request.getKnob(HttpRequestKnob.class);
        if (hrk == null) {
            hrk = new HttpRequestKnobAdapter();
        }
        return hrk;
    }

    protected JdbcModelCache getJdbcModelCache() {
        return modelCache;
    }

    /**
     * Returns the proper content type base on the format
     */
    private ContentTypeHeader getContentType(final String format) {
        if (!isEmpty(format)) {
            if (format.contains("json")) {
                return ContentTypeHeader.APPLICATION_JSON;
            } else if (format.equals("text")) { //TODO: should only be for getEntititesCount
                return ContentTypeHeader.TEXT_DEFAULT;
            }
        }
        return ContentTypeHeader.XML_DEFAULT;
    }


    /**
     * checks if a string is empty
     */
    private boolean isEmpty(final String var) {
        if (var == null || "".equals(var)) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the parameter need further processing
     */
    private String processParam(final String parameterName, final String currentValue, final String[] queryOptions) {
        if (!isEmpty(currentValue)) {//if not empty, means there was a value provided in the context variable, return as is
            return currentValue;
        }
        for (String str : queryOptions) {
            if (str.startsWith(parameterName)) {
                String[] param = str.split("=");
                if (param.length > 1) {
                    try {
                        return URLDecoder.decode(param[1], "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        logAndAudit(AssertionMessages.EXCEPTION_INFO, e.getMessage());
                    }
                }
                break;
            }
        }
        return null;
    }

    //some pattern that jax-rs uses
    final Pattern EntityRequestResource_id_Pattern = Pattern.compile("\\([^/()]+?\\)");

}
