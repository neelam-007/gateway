package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.uri.*;
import org.apache.olingo.odata2.core.ODataPathSegmentImpl;
import org.apache.olingo.odata2.core.PathInfoImpl;
import org.apache.olingo.odata2.core.uri.UriInfoImpl;
import org.apache.olingo.odata2.core.uri.UriParserImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses OData request URIs and payloads into formats easily analyzed for threats or forbidden operations.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class OdataParser {
    private final UriParser uriParser;

    public OdataParser(Edm entityDataModel) {
        uriParser = new UriParserImpl(entityDataModel);
    }

    public OdataRequestInfo parseRequest(String resourcePath, String queryString) throws OdataParsingException {
        if (resourcePath.contains(";")) {
            throw new OdataParsingException("Could not parse matrix parameters in resource path.");
        }

        UriInfoImpl uriInfo;
        Map<String, String> queryParameters;

        List<PathSegment> odataSegments = extractPathSegments(resourcePath);

        try {
            queryParameters = extractQueryParameters(queryString);

            uriInfo = (UriInfoImpl) uriParser.parse(odataSegments, queryParameters);
        } catch (UriSyntaxException | EdmException | UriNotMatchingException | IOException e) {
            throw new OdataParsingException(ExceptionUtils.getMessage(e), e);
        }

        boolean foundExpand = false;
        boolean foundSelect = false;

        String expandExpression = null;
        String selectExpression = null;

        for (String parameter : queryParameters.keySet()) {
            switch (parameter) {
                case "$expand":
                    expandExpression = queryParameters.get(parameter);
                    foundExpand = true;
                    break;
                case "$select":
                    selectExpression = queryParameters.get(parameter);
                    foundSelect = true;
                    break;
                default:
                    continue;
            }

            if (foundExpand && foundSelect) {
                break;
            }
        }

        return new OdataRequestInfo(uriInfo, expandExpression, selectExpression, odataSegments);
    }

    /**
     * Parse the payload, validating against its related request information.
     *
     * N.B. Batch requests are unsupported and will cause an exception
     *
     * @param method the HTTP method of the request
     * @param requestInfo the information about the request the payload came with
     * @param payload the payload of the request, to be validated
     * @param payloadContentType the content type of the payload
     * @return OdataPayloadInfo describing the payload, or null if the payload is empty
     * @throws OdataParsingException if the payload could not be read, is malformed, or does not match the request
     * details; if the method is not supported for the given request type; or if parsing fails for any other reason
     */
    public OdataPayloadInfo parsePayload(String method, OdataRequestInfo requestInfo,
                                         InputStream payload, String payloadContentType) throws OdataParsingException {
        // payloads are not expected for GET or DELETE operations
        if ("GET".equals(method) || "DELETE".equals(method)) {
            try {
                if (-1 != payload.read()) {
                    throw new OdataParsingException("Payload not supported for HTTP method '" + method + "'.");
                }
            } catch (IOException e) {
                throw new OdataParsingException("Payload could not be read: " + ExceptionUtils.getMessage(e), e);
            }

            return null;
        }

        boolean media = false;
        ODataEntry entry = null;
        List<String> links = new ArrayList<>();
        Object propertyValue = null;
        Map<String, Object> properties = new HashMap<>();

        try {
            EntityProviderReadProperties readProperties =
                    EntityProviderReadProperties.init().mergeSemantic(false).build();

            switch (requestInfo.getUriType()) {
                // operations that don't take payloads, and only allow GET requests
                case URI0:      // service document
                case URI6A:     // navigation property with multiplicity '1' or '0..1'
                case URI8:      // $metadata
                case URI15:     // count of entity set
                case URI16:     // count single entity
                case URI50A:    // count of link to single entity
                case URI50B:    // count of links to multiple entities
                    throw new OdataParsingException("HTTP method '" + method + "' invalid for the requested resource.");

                // service operations - only take GET and POST requests, but payload can't be validated
                case URI10:     // function import returning single entity
                case URI11:     // function import returning collection of complex type
                case URI12:     // function import returning single complex property
                case URI13:     // function import returning collection of primitives
                case URI14:     // function import returning single primitive property
                    if ("POST".equals(method)) {
                        return null;  // we don't know what the payload might be, so we can't parse it
                    }

                    throw new OdataParsingException("HTTP method '" + method + "' invalid for the requested resource.");

                // batch operation
                case URI9:
                    throw new OdataParsingException("Parsing of Batch Requests not supported.");

                // create an entity
                case URI1:
                case URI6B:
                    switch (method) {
                        case "POST":
                            if (requestInfo.getTargetEntitySet().getEntityType().hasStream()) {  // creating a media resource
                                // no need to use EntityProvider.readBinary() - it doesn't do any validation
                                media = true;
                            } else {  // creating regular entry
                                entry = EntityProvider.readEntry(payloadContentType,
                                        requestInfo.getTargetEntitySet(), payload, readProperties);
                            }
                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                // update an entity
                case URI2:
                    switch (method) {
                        case "PUT":
                        case "PATCH":
                        case "MERGE":
                            entry = EntityProvider.readEntry(payloadContentType,
                                    requestInfo.getTargetEntitySet(), payload, readProperties);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                // update complex property
                case URI3:
                    switch (method) {
                        case "PUT":
                        case "PATCH":
                        case "MERGE":
                            properties = EntityProvider.readProperty(payloadContentType,
                                    requestInfo.getTargetProperty(), payload, readProperties);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                // update simple property
                case URI4:
                case URI5:
                    switch (method) {
                        case "PUT":
                        case "PATCH":
                        case "MERGE":
                            if (requestInfo.isValueRequest()) {
                                propertyValue = EntityProvider.readPropertyValue(requestInfo.getTargetProperty(),
                                        payload);
                            } else {
                                properties = EntityProvider.readProperty(payloadContentType,
                                        requestInfo.getTargetProperty(), payload, readProperties);
                            }

                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                // update link to a single entity
                case URI7A:
                    switch (method) {
                        case "PUT":
                        case "PATCH":
                        case "MERGE":
                            links.add(EntityProvider.readLink(payloadContentType,
                                    requestInfo.getTargetEntitySet(), payload));
                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                // update link to multiple entities
                case URI7B:
                    switch (method) {
                        case "POST":
                            links.addAll(EntityProvider.readLinks(payloadContentType,
                                    requestInfo.getTargetEntitySet(), payload));
                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                // update entity media resource
                case URI17:
                    switch (method) {
                        case "PUT":
                            // no need to use EntityProvider.readBinary() - it doesn't do any validation
                            media = true;
                            break;
                        default:
                            throw new OdataParsingException("HTTP method '" + method +
                                    "' invalid for the requested resource.");
                    }

                    break;

                default:
                    throw new OdataParsingException("Unrecognized request type.");
            }
        } catch (EntityProviderException | EdmException e) {
            throw new OdataParsingException(ExceptionUtils.getMessage(e), e);
        }

        return new OdataPayloadInfo(entry, links, propertyValue, properties, media);
    }

    protected static List<PathSegment> extractPathSegments(String resourcePath) {
        String pathInfoString = resourcePath;

        while (pathInfoString.startsWith("/")) {
            pathInfoString = pathInfoString.substring(1);
        }

        List<String> pathSegments = Arrays.asList(pathInfoString.split("/"));

        List<PathSegment> odataSegments = new ArrayList<>();

        for (final String segment : pathSegments) {
            odataSegments.add(new ODataPathSegmentImpl(segment, null));
        }

        PathInfoImpl pathInfo = new PathInfoImpl();
        pathInfo.setODataPathSegment(odataSegments);
        pathInfo.setPrecedingPathSegment(Collections.<PathSegment>emptyList());

        return odataSegments;
    }

    protected static Map<String, String> extractQueryParameters(final String queryString) throws IOException {
        Map<String, String> queryParametersMap = new HashMap<>();

        if (queryString != null && !queryString.isEmpty()) {
            // decode the query string and split it on ampersands
            List<String> queryParameters = Arrays.asList(HexUtils.urlDecode(queryString).split("\\u0026"));

            for (String param : queryParameters) {
                int indexOfEqualsSign = param.indexOf("=");

                if (indexOfEqualsSign < 0) {
                    queryParametersMap.put(param, "");
                } else {
                    queryParametersMap.put(param.substring(0, indexOfEqualsSign),
                            param.substring(indexOfEqualsSign + 1));
                }
            }
        }

        return queryParametersMap;
    }

    public class OdataParsingException extends Exception {
        public OdataParsingException(String message) {
            super(message);
        }

        public OdataParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
