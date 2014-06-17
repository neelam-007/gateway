package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.common.io.DuplicateAliasException;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.StaleUpdateException;
import com.l7tech.policy.PolicyDeletionForbiddenException;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.glassfish.jersey.server.ParamException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.xml.sax.SAXException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
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
        logger.log(Level.FINEST,"",e);
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
        } else if ( e instanceof PermissionDeniedException || e instanceof InsufficientPermissionsException ) {
            String userId = JaasUtils.getCurrentUser()==null ? "<unauthenticated>" : JaasUtils.getCurrentUser().getLogin();
            logMessage = ExceptionUtils.getMessage(e) + ", for user '"+userId+"'.";
            status = Response.Status.UNAUTHORIZED;
        } else if ( e instanceof ResourceFactory.DuplicateResourceAccessException) {
            status = Response.Status.FORBIDDEN;
        } else if ( e instanceof ResourceFactory.ResourceAccessException ) {
            if ( ExceptionUtils.causedBy(e, DuplicateObjectException.class) ) {
                final DuplicateObjectException cause = ExceptionUtils.getCauseIfCausedBy( e, DuplicateObjectException.class );
                errorResponse.setType(cause.getClass().getSimpleName().replace("Exception",""));
                status = Response.Status.BAD_REQUEST;
            } else if ( ExceptionUtils.causedBy( e, StaleUpdateException.class) ) {
                final StaleUpdateException cause = ExceptionUtils.getCauseIfCausedBy( e, StaleUpdateException.class );
                errorResponse.setType(cause.getClass().getSimpleName().replace("Exception",""));
                status = Response.Status.FORBIDDEN;
            } else if ( ExceptionUtils.causedBy( e, PolicyDeletionForbiddenException.class) ) {
                final PolicyDeletionForbiddenException cause = ExceptionUtils.getCauseIfCausedBy( e, PolicyDeletionForbiddenException.class );
                errorResponse.setType(cause.getClass().getSimpleName().replace("Exception",""));
                status = Response.Status.FORBIDDEN;
            } else if ( ExceptionUtils.causedBy( e, DataIntegrityViolationException.class) ) {
                final DataIntegrityViolationException cause = ExceptionUtils.getCauseIfCausedBy( e, DataIntegrityViolationException.class );
                if(cause.getCause() instanceof ConstraintViolationException){
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause.getCause();
                    errorResponse.setType("ConstraintViolation");
                    errorResponse.setDetail(ExceptionUtils.getMessage(constraintViolationException.getSQLException(), constraintViolationException.getMessage()));
                    status = Response.Status.BAD_REQUEST;
                } else {
                    errorResponse.setType(cause.getClass().getSimpleName().replace("Exception", ""));
                    logger.log(Level.INFO, "Resource operation forbidden, '" + ExceptionUtils.getMessage(cause) + "'", ExceptionUtils.getDebugException(e));
                    status = Response.Status.FORBIDDEN;
                }
            } else if( ExceptionUtils.causedBy(e, SAXException.class))  {
                final SAXException cause = ExceptionUtils.getCauseIfCausedBy( e, SAXException.class );
                logger.log( Level.WARNING, ExceptionUtils.getMessage(cause), ExceptionUtils.getDebugException(e) );
                status = Response.Status.BAD_REQUEST;
            } else if( ExceptionUtils.causedBy(e, IllegalArgumentException.class))  {
                final IllegalArgumentException cause = ExceptionUtils.getCauseIfCausedBy( e, IllegalArgumentException.class );
                logger.log( Level.WARNING, ExceptionUtils.getMessage(cause), ExceptionUtils.getDebugException(e) );
                errorResponse.setType("InvalidResource");
                errorResponse.setDetail(ExceptionUtils.getMessageWithCause(cause));
                status = Response.Status.BAD_REQUEST;
            } else if( ExceptionUtils.causedBy(e, DuplicateAliasException.class))  {
                final DuplicateAliasException cause = ExceptionUtils.getCauseIfCausedBy( e, DuplicateAliasException.class );
                logger.log( Level.WARNING, ExceptionUtils.getMessage(cause), ExceptionUtils.getDebugException(e) );
                errorResponse.setType("InvalidResource");
                errorResponse.setDetail(ExceptionUtils.getMessageWithCause(cause));
                status = Response.Status.BAD_REQUEST;
            } else{
                logger.log( Level.WARNING, "Resource access error processing management request: "+ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
                status = Response.Status.FORBIDDEN;
            }
        } else if (e instanceof InvalidArgumentException) {
            logger.log(Level.WARNING, "InvalidArgumentException error processing management request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            final InvalidArgumentException ex = (InvalidArgumentException) e;
            errorResponse.setDetail("Invalid value for argument" + (ex.getArgumentName() != null ? " '" + ex.getArgumentName() + "'" : "") + ". " + ex.getMessage());
            status = Response.Status.BAD_REQUEST;
        } else if(e instanceof IllegalArgumentException)  {
            logger.log( Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            errorResponse.setDetail(ExceptionUtils.getMessageWithCause(e));
            status = Response.Status.BAD_REQUEST;
        } else if (e instanceof ParamException.QueryParamException) {
            logger.log(Level.WARNING, "QueryParamException error processing management request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            final ParamException.QueryParamException ex = (ParamException.QueryParamException) e;
            errorResponse.setDetail("Invalid value for query parameter '" + ex.getParameterName() + "'. " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
            errorResponse.setType("InvalidQueryParam");
            status = Response.Status.BAD_REQUEST;
        } else if ( e instanceof WebApplicationException) {
            logger.log( Level.WARNING, "Resource access error processing management request: "+ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            final WebApplicationException ex = (WebApplicationException)e;
            status = ex.getResponse().getStatusInfo();
        } else if ( e instanceof ObjectNotFoundException) {
            logger.log( Level.WARNING, "Resource not found: "+ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            status = Response.Status.NOT_FOUND;
        } else {
            logger.log( Level.WARNING, "Error processing management request: "+ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e) );
        }
        logger.log( Level.INFO,logMessage,ExceptionUtils.getDebugException(e));
        return new Pair<Response.StatusType,ErrorResponse>(status,errorResponse);
    }
}
