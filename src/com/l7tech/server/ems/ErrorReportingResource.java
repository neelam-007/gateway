package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Resource that reports resource exceptions using an HTML error message.
 * Currently only wraps POSTs.
 */
public class ErrorReportingResource extends Resource {
    private static final Logger logger = Logger.getLogger(ErrorReportingResource.class.getName());

    public ErrorReportingResource() {
    }

    public ErrorReportingResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public void handlePost() {
        try {
            acceptRepresentation(getRequest().getEntity());
        } catch (ResourceException re) {
            logger.log(Level.INFO, "ResourceException while handling POST request: " + ExceptionUtils.getMessage(re), re);
            getResponse().setStatus(re.getStatus());
            if (!getResponse().isEntityAvailable())
                respondWithErrorMessage(re);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception while handing POST request: " + ExceptionUtils.getMessage(e), e);
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            if (!getResponse().isEntityAvailable())
                respondWithErrorMessage(e);
        }
    }

    private void respondWithErrorMessage(Throwable re) {
        // Unwrap ResourceExceptions that don't contain any additional info
        while (re.getClass() == ResourceException.class && re.getCause() != null)
            re = re.getCause();

        // TODO something nicer than this
        getResponse().setEntity(new StringRepresentation("Error: " + ExceptionUtils.getMessage(re)));
    }
}
