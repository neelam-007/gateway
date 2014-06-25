package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.uri.*;
import org.apache.olingo.odata2.core.ODataPathSegmentImpl;
import org.apache.olingo.odata2.core.PathInfoImpl;
import org.apache.olingo.odata2.core.uri.UriInfoImpl;
import org.apache.olingo.odata2.core.uri.UriParserImpl;
import org.apache.olingo.odata2.core.uri.UriType;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses OData request URIs and payloads into formats easily analyzed for threats or forbidden operations.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class OdataParser {
    private final Edm entityDataModel;
    private final UriParser uriParser;

    public OdataParser(Edm entityDataModel) {
        this.entityDataModel = entityDataModel;

        uriParser = new UriParserImpl(this.entityDataModel);
    }

    public OdataRequestInfo parseRequest(String resourcePath, String queryString) throws OdataParsingException {
        if (resourcePath.contains(";")) {
            // TODO jwilliams: write good message for matrix parameters in odata segment, see if anything in specs
            throw new OdataParsingException("Could not parse resource path");
        }

        UriInfoImpl uriInfo;

        try {
            List<PathSegment> odataSegments = extractPathSegments(resourcePath);
            Map<String, String> queryParameters = extractQueryParameters(queryString);

            uriInfo = (UriInfoImpl) uriParser.parse(odataSegments, queryParameters);
        } catch (UriSyntaxException | EdmException | UriNotMatchingException | IOException e) {
            throw new OdataParsingException(ExceptionUtils.getMessage(e), e);
        }

        return new OdataRequestInfo(uriInfo);
    }

    public OdataPayloadInfo parsePayload(InputStream requestBody) {
        return new OdataPayloadInfo();
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

        if (queryString != null) {
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

    public class OdataRequestInfo {
        private final UriInfoImpl uriInfo;

        public OdataRequestInfo(UriInfoImpl uriInfo) {
            this.uriInfo = uriInfo;
        }

        public boolean isServiceDocumentRequest() {
            return UriType.URI0 == uriInfo.getUriType();
        }

        public boolean isMetadataRequest() {
            return UriType.URI8 == uriInfo.getUriType();
        }

        public boolean isValueRequest() {
            return uriInfo.isValue();
        }
    }

    public class OdataPayloadInfo {
        public String containsOpenTypeEntity() {
            return null;
        }

        public String getOperation() { // TODO jwilliams: necessary?
            return null;
        }
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
