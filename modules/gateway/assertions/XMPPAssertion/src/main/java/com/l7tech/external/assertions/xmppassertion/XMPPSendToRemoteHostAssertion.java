package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.external.assertions.xmppassertion.server.XMPPConnectionEntityManagerServerSupport;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/03/12
 * Time: 10:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPSendToRemoteHostAssertion extends RoutingAssertion implements UsesVariables, MessageTargetable {
    protected static final Logger logger = Logger.getLogger(XMPPSendToRemoteHostAssertion.class.getName());

    private boolean toOutboundConnection = true;
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST);
    private String sessionId;

    @Override
    public String[] getVariablesUsed() {
        if(requestTarget != null) {
            return requestTarget.getMessageTargetVariablesUsed().withExpressions(sessionId).asArray();
        } else {
            return Syntax.getReferencedNames(sessionId);
        }
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[0];
    }

    @Override
    public TargetMessageType getTarget() {
        return requestTarget.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        requestTarget.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return requestTarget.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        requestTarget.setOtherTargetMessageVariable(otherMessageVariable);
    }

    @Override
    public String getTargetName() {
        return requestTarget.getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return false;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return requestTarget == null || TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return false;
    }

    @Override
    public boolean needsInitializedResponse() {
        return requestTarget != null && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    public boolean isToOutboundConnection() {
        return toOutboundConnection;
    }

    public void setToOutboundConnection(boolean toOutboundConnection) {
        this.toOutboundConnection = toOutboundConnection;
    }

    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = XMPPSendToRemoteHostAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Send To Remote XMPP Host");
        meta.put(AssertionMetadata.LONG_NAME, "Send To Remote XMPP Host Assertion");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmppassertion.console.XMPPSendToRemoteHostAssertionPropertiesDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        //Register Action class for task framework
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.xmppassertion.console.XMPPConnectionManagerAction"});

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntityAdminImpl");

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return XMPPConnectionEntityManagerServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Mllp" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
