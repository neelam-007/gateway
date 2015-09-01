package com.l7tech.external.assertions.odata.server;

import org.odata4j.core.ODataConstants;
import org.odata4j.exceptions.ODataProducerException;
import org.odata4j.exceptions.ServerErrorException;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;
import org.odata4j.producer.ErrorResponse;
import org.odata4j.producer.resources.ExceptionMappingProvider;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.StringWriter;

/**
 * @author rraquepo, 8/26/13
 */
public class ODataExceptionMappingProvider extends ExceptionMappingProvider {
    private static ODataExceptionMappingProvider instance;

    public static ODataExceptionMappingProvider getInstance() {
        if (instance == null) {
            instance = new ODataExceptionMappingProvider();
        }
        return instance;
    }

    public Response toResponse(HttpHeaders httpHeaders, UriInfo uriInfo, RuntimeException e, String format, String callback, boolean showInlineError) {
        ODataProducerException exception;
        if (e instanceof ODataProducerException)
            exception = (ODataProducerException) e;
        else
            exception = new ServerErrorException(e);
        FormatWriter<ErrorResponse> fw = FormatWriterFactory.getFormatWriter(ErrorResponse.class, httpHeaders.getAcceptableMediaTypes(),
                format, callback);
        StringWriter sw = new StringWriter();
        fw.write(uriInfo, sw, getErrorResponse(exception, showInlineError));

        return Response.status(exception.getHttpStatus())
                .type(fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                .entity(sw.toString())
                .build();
    }
}
