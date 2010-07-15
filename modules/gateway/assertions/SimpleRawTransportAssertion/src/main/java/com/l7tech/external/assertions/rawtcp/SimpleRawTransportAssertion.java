package com.l7tech.external.assertions.rawtcp;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.SyspropUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Bean for configuring outbound raw TCP (and someday TLS) single-shot message.
 */
public class SimpleRawTransportAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(SimpleRawTransportAssertion.class.getName());

    public static final int DEFAULT_WRITE_TIMEOUT = SyspropUtil.getInteger("com.l7tech.external.assertions.rawtcp.defaultWriteTimeout", 2000);
    public static final int DEFAULT_READ_TIMEOUT = SyspropUtil.getInteger("com.l7tech.external.assertions.rawtcp.defaultReadTimeout", 2000);
    public static final long DEFAULT_RESPONSE_SIZE_LIMIT = SyspropUtil.getLong("com.l7tech.external.assertions.rawtcp.defaultResponseSizeLimit", 1024 * 1024);

    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private long maxResponseBytes = DEFAULT_RESPONSE_SIZE_LIMIT;
    private String responseContentType = "text/xml; charset=UTF-8";
    private int writeTimeoutMillis = DEFAULT_WRITE_TIMEOUT;
    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT;
    private String targetHost = null;
    private int targetPort = 13224;

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
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

    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(long maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
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

    //
    // Metadata
    //
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.rawtcp.server.ModuleLoadListener");

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Raw TCP Routing");
        meta.put(AssertionMetadata.DESCRIPTION, "Send a request over raw TCP");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.rawtcp.console.SimpleRawTransportAssertionPropertiesDialog");

        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        ret.addAll(Arrays.asList(requestTarget.getVariablesUsed()));
        ret.addAll(Arrays.asList(responseTarget.getVariablesUsed()));
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return responseTarget.mergeVariablesSet(requestTarget.getVariablesSet());
    }
}
