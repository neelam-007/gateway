package com.l7tech.external.assertions.odata.server;

import java.io.StringWriter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.OError;
import org.odata4j.core.OErrors;
import org.odata4j.exceptions.ODataProducerException;
import org.odata4j.exceptions.ServerErrorException;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;
import org.odata4j.producer.ErrorResponse;
import org.odata4j.producer.Responses;
import org.odata4j.producer.resources.ExceptionMappingProvider;

/**
 * @author rraquepo, 8/26/13
 */
public class ODataExceptionMappingProvider extends ExceptionMappingProvider {
  public static final String STACK_TRACE_START_ELEMENT = "<stacktrace>";
  public static final String STACK_TRACE_END_ELEMENT = "</stacktrace>";
  public static final String REPLACE_ME_INNER_ERROR = "REPLACE_ME_INNER_ERROR";

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
    FormatWriter<ErrorResponse> fw = FormatWriterFactory.getFormatWriter(ErrorResponse.class, httpHeaders.getAcceptableMediaTypes(), format, callback);
    StringWriter sw = new StringWriter();
    fw.write(uriInfo, sw, getCustomizedErrorResponse(exception, showInlineError));

    return Response.status(exception.getHttpStatus()).type(fw.getContentType()).header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER).entity(sw.toString()).build();
  }

  private ErrorResponse getCustomizedErrorResponse(ODataProducerException exception, boolean includeInnerError) {
    if (!includeInnerError) {
      return super.getErrorResponse(exception, includeInnerError);
    }
    OError error = exception.getOError();
    error = OErrors.error(error.getCode(), error.getMessage(), REPLACE_ME_INNER_ERROR);
    return Responses.error(error);
  }
}
