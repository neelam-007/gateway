package com.l7tech.external.assertions.retrieveservicewsdl;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

/**
 * Retrieves a WSDL for a service and rewrites the endpoints and references.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class RetrieveServiceWsdlAssertion extends Assertion implements UsesVariables, SetsVariables {
    private static final String ICON_FILE = "com/l7tech/external/assertions/retrieveservicewsdl/WSDL_File_16x16.png";
    private final static String BASE_NAME = "Retrieve Service WSDL";
    private final static String LONG_NAME = "Retrieve WSDL for Service";
    private static final String META_INITIALIZED =
            RetrieveServiceWsdlAssertion.class.getName() + ".metadataInitialized";

    public static final int PORT_RANGE_START = 1;
    public static final int PORT_RANGE_END = 65535;

    @NotNull
    private String serviceId = "${service.oid}";

    private String protocol = "http";

    private String protocolVariable = null;

    @NotNull
    private String host = "${gateway.cluster.hostname}";

    @NotNull
    private String port = "8080";

    @NotNull
    private MessageTargetableSupport messageTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);

    private boolean proxyDependencies = false;

    @NotNull
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(@NotNull String serviceId) {
        this.serviceId = serviceId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocolVariable() {
        return protocolVariable;
    }

    public void setProtocolVariable(String protocolVariable) {
        this.protocolVariable = protocolVariable;
    }

    @NotNull
    public String getHost() {
        return host;
    }

    public void setHost(@NotNull String host) {
        this.host = host;
    }

    @NotNull
    public String getPort() {
        return port;
    }

    public void setPort(@NotNull String port) {
        this.port = port;
    }

    @NotNull
    public MessageTargetableSupport getMessageTarget() {
        return messageTarget;
    }

    public void setMessageTarget(@NotNull MessageTargetableSupport messageTarget) {
        this.messageTarget = messageTarget;
    }

    public boolean isProxyDependencies() {
        return proxyDependencies;
    }

    public void setProxyDependencies(boolean proxyDependencies) {
        this.proxyDependencies = proxyDependencies;
    }

    public String[] getVariablesUsed() {
        return messageTarget.getMessageTargetVariablesUsed()
                .withExpressions(serviceId, protocolVariable, host, port)
                .asArray();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (TargetMessageType.OTHER == messageTarget.getTarget()) {
            return new VariableMetadata[] {
                    new VariableMetadata(messageTarget.getOtherTargetMessageVariable(),
                            false, false, null, true, DataType.MESSAGE)
            };
        }

        return new VariableMetadata[0];
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.LONG_NAME, LONG_NAME);
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<RetrieveServiceWsdlAssertion>() {
            @Override
            public String getAssertionName(final RetrieveServiceWsdlAssertion assertion, final boolean decorate) {
                if (!decorate) return BASE_NAME;

                return LONG_NAME + " [" + assertion.getServiceId() + "]";
            }
        });

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "internalAssertions" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, ICON_FILE);
        meta.put(AssertionMetadata.POLICY_NODE_ICON, ICON_FILE);

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    public Object clone() {
        RetrieveServiceWsdlAssertion clone = (RetrieveServiceWsdlAssertion) super.clone();
        clone.messageTarget = new MessageTargetableSupport(messageTarget);
        return clone;
    }
}
