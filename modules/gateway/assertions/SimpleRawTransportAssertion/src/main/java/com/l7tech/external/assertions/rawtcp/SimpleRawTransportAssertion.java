package com.l7tech.external.assertions.rawtcp;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Bean for configuring outbound raw TCP/UDP (and someday TLS) single-shot message.
 */
public class SimpleRawTransportAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(SimpleRawTransportAssertion.class.getName());

    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private long maxRequestBytes = Integer.MAX_VALUE;
    private long maxResponseBytes = Integer.MAX_VALUE;
    private String responseContentType = "text/xml; charset=UTF-8";
    private int writeTimeoutMillis = 2000;
    private int readTimeoutMillis = 2000;
    private String targetHost = null;
    private int targetPort = 13224;
    private boolean udp = false;

    // TODO TLS support
    //private boolean tls = false;
    //private long clientKeystoreId = 0;
    //private String clientKeyAlias = null;

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

    public boolean isUdp() {
        return udp;
    }

    /**
     * Set whether to use UDP or TCP.
     *
     * @param udp if true, the server assertion will attempt to fit the request into a single UDP datagram.
     *            UDP routing requires a request target, and currently does not support waiting for a response.
     */
    public void setUdp(boolean udp) {
        this.udp = udp;
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

    public long getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(long maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
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

        // Set up extra tasks menu actions
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] {
                "com.l7tech.external.assertions.rawtcp.console.RawTcpStatusAction",
                "com.l7tech.external.assertions.rawtcp.console.RawUdpStatusAction"
        });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Raw TCP/UDP Routing");
        meta.put(AssertionMetadata.DESCRIPTION, "Send a request over raw TCP or UDP");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

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
