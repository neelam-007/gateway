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

        try {
            List<PathSegment> odataSegments = extractPathSegments(resourcePath);
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

        return new OdataRequestInfo(uriInfo, expandExpression, selectExpression);
    }

    // TODO jwilliams: two methods, one each for json and atom? entry for atom could be evaluated after parsing for match on entity term and target uri

    public OdataPayloadInfo parsePayload(String method, OdataRequestInfo requestInfo,
                                         InputStream payload, String payloadContentType) throws OdataParsingException {
        if ("GET".equals(method) || "DELETE".equals(method)) {
            throw new OdataParsingException("Payload not supported for HTTP method '" + method + "'.");
        }

        ODataEntry entry = null;

        try {
            EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(false).build();
//            EntityProvider.readEntry(payloadContentType, requestInfo.getStartEntitySet(), payload, properties);

            switch (requestInfo.getUriType()) {
                case URI0: // TODO jwilliams: probably not needed - no payload - maybe confirm that it's empty?
                        throw new OdataParsingException("Payload not supported for this request type.");

                case URI1:
                case URI6B:
                    switch (method) {
                        case "POST":
                            entry = EntityProvider.readEntry(payloadContentType, requestInfo.getStartEntitySet(), payload, properties);
//                            return service.getEntitySetProcessor().createEntity(requestInfo, payload, payloadContentType, contentType);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                case URI2:
                    switch (method) {
                        case "PUT":
//                            return service.getEntityProcessor().updateEntity(requestInfo, payload, payloadContentType, false, contentType);
                        case "PATCH":
                        case "MERGE":
                            entry = EntityProvider.readEntry(payloadContentType, requestInfo.getStartEntitySet(), payload, properties);
//                            return service.getEntityProcessor().updateEntity(requestInfo, payload, payloadContentType, true, contentType);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                case URI3:
                    switch (method) {
                        case "PUT":
//                            return service.getEntityComplexPropertyProcessor().updateEntityComplexProperty(requestInfo, payload,
//                                    payloadContentType, false, contentType);
                        case "PATCH":
                        case "MERGE":
//                            Map<String, Object> propertyValues =
//                                    EntityProvider.readProperty(payloadContentType, requestInfo.getTargetProperty(), payload, properties);
//                            return service.getEntityComplexPropertyProcessor().updateEntityComplexProperty(requestInfo, payload,
//                                    payloadContentType, true, contentType);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                case URI4:
                case URI5:
                    switch (method) {
                        case "PUT":
                        case "PATCH":
                        case "MERGE":
                            if (requestInfo.isValueRequest()) {
//                                Object propertyValue =
                                        EntityProvider.readPropertyValue(requestInfo.getTargetProperty(), payload);
//                                return service.getEntitySimplePropertyValueProcessor().updateEntitySimplePropertyValue(requestInfo, payload,
//                                        payloadContentType, contentType);
                            } else {
//                                Map<String, Object> propertyValues =
                                        EntityProvider.readProperty(payloadContentType, requestInfo.getTargetProperty(), payload, properties);
//                                return service.getEntitySimplePropertyProcessor().updateEntitySimpleProperty(requestInfo, payload,
//                                        payloadContentType, contentType);
                            }

                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                case URI6A:
                    throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");

                case URI7A:
                    switch (method) {
                        case "PUT":
                        case "PATCH":
                        case "MERGE":
//                            String link =
                                    EntityProvider.readLink(payloadContentType, requestInfo.getStartEntitySet(), payload);
//                            return service.getEntityLinkProcessor().updateEntityLink(requestInfo, payload, payloadContentType, contentType);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                case URI7B:
                    switch (method) {
                        case "POST":
//                            String link =
                                    EntityProvider.readLink(payloadContentType, requestInfo.getStartEntitySet(), payload);
//                            return service.getEntityLinksProcessor().createEntityLink(requestInfo, payload, payloadContentType, contentType);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                case URI8:
                    throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");

                case URI9:
//                    if ("POST".equals(method)) {
//                        BatchHandler handler = new BatchHandlerImpl(serviceFactory, service);
//                        return service.getBatchProcessor().executeBatch(handler, payloadContentType, payload);
//                    } else {
//                        throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
//                    }
                    throw new OdataParsingException("Parsing of Batch Requests not supported.");

                case URI10:
                case URI11:
                case URI12:
                case URI13:
                case URI14:
                case URI15:
                case URI16:
                case URI50A:
                case URI50B:
                    throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");

                case URI17:
                    switch (method) {
                        case "PUT":
                            entry = EntityProvider.readEntry(payloadContentType, requestInfo.getStartEntitySet(), payload, properties);
//                            return service.getEntityMediaProcessor().updateEntityMedia(requestInfo, payload, payloadContentType, contentType);
                            break;
                        default:
                            throw new OdataParsingException("HTTP method " + method + " invalid for the requested resource.");
                    }

                    break;

                default:
                    throw new OdataParsingException("Unknown request type.");
            }
        } catch (EntityProviderException e) {
            throw new OdataParsingException(ExceptionUtils.getMessage(e), e);
        }

        return new OdataPayloadInfo(entry);
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
        pathInfo.setPrecedingPathSegment(Collections.<PathSegment>emptyList()); // TODO jwilliams: necessary?

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
