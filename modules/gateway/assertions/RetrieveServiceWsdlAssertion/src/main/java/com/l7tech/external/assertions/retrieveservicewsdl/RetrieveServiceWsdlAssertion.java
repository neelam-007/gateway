package com.l7tech.external.assertions.retrieveservicewsdl;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

/**
 * Retrieves and rewrite the endpoints of a WSDL for a service.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class RetrieveServiceWsdlAssertion extends Assertion implements UsesVariables, SetsVariables {
    private final static String BASE_NAME = "Retrieve Service WSDL";
    private final static String LONG_NAME = "Retrieve WSDL for Service";
    private static final String META_INITIALIZED =
            RetrieveServiceWsdlAssertion.class.getName() + ".metadataInitialized";

    @NotNull
    private String serviceId = "${service.oid}";

    @NotNull
    private String hostname = "${gateway.cluster.hostname}";

    @NotNull
    private MessageTargetableSupport messageTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);

    @NotNull
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(@NotNull String serviceId) {
        this.serviceId = serviceId;
    }

    @NotNull
    public String getHostname() {
        return hostname;
    }

    public void setHostname(@NotNull String hostname) {
        this.hostname = hostname;
    }

    @NotNull
    public MessageTargetableSupport getMessageTarget() {
        return messageTarget;
    }

    public void setMessageTarget(@NotNull MessageTargetableSupport messageTarget) {
        this.messageTarget = messageTarget;
    }

    public String[] getVariablesUsed() {
        return messageTarget.getMessageTargetVariablesUsed()
                .withExpressions(Syntax.getReferencedNames(serviceId, hostname))
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
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/CreateWSDL16x16.gif"); // TODO jwilliams: get new icon
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/CreateWSDL16x16.gif"); // TODO jwilliams: get new icon

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
