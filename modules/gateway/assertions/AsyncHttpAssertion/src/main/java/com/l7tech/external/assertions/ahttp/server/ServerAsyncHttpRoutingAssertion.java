package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.ahttp.AsyncHttpRoutingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.OutboundHeadersKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.AuditLogFormatter;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.assertion.AbstractServerHttpRoutingAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import static com.l7tech.util.Option.optional;

/**
 *
 */
public class ServerAsyncHttpRoutingAssertion extends AbstractServerHttpRoutingAssertion<AsyncHttpRoutingAssertion> {
    private final String[] varsUsed;

    @Inject
    private PolicyCache policyCache;

    @Inject
    private AuditContextFactory auditContextFactory;

    @Inject
    private MessageSummaryAuditFactory messageSummaryAuditFactory;

    @Inject
    private StashManagerFactory stashManagerFactory;


    public ServerAsyncHttpRoutingAssertion(final AsyncHttpRoutingAssertion assertion, final ApplicationContext applicationContext) throws ServerPolicyException {
        super(assertion, applicationContext);
        varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        ResponseContextInfo responseContextInfo = new ResponseContextInfo();
        try {
            String guid = assertion.getPolicyGuid();
            responseContextInfo.serverPolicyHandle = guid == null ? null : policyCache.getServerPolicy(guid);
            if (responseContextInfo.serverPolicyHandle == null) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to perform asynchronous routing -- response policy GUID " + assertion.getPolicyGuid() + " could not be found");
                return AssertionStatus.SERVER_ERROR;
            }

            String[] varsToCopy = responseContextInfo.serverPolicyHandle.getPolicyMetadata().getVariablesUsed();

            final Map<String, Object> varsMap = context.getVariableMap(varsUsed, getAudit());
            String urlStr = ExpandVariables.process(assertion.getProtectedServiceUrl(), varsMap, getAudit());

            final Map<String, Object> varsToCopyMap = context.getVariableMap(varsToCopy, getAudit());
            responseContextInfo.clonedContext = copyContext(context, varsToCopyMap);

            URI uri = parseUri(urlStr);
            Message targetMessage = context.getRequest(); // TODO configurable target message to route
            HttpRequest httpRequest = createNettyHttpRequest(uri, targetMessage);
            int port = uri.getPort();
            if (-1 == port)
                port = 80; // TODO use 443 as default for https when supported

            final ResponseContextInfo finalResponseContextInfo = responseContextInfo;
            NettyHttpClient.issueAsyncHttpRequest(uri.getHost(), port, httpRequest, new Functions.UnaryVoid<Either<IOException, HttpResponse>>() {
                @Override
                public void call(Either<IOException, HttpResponse> ioExceptionHttpResponseEither) {
                    try {
                        invokeResponsePolicy(finalResponseContextInfo, ioExceptionHttpResponseEither);
                    } finally {
                        ResourceUtils.closeQuietly(finalResponseContextInfo);
                    }
                }
            });

            // We are no longer responsible for closing the server policy handle after handoff to async callback
            responseContextInfo = null;
            return AssertionStatus.NONE;

        } catch ( BeansException be ) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to access policy cache for asynchronous HTTP routing: " + ExceptionUtils.getMessage(be)}, ExceptionUtils.getDebugException(be));
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to initiate asynchronous HTTP routing: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to read request message parts for asynchronous HTTP routing: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } finally {
            ResourceUtils.closeQuietly(responseContextInfo);
        }
    }

    private URI parseUri(String urlStr) {
        try {
            URI uri = new URI(urlStr);

            String scheme = optional(uri.getScheme()).orSome("http");

            // TODO https
            if (!"http".equalsIgnoreCase(scheme)) {
                final String msg = "Only HTTP URIs are supported for async routing currently: scheme=" + scheme;
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{msg}, null);
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, msg);
            }

            return uri;
        } catch (URISyntaxException e) {
            final String msg = "Unable to produce URI for asynchronous HTTP routing: " + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, msg, e);
        }
    }

    private HttpRequest createNettyHttpRequest(URI uri, Message targetMessage) throws NoSuchPartException, IOException {
        HttpVersion httpVersion;
        GenericHttpRequestParams.HttpVersion ghv = assertion.getHttpVersion();
        switch (ghv) {
            case HTTP_VERSION_1_0:
                httpVersion = HttpVersion.HTTP_1_0;
                break;

            case HTTP_VERSION_1_1:
            default:
                httpVersion = HttpVersion.HTTP_1_1;
        }
        HttpMethod httpMethod = assertion.getHttpMethod() == null ? HttpMethod.POST : HttpMethod.valueOf(assertion.getHttpMethod().name());

        HttpRequest httpRequest = new DefaultHttpRequest(httpVersion, httpMethod, uri.getRawPath());
        httpRequest.setHeader(HttpHeaders.Names.HOST, uri.getHost());

        MimeKnob mk = targetMessage.getKnob(MimeKnob.class);
        if (mk != null) {
            httpRequest.setHeader(HttpHeaders.Names.CONTENT_TYPE, mk.getOuterContentType().getFullValue());
            httpRequest.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

            // TODO request streaming, large requests, chunked encoding
            httpRequest.setContent(ChannelBuffers.copiedBuffer(IOUtils.slurpStream(mk.getEntireMessageBodyAsInputStream(false))));
            httpRequest.setHeader(HttpHeaders.Names.CONTENT_LENGTH, mk.getContentLength());

            // TODO keepalive support, with per-host connection pooling
            httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        }

        OutboundHeadersKnob heads = targetMessage.getKnob(OutboundHeadersKnob.class);
        if (heads != null) {
            String[] names = heads.getHeaderNames();
            for (String name : names) {
                String[] values = heads.getHeaderValues(name);
                for (String value : values) {
                    httpRequest.addHeader(name, value);
                }
            }
        }

        // TODO outbound cookies
        // TODO configurable header passthrough
        // TODO attach saml sender-vouches
        // TODO authentication

        return httpRequest;
    }

    // Info that will be needed in order to create a PEC for ivoking the response policy,
    // when an async response eventually arrives.
    private static class ResponseContextInfo implements Closeable {
        private PolicyEnforcementContext clonedContext;
        private ServerPolicyHandle serverPolicyHandle;

        @Override
        public void close() {
            ResourceUtils.closeQuietly(serverPolicyHandle);
            serverPolicyHandle = null;
            ResourceUtils.closeQuietly(clonedContext);
            clonedContext = null;
        }
    }

    private void invokeResponsePolicy(final ResponseContextInfo responseContextInfo, final Either<IOException, HttpResponse> result) {

        try {
            final PolicyEnforcementContext context = responseContextInfo.clonedContext;

            final AssertionStatus status[] = { AssertionStatus.UNDEFINED };
            auditContextFactory.doWithNewAuditContext(
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        final AuditContext auditContext = AuditContextFactory.getCurrent();
                        context.setAuditContext(auditContext);
                        try {
                            // Copy response headers and body, or generate a fault message/fault variable in context
                            if (Eithers.isSuccess(result)) {
                                HttpResponse httpResponse = result.right();
                                // Populate successful response
                                final ContentTypeHeader contentType = getContentType(httpResponse);
                                context.getResponse().initialize(stashManagerFactory.createStashManager(), contentType, new ByteBuffersInputStream(Arrays.asList(httpResponse.getContent().toByteBuffers())));
                                context.getResponse().attachHttpResponseKnob(new NettyHttpResponseKnob(httpResponse));
                                // Free up channel buffers early now that we have copied them
                                httpResponse.setContent(null);

                                // TODO attach header knob, copy response headers
                                context.setVariable("async.response.status", Double.valueOf(httpResponse.getStatus().getCode()));
                                context.setVariable("async.response.success", Boolean.TRUE);
                            } else {
                                // Indicate failure
                                // TODO proper faults
                                final IOException e = result.left();
                                final String msg = "Failed to obtain async response: " + ExceptionUtils.getMessage(e);
                                // Don't audit here, the policy can do that if it wants.  (Of course we will audit if the policy fails!)

                                context.getResponse().initialize(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(msg.getBytes(Charsets.UTF8)));
                                context.setVariable("async.response.success", Boolean.FALSE);
                            }

                            status[0] = responseContextInfo.serverPolicyHandle.checkRequest(context);

                            if (!AssertionStatus.NONE.equals(status[0])) {
                                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    "Response policy failed while delivering async response with assertion status: " + status[0]);
                            }

                        } catch (PolicyAssertionException e) {
                            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {
                                "Misconfigured policy assertion encountered while delivering async HTTP response: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                        } catch (IOException e) {
                            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {
                                "I/O error while delivering async HTTP response: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                        } finally {
                            String[] ctxVariables = AuditLogFormatter.getContextVariablesUsed();
                            if (ctxVariables != null && ctxVariables.length > 0) {
                                auditContext.setContextVariables(context.getVariableMap(ctxVariables, getAudit()));
                            }
                        }

                        return null; // Void
                    }
                },
                new Functions.Nullary<com.l7tech.gateway.common.audit.AuditRecord>() {
                    @Override
                    public AuditRecord call() {
                        return messageSummaryAuditFactory.makeEvent(context, status[0]);
                    }
                }
            );
        } catch (Exception e) {
            // Nowhere to audit it, if it hasn't already been audited, so log it
            logger.log(Level.WARNING, "Unexpected exception while delivering async HTTP response: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private ContentTypeHeader getContentType(HttpResponse httpResponse) {
        try {
            return ContentTypeHeader.parseValue(httpResponse.getHeader(HttpHeaders.Names.CONTENT_TYPE));
        } catch (IOException e) {
            return ContentTypeHeader.OCTET_STREAM_DEFAULT;
        }
    }

    // TODO move copyContext() and support methods, copied from ServerConcurrentAll assertion, to some
    // utility class for proper reuse in both places.

    /**
     * Create a new PolicyEnforcmentContext with a blank request and response, all String and Message context
     * variables deep-copied from the specified source context.
     * <p/>
     * The returned PEC is not registered as the current thread-local context, and is safe to use on another thread.
     * It must still be closed when it is no longer needed.
     *
     * @param source the context to copy.  Required.
     * @param varsMap the variables to copy over.  Required.
     * @return a new context with some material copied from the specified one.
     * @throws java.io.IOException if a Message variable is to be copied and it cannot be read
     */
    private PolicyEnforcementContext copyContext(PolicyEnforcementContext source, Map<String, Object> varsMap) throws IOException {
        PolicyEnforcementContext ret = PolicyEnforcementContextFactory.createUnregisteredPolicyEnforcementContext(new Message(), new Message(), false);

        ret.setRequestWasCompressed(source.isRequestWasCompressed());
        ret.setService(source.getService());
        ret.setServicePolicyMetadata(source.getServicePolicyMetadata());
        ret.setCurrentPolicyMetadata(source.getCurrentPolicyMetadata());
        ret.setAuditLevel(source.getAuditLevel());
        ret.setPolicyExecutionAttempted(true);

        for (Map.Entry<String, Object> entry : varsMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            copyValue(ret, name, value);
        }

        for ( final Integer ordinal : source.getAssertionOrdinalPath() ) {
            ret.pushAssertionOrdinal( ordinal );
        }

        return ret;
    }


    private void copyValue(PolicyEnforcementContext ret, String name, Object value) throws IOException {
        if (value == null) {
            safeSetVariable(ret, name, null);
        } else if (value instanceof String) {
            safeSetVariable(ret, name, value);
        } else if (value instanceof Message) {
            safeSetVariable(ret, name, cloneMessageBody((Message)value));
        } else if (value instanceof Object[]) {
            Class<?> elementType = value.getClass().getComponentType();
            if (isSafeToCopyArrayOf(elementType)) {
                Object[] arr = ((Object[])value);
                safeSetVariable(ret, name, Arrays.copyOf(arr, arr.length));
            } else {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Variable named " + name + " has unsupported type: " + value.getClass());
            }
        } else {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Variable named " + name + " has unsupported type: " + value.getClass());
        }
    }

    private static boolean isSafeToCopyArrayOf(Class<?> elementType) {
        // We know we can safely copy with Arrays.copyOf(Object[]) arrays of primitive types (int, char, byte, short, double, etc),
        // arrays of String, and arrays of boxed primitive types (Integer, Character, Byte, Short, Double, etc).
        // We recognized boxed primitive types by checking for subclasses of java.lang.Number that are in the java.lang package.
        return elementType.isPrimitive() ||
            String.class.equals(elementType) ||
            (Number.class.isAssignableFrom(elementType) && elementType.getPackage().equals(Integer.class.getPackage()));
    }

    private void safeSetVariable(PolicyEnforcementContext ctx, String name, @Nullable Object value) {
        try {
            ctx.setVariable(name, value);
        } catch (VariableNotSettableException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Unable to set non-settable variable: " + name);
            // Ignore it and fallthrough
        }
    }

    static Message cloneMessageBody(Message source) throws IOException {
        MimeKnob mk = source.getKnob(MimeKnob.class);
        if (mk == null || !source.isInitialized())
            return new Message(); // not yet initialized

        try {
            byte[] sourceBytes = IOUtils.slurpStream(mk.getEntireMessageBodyAsInputStream());
            return new Message(new ByteArrayStashManager(), mk.getOuterContentType(), new ByteArrayInputStream(sourceBytes));
        } catch (NoSuchPartException e) {
            throw new IOException(e);
        }
    }

}
