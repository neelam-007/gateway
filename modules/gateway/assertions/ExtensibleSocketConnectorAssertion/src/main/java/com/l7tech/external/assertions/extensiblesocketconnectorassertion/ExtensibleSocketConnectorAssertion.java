package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.console.ExtensibleSocketConnectorsDialog;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorEntityManagerServerSupport;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.search.Dependency;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapper;
import org.springframework.context.ApplicationContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class ExtensibleSocketConnectorAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorAssertion.class.getName());

    public static final String PARAM_EXTRA_CODECS = "extensibleSocketConnector.codecs";

    private Goid socketConnectorGoid = null;

    private MessageTargetableSupport requestTarget;
    private MessageTargetableSupport responseTarget;

    private String sessionIdStoreVariable = null;
    private String sessionIdVariable = null;
    private boolean failOnNoSession = false;

    @Override
    public String[] getVariablesUsed() {

        List<String> completeList = new ArrayList<String>();

        completeList.addAll(Arrays.asList(requestTarget.getVariablesUsed()));

        if (sessionIdVariable != null && !sessionIdVariable.trim().isEmpty()) {
            completeList.add(sessionIdVariable);
        }

        if (sessionIdStoreVariable != null && !sessionIdStoreVariable.trim().isEmpty()) {
            completeList.add(sessionIdStoreVariable);
        }

        return completeList.toArray(new String[]{});
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return responseTarget.getVariablesSet();
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
        return requestTarget != null && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    /**
     * This is here for backwards compatibility, we will take the old OID and convert it to a GOID;
     * then disregard the OID altogether.
     *
     * @param socketConnectorOid the OID to convert to a GOID
     */
    public void setSocketConnectorOid(long socketConnectorOid) {
        if (socketConnectorGoid == null && socketConnectorOid > -1) {
            socketConnectorGoid = GoidUpgradeMapper.mapOid(EntityType.GENERIC, socketConnectorOid);
        }
    }

    @Dependency(type = Dependency.DependencyType.GENERIC, methodReturnType = Dependency.MethodReturnType.GOID)
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public Goid getSocketConnectorGoid() {
        return socketConnectorGoid;
    }

    public void setSocketConnectorGoid(Goid socketConnectorGoid) {
        this.socketConnectorGoid = socketConnectorGoid;
    }

    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    public void setResponseTarget(MessageTargetableSupport responseTarget) {
        this.responseTarget = responseTarget;
    }

    public String getSessionIdStoreVariable() {
        return sessionIdStoreVariable;
    }

    public void setSessionIdStoreVariable(String sessionIdStoreVariable) {
        this.sessionIdStoreVariable = sessionIdStoreVariable;
    }

    public String getSessionIdVariable() {
        return sessionIdVariable;
    }

    public void setSessionIdVariable(String sessionIdVariable) {
        this.sessionIdVariable = sessionIdVariable;
    }

    public boolean isFailOnNoSession() {
        return failOnNoSession;
    }

    public void setFailOnNoSession(boolean failOnNoSession) {
        this.failOnNoSession = failOnNoSession;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(PARAM_EXTRA_CODECS, new String[]{
                "Additional Codecs that have been added to the ExtensibleSocketConnetor AAR file.",
                ""
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Send To Remote Socket");
        meta.put(AssertionMetadata.LONG_NAME, "Send To Remote Socket Assertion");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"routing"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{getClass().getName() + "$CustomAction"});

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.extensiblesocketconnectorassertion.console.ExtensibleSocketConnectorAssertionPropertiesDialog");

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorModuleLoadListener");

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return ExtensibleSocketConnectorEntityManagerServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Mllp" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class CustomAction extends AbstractAction {
        public CustomAction() {
            super("Manage Socket Connectors", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ExtensibleSocketConnectorsDialog dialog = new ExtensibleSocketConnectorsDialog(TopComponents.getInstance().getTopParent());
            Utilities.centerOnScreen(dialog);
            DialogDisplayer.display(dialog);
        }
    }
}
