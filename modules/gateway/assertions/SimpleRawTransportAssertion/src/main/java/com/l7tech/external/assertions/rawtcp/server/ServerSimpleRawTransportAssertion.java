package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;

import javax.net.SocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Server side implementation of the SimpleRawTransportAssertion.
 * <p/>
 * Initial version is one-way only.
 *
 * @see com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion
 */
public class ServerSimpleRawTransportAssertion extends AbstractServerAssertion<SimpleRawTransportAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSimpleRawTransportAssertion.class.getName());

    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;
    private final ContentTypeHeader responseContentType;
    private final String responseContentTypeTemplate;
    private final String[] referencedVariables;
    SocketFactory socketFactory = SocketFactory.getDefault();

    public ServerSimpleRawTransportAssertion(SimpleRawTransportAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub)
            throws PolicyAssertionException, IOException
    {
        super(assertion);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
        this.stashManagerFactory = beanFactory == null ? new ByteArrayStashManagerFactory() : beanFactory.getBean("stashManagerFactory", StashManagerFactory.class);
        this.referencedVariables = assertion.getVariablesUsed();

        final String responseContentType = assertion.getResponseContentType();
        boolean contentTypeVaries = Syntax.getReferencedNames(responseContentType).length > 0;
        if (contentTypeVaries) {
            this.responseContentType = null;
            this.responseContentTypeTemplate = responseContentType;
        } else {
            this.responseContentType = ContentTypeHeader.parseValue(responseContentType);
            this.responseContentTypeTemplate = null;
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            Message request = assertion.getRequestTarget() == null ? null : context.getTargetMessage(assertion.getRequestTarget(), true);
            Message response = assertion.getResponseTarget() == null ? null : context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);

            Map<String,?> vars = context.getVariableMap(referencedVariables, auditor);

            context.setRoutingStatus(RoutingStatus.ATTEMPTED);
            transmitOverTcp(context, request, response, vars);
            context.setRoutingStatus(RoutingStatus.ROUTED);

            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Raw transport failed: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Raw transport failed: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        } catch (VariableNameSyntaxException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Raw transport failed: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private void transmitOverTcp(PolicyEnforcementContext context, Message request, Message response, Map<String, ?> vars) throws IOException, NoSuchPartException {
        Socket sock = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            String targetHost = ExpandVariables.process(assertion.getTargetHost(), vars, auditor, true);
            sock = socketFactory.createSocket(InetAddress.getByName(targetHost), assertion.getTargetPort());
            sock.setSoTimeout(assertion.getWriteTimeoutMillis());
            inputStream = request == null
                    ? new EmptyInputStream()
                    : request.getMimeKnob().getEntireMessageBodyAsInputStream();
            outputStream = sock.getOutputStream();
            IOUtils.copyStream(inputStream, outputStream);
            outputStream.flush();
            sock.shutdownOutput();

            if (response != null) {
                sock.setSoTimeout(assertion.getReadTimeoutMillis());
                ContentTypeHeader contentType = responseContentType != null
                        ? responseContentType
                        : ContentTypeHeader.parseValue(ExpandVariables.process(responseContentTypeTemplate, vars, auditor, true));
                response.initialize(stashManagerFactory.createStashManager(), contentType, new ByteLimitInputStream(new BufferedInputStream(sock.getInputStream()), 1024, assertion.getMaxResponseBytes()));
                final Socket finalSock = sock;
                sock = null; // defer closing response socket until end of request, so we might be able to stream it
                context.runOnClose(new Runnable() {
                    @Override
                    public void run() {
                        ResourceUtils.closeQuietly(finalSock);
                    }
                });
                // Force response first part to get read and stashed before socket gets closed
                response.getMimeKnob().getEntireMessageBodyAsInputStream().close();
            }

        } finally {
            ResourceUtils.closeQuietly(inputStream);
            ResourceUtils.closeQuietly(sock);
        }
    }
}
