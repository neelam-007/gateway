package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.rest.RestResponse;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.StaleUpdateException;
import com.l7tech.policy.PolicyDeletionForbiddenException;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jboss.cache.marshall.MarshallingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.UnmarshalException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This handles Exception's
 *
 */
@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {
    private static final Logger logger = Logger.getLogger(ExceptionMapper.class.getName());

    @Context
    protected UriInfo uriInfo;

    @Context
    Request request;

    @Override
    public Response toResponse(Exception exception) {
        Pair<Response.StatusType,ErrorResponse> msg = handleOperationException(exception);
        return Response.status(msg.getKey()).
                entity(msg.getValue()).
                build();
    }

    private Pair<Response.StatusType,ErrorResponse> handleOperationException( final Throwable e ) {
        Response.StatusType status = Response.Status.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = ManagedObjectFactory.createErrorResponse();
        errorResponse.setDetail(ExceptionUtils.getMessageWithCause(e));
        errorResponse.setLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()));
        errorResponse.setType(e.getClass().getSimpleName().replace("Exception",""));

        String logMessage = "Error processing management request:" + ExceptionUtils.getMessage(e);
        if ( e instanceof ResourceFactory.InvalidResourceSelectors ) {
            status = Response.Status.BAD_REQUEST;
        } else if ( e instanceof ResourceFactory.ResourceNotFoundException ) {
            status = Response.Status.NOT_FOUND;
        } else if ( e instanceof ResourceFactory.InvalidResourceException ) {
            status = Response.Status.BAD_REQUEST;
        } else if ( e instanceof PermissionDeniedException ) {
            String userId = JaasUtils.getCurrentUser()==null ? "<unauthenticated>" : JaasUtils.getCurrentUser().getLogin();
            logMessage = ExceptionUtils.getMessage(e) + ", for user '"+userId+"'.";
            status = Response.Status.UNAUTHORIZED;
        } else if ( e instanceof ResourceFactory.DuplicateResourceAccessException) {
            status = Response.Status.FORBIDDEN;
        } else if ( e instanceof ResourceFactory.ResourceAccessException ) {
            if ( ExceptionUtils.causedBy(e, DuplicateObjectException.class) ) {
                final DuplicateObjectException cause = ExceptionUtils.getCauseIfCausedBy( e, DuplicateObjectException.class );
                errorResponse.setType(cause.getClass().getCanonicalName().replace("Exception",""));
                status = Response.Status.FORBIDDEN;
            } else if ( ExceptionUtils.causedBy( e, StaleUpdateException.class) ) {
                final StaleUpdateException cause = ExceptionUtils.getCauseIfCausedBy( e, StaleUpdateException.class );
                errorResponse.setType(cause.getClass().getCanonicalName().replace("Exception",""));
                status = Response.Status.FORBIDDEN;
            } else if ( ExceptionUtils.causedBy( e, PolicyDeletionForbiddenException.class) ) {
                final PolicyDeletionForbiddenException cause = ExceptionUtils.getCauseIfCausedBy( e, PolicyDeletionForbiddenException.class );
                errorResponse.setType(cause.getClass().getCanonicalName().replace("Exception",""));
                status = Response.Status.FORBIDDEN;
            } else if ( ExceptionUtils.causedBy( e, DataIntegrityViolationException.class) ) {
                final DataIntegrityViolationException cause = ExceptionUtils.getCauseIfCausedBy( e, DataIntegrityViolationException.class );
                errorResponse.setType(cause.getClass().getCanonicalName().replace("Exception",""));
                logger.log( Level.INFO, "Resource deletion forbidden (in use), '"+ExceptionUtils.getMessage(cause)+"'", ExceptionUtils.getDebugException(e) );
                status = Response.Status.FORBIDDEN;
            } else if( ExceptionUtils.causedBy(e, SAXException.class))  {
                final SAXException cause = ExceptionUtils.getCauseIfCausedBy( e, SAXException.class );
                logger.log( Level.WARNING, ExceptionUtils.getMessage(cause), ExceptionUtils.getDebugException(e) );
                status = Response.Status.BAD_REQUEST;
            } else{
            logger.log( Level.WARNING, "Resource access error processing management request: "+ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            status = Response.Status.FORBIDDEN;
            }
        } else if ( e instanceof WebApplicationException) {
            logger.log( Level.WARNING, "Resource access error processing management request: "+ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            final WebApplicationException ex = (WebApplicationException)e;
            status = ex.getResponse().getStatusInfo();
        } else {
            logger.log( Level.WARNING, "Error processing management request: "+ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e) );
        }
        logger.log( Level.INFO,logMessage,ExceptionUtils.getDebugException(e));
        return new Pair<Response.StatusType,ErrorResponse>(status,errorResponse);
    }
}
