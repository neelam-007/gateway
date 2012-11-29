package com.l7tech.external.assertions.rawtcp;

import com.l7tech.external.assertions.rawtcp.server.SimpleRawTransportAdminImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Bean for configuring outbound raw TCP (and someday TLS) single-shot message.
 */
public class SimpleRawTransportAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {
    public static final int DEFAULT_WRITE_TIMEOUT = ConfigFactory.getIntProperty( "com.l7tech.external.assertions.rawtcp.defaultWriteTimeout", 2000 );
    public static final int DEFAULT_READ_TIMEOUT = ConfigFactory.getIntProperty( "com.l7tech.external.assertions.rawtcp.defaultReadTimeout", 2000 );
    public static final long DEFAULT_RESPONSE_SIZE_LIMIT = ConfigFactory.getLongProperty( "com.l7tech.external.assertions.rawtcp.defaultResponseSizeLimit", -1 );
    public static final long DEFAULT_REQUEST_SIZE_LIMIT = ConfigFactory.getLongProperty( "com.l7tech.external.assertions.rawtcp.defaultRequestSizeLimit", 1024 * 1024 );

    public static final String LISTEN_PROP_BACKLOG = "l7.raw.backlog";
    public static final String LISTEN_PROP_READ_TIMEOUT = "l7.raw.readTimeout";
    public static final String LISTEN_PROP_WRITE_TIMEOUT = "l7.raw.writeTimeout";

    public static final String VAR_RAW_TCP_REASON_CODE = "rawtcp.reasonCode";

    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private String maxResponseBytesText = Long.toString(DEFAULT_RESPONSE_SIZE_LIMIT);
    private String responseContentType = "text/xml; charset=UTF-8";
    private int writeTimeoutMillis = DEFAULT_WRITE_TIMEOUT;
    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT;
    private String targetHost = null;
    private String targetPort = "13224";

    public String getTargetHost() {
        return targetHost;
    }

    /**
     * This method exists for backwards compatibility with serialized SimpleRawTransportAssertion.
     *
     * This method simply calls {@link #setTargetPort(String)} by converting the given int to String
     *
     * @deprecated Use {@link #setTargetPort(String)} instead.
     * @see #setTargetPort(String)
     */
    @Deprecated
    public void setTargetPort(int targetPort) {
        setTargetPort(Integer.toString(targetPort));
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public String getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(String targetPort) {
        this.targetPort = targetPort;
    }

    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    /**
     * Set the target message (or variable) into which the Response shall be saved.
     *
     * @param responseTarget Response Message, or null if no response is expected.
     */
    public void setResponseTarget(MessageTargetableSupport responseTarget) {
        this.responseTarget = responseTarget;
    }

    @Override
    public boolean initializesRequest() {
        return responseTarget != null && TargetMessageType.REQUEST == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedRequest() {
        return requestTarget == null || TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return responseTarget != null && TargetMessageType.RESPONSE == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedResponse() {
        // Response must be initialized only in the extremely unlikely event that we are sending the default response as the routing request
        return requestTarget != null && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    /**
     * Set the target message (or variable) to use as the Request.
     *
     * @param requestTarget  Request Message.  Required.
     */
    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    public String getMaxResponseBytesText() {
        return maxResponseBytesText;
    }

    public void setMaxResponseBytesText( String maxResponseBytesText ) {
        this.maxResponseBytesText = maxResponseBytesText;
    }

    public void setMaxResponseBytes( long maxResponseBytes ) {
        if ( maxResponseBytes >= 0L ) {
            this.maxResponseBytesText = Long.toString( maxResponseBytes );
        }
    }

    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    private static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SimpleRawTransportAssertion>(){
        @Override
        public String getAssertionName( final SimpleRawTransportAssertion assertion, final boolean decorate) {
            if (!decorate)
                return "Route via Raw TCP";
            StringBuilder sb = new StringBuilder("Route via Raw TCP");

            String targetHost = assertion.getTargetHost();
            if (targetHost != null && targetHost.trim().length() > 0) {
                sb.append(" to ").append(targetHost);
                sb.append(" port ").append(assertion.getTargetPort());
            }

            return AssertionUtils.decorateName(assertion, sb.toString());
        }
    };

    //
    // Metadata
    //
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.rawtcp.server.ModuleLoadListener");

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Route via Raw TCP");
        meta.put(AssertionMetadata.DESCRIPTION, "Send a request over raw TCP");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.rawtcp.console.SimpleRawTransportAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Raw TCP Routing Properties");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final ExtensionInterfaceBinding<SimpleRawTransportAdmin> binding = new ExtensionInterfaceBinding<SimpleRawTransportAdmin>(
                        SimpleRawTransportAdmin.class,
                        null,
                        new SimpleRawTransportAdminImpl());
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        return requestTarget.getMessageTargetVariablesUsed().withExpressions(
            responseContentType,
            targetHost,
                maxResponseBytesText,
                targetPort
        ).asArray();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        MessageTargetableSupport.VariablesSet variablesSet = responseTarget.getMessageTargetVariablesSet();
        variablesSet.add(requestTarget.getMessageTargetVariablesSet());
        variablesSet.addVariables(new VariableMetadata(VAR_RAW_TCP_REASON_CODE, false, false, VAR_RAW_TCP_REASON_CODE, false, DataType.INTEGER));
        return variablesSet.asArray();
    }
}
