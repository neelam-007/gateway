package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;

/**
 * The Server side Hardcoded Response.
 */
public class ServerHardcodedResponseAssertion extends AbstractServerAssertion<HardcodedResponseAssertion> {
    private final StashManagerFactory stashManagerFactory;

    private final String message; // message if dynamically processed, else null
    private final byte[] messageBytesNoVar; // message if static, else null
    private final ContentTypeHeader contentType; // content type if static, else null
    private final int status;
    private final boolean earlyResponse;
    private final String[] variablesUsed;

    public ServerHardcodedResponseAssertion( final HardcodedResponseAssertion ass,
                                             final ApplicationContext springContext )
            throws PolicyAssertionException
    {
        super(ass);
        stashManagerFactory = springContext.getBean("stashManagerFactory", StashManagerFactory.class);

        // Cache the the content type if static
        if ( ass.getResponseContentType() == null ) {
            logger.log(Level.WARNING, "Missing content type. Falling back on text/plain");
            this.contentType = ContentTypeHeader.TEXT_DEFAULT;
        } else if ( Syntax.getReferencedNames( ass.getResponseContentType() ).length == 0 ) {
            ContentTypeHeader ctype;
            try {
                ctype = ContentTypeHeader.parseValue(ass.getResponseContentType());
            } catch (IOException e) {
                // fla bugfix, instead of breaking policy completely, log the problem (which was not done before)
                // as warning and fallback on a safe value
                logger.log(Level.WARNING, "Error parsing content type. Falling back on text/plain: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ctype = ContentTypeHeader.TEXT_DEFAULT;
            }
            this.contentType = ctype;
        } else {
            this.contentType = null; // dynamic
        }

        // If the content type is dynamic we cannot cache the response since
        // the encoding can change
        final String responseBody = ass.responseBodyString()==null ? "" : ass.responseBodyString();
        if ( this.contentType != null && Syntax.getReferencedNames( responseBody ).length == 0 ) {
            this.message = null;
            this.messageBytesNoVar = responseBody.getBytes(contentType.getEncoding());
        } else {
            this.message = responseBody;
            this.messageBytesNoVar = null;
        }

        this.status = ass.getResponseStatus();
        this.earlyResponse = ass.isEarlyResponse();
        this.variablesUsed = ass.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context )
      throws IOException, PolicyAssertionException
    {
        // Create a real stash manager, rather than making a RAM-only one, in case later assertions replace the
        // response with one that is huge (and hence needs the real hybrid stashing strategy).
        final StashManager stashManager = stashManagerFactory.createStashManager();

        final Message response = context.getResponse();
        final HttpResponseKnob hrk = getHttpResponseKnob( response );

        final Map<String,Object> variableMap;
        try{
            variableMap = context.getVariableMap(variablesUsed, getAudit());
        }catch(RuntimeException e){
            if (e.getCause() instanceof ByteLimitInputStream.DataSizeLimitExceededException){
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                            new String[] {e.getCause().getMessage()},
                            ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            }
            throw e;
        }

        final ContentTypeHeader contentType = getResponseContentType( variableMap );
        final byte[] bytes = getResponseContent( variableMap, contentType );

        // fla bugfix attach the status before closing otherwise, it's lost
        hrk.setStatus(status);
        response.close();
        response.initialize(stashManager, contentType, new ByteArrayInputStream(bytes));
        response.attachHttpResponseKnob(hrk);

        // todo: move to abstract routing assertion
        final Message request = context.getRequest();
        request.notifyMessage(response, MessageRole.RESPONSE);
        response.notifyMessage(request, MessageRole.REQUEST);
        
        context.setRoutingStatus(RoutingStatus.ROUTED);

        // process early response
        if ( earlyResponse ) {
            if (hrk instanceof HttpServletResponseKnob) {
                logAndAudit(AssertionMessages.TEMPLATE_RESPONSE_EARLY);
                final HttpServletResponseKnob hsrk = (HttpServletResponseKnob) hrk;
                @SuppressWarnings({ "deprecation" })
                final HttpServletResponse hresponse = hsrk.getHttpServletResponse();

                try {
                    hresponse.setStatus(status);
                    if ( status != HttpConstants.STATUS_NO_CONTENT ) {
                        hresponse.setContentType(contentType.getFullValue());
                        hresponse.addHeader(HttpConstants.HEADER_CONNECTION, "close");
                        final OutputStream responseos = hresponse.getOutputStream();
                        IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), responseos);
                        responseos.close();
                    }
                    hresponse.flushBuffer();
                } catch (NoSuchPartException e) {
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                            new String[] {"Unable to send hardcoded response"},
                            e);
                    return AssertionStatus.FAILED;
                }
            } else {
                logAndAudit(AssertionMessages.TEMPLATE_RESPONSE_NOT_HTTP);
                return AssertionStatus.FALSIFIED;
            }
        }

        return AssertionStatus.NONE;
    }

    private HttpResponseKnob getHttpResponseKnob( final Message response ) {
        HttpResponseKnob hrk = response.getKnob(HttpResponseKnob.class);
        if (hrk == null) {
            hrk = new AbstractHttpResponseKnob() {
                @Override
                public void addCookie( HttpCookie cookie) {
                    // This was probably not an HTTP request, so cookies are meaningless anyway.
                }
            };
        }
        return hrk;
    }

    private ContentTypeHeader getResponseContentType( final Map<String, Object> variableMap ) {
        ContentTypeHeader contentType = this.contentType;

        if ( contentType == null && assertion.getResponseContentType() != null ) {
            final String contentTypeStr = ExpandVariables.process(
                    assertion.getResponseContentType(),
                    variableMap,
                    getAudit());
            try {
                contentType = ContentTypeHeader.parseValue( contentTypeStr );
            } catch ( IOException e ) {
                logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] { "Invalid content type, using text/plain : " + ExceptionUtils.getMessage(e) },
                        ExceptionUtils.getDebugException( e ));
            }
        }

        if ( contentType == null ) {
            contentType = ContentTypeHeader.TEXT_DEFAULT;
        }

        return contentType;
    }

    private byte[] getResponseContent( final Map<String, Object> variableMap,
                                       final ContentTypeHeader contentType ) {
        final byte[] bytes;
        if ( message != null ) {
            String msg = message;
            msg = ExpandVariables.process(msg, variableMap, getAudit());
            bytes = msg.getBytes(contentType.getEncoding());
        } else {
            bytes = this.messageBytesNoVar;
        }
        return bytes;
    }
}
