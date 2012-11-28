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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.BeanFactory;

import javax.net.SocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;

/**
 * Server side implementation of the SimpleRawTransportAssertion.
 * <p/>
 * Initial version is one-way only.
 *
 * @see com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion
 */
public class ServerSimpleRawTransportAssertion extends AbstractServerAssertion<SimpleRawTransportAssertion> {

    //Define the failed reason code.
    public static final int SUCCESS = 0;
    public static final int HOST_NOT_FOUND = -1; // The Host can not be reached
    public static final int CONNECTION_REFUSE = -2;  // the port is incorrect
    public static final int SOCKET_TIMEOUT = -3;  // this will indicate connecting to a host timed out.  Thus, no ACK was ever received for the first SYN
    public static final int DATA_SIZE_LIMIT_EXCEED = -4;
    public static final int UNDEFINED = -5;

    private final StashManagerFactory stashManagerFactory;
    private final ContentTypeHeader responseContentType;
    private final String responseContentTypeTemplate;
    private final String[] referencedVariables;
    SocketFactory socketFactory = SocketFactory.getDefault();

    public ServerSimpleRawTransportAssertion(SimpleRawTransportAssertion assertion, BeanFactory beanFactory)
            throws PolicyAssertionException, IOException
    {
        super(assertion);
        this.stashManagerFactory = beanFactory == null ? new ByteArrayStashManagerFactory() : beanFactory.getBean("stashManagerFactory", StashManagerFactory.class);
        this.referencedVariables = assertion.getVariablesUsed();

        final String responseContentType = assertion.getResponseContentType();
        boolean contentTypeVaries = Syntax.getReferencedNames(responseContentType).length > 0;
        if (contentTypeVaries) {
            this.responseContentType = null;
            this.responseContentTypeTemplate = responseContentType;
        } else {
            this.responseContentType = ContentTypeHeader.create(responseContentType);
            this.responseContentTypeTemplate = null;
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            context.setVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE, UNDEFINED);
            Message request = assertion.getRequestTarget() == null ? null : context.getTargetMessage(assertion.getRequestTarget(), true);
            Message response = assertion.getResponseTarget() == null ? null : context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);

            Map<String,?> vars = context.getVariableMap(referencedVariables, getAudit());

            context.setRoutingStatus(RoutingStatus.ATTEMPTED);
            transmitOverTcp(context, request, response, vars);
            context.setRoutingStatus(RoutingStatus.ROUTED);
            context.setVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE, SUCCESS);

            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.NO_SUCH_VARIABLE, e.getVariable() );
            return AssertionStatus.SERVER_ERROR;
        } catch (UnknownHostException e) {
            context.setVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE, HOST_NOT_FOUND);
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch (ConnectException e) {
            context.setVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE, CONNECTION_REFUSE);
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch (SocketTimeoutException e) {
            context.setVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE, SOCKET_TIMEOUT);
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch (ByteLimitInputStream.DataSizeLimitExceededException e) {
            context.setVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE, DATA_SIZE_LIMIT_EXCEED);
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch (IOException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, e );
            return AssertionStatus.FAILED;
        } catch (VariableNameSyntaxException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.SERVER_ERROR;
        }  catch (NumberFormatException e){
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Raw transport failed. Invalid response size limit. " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private void transmitOverTcp(PolicyEnforcementContext context, Message request, Message response, Map<String, ?> vars) throws IOException, NoSuchPartException, NumberFormatException {
        Socket sock = null;
        OutputStream outputStream;
        InputStream inputStream = null;
        try {
            final String targetHost = ExpandVariables.process(assertion.getTargetHost(), vars, getAudit(), true);
            final String targetPort = ExpandVariables.process(assertion.getTargetPort(), vars, getAudit(), true);
            final long maxResponseSize = assertion.getMaxResponseBytesText()== null ?
                    Message.getMaxBytes() :
                    Long.parseLong(ExpandVariables.process(assertion.getMaxResponseBytesText(), vars, getAudit(), true));
            sock = socketFactory.createSocket(InetAddress.getByName(targetHost), Integer.parseInt(targetPort));
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
                        : ContentTypeHeader.create(ExpandVariables.process(responseContentTypeTemplate, vars, getAudit(), true));

                response.initialize(stashManagerFactory.createStashManager(), contentType, new ByteLimitInputStream(new BufferedInputStream(sock.getInputStream()), 1024,maxResponseSize),maxResponseSize);
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
