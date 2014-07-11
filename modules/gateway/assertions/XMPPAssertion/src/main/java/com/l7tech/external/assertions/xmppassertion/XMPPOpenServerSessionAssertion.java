package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: njordan
 * Date: 14/03/12
 * Time: 2:17 PM
 */
public class XMPPOpenServerSessionAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(XMPPOpenServerSessionAssertion.class.getName());

    public static final String OUTBOUND_SESSION_ID_VAR_NAME = "xmpp.outbound.sessionID";

    private Goid xmppConnectionId;

    public XMPPOpenServerSessionAssertion() {
        String a = "5";
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {new VariableMetadata(OUTBOUND_SESSION_ID_VAR_NAME, false, false, null, false)};
    }

    public Goid getXMPPConnectionId() {
        return xmppConnectionId;
    }

    // To smooth migration from OID to GOID, apparently.
    @Deprecated
    public void setXMPPConnectionId(long xmppConnectionId) {
        this.xmppConnectionId = GoidUpgradeMapper.mapOid(EntityType.SSG_CONNECTOR, xmppConnectionId);
    }
    public void setXMPPConnectionId(Goid xmppConnectionId) {
        this.xmppConnectionId = xmppConnectionId;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = XMPPOpenServerSessionAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
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
        meta.put(AssertionMetadata.SHORT_NAME, "Open XMPP Server Session");
        meta.put(AssertionMetadata.LONG_NAME, "Open XMP Server Session Assertion");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmppassertion.console.XMPPOpenServerSessionAssertionPropertiesDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<XMPPOpenServerSessionAssertion>() {
            @Override
            public String getAssertionName(final XMPPOpenServerSessionAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                if(decorate) {
                    String xmppConnection = "Invalid Outbound XMPP Connection selected";

                    if(assertion.getXMPPConnectionId() != null && !assertion.getXMPPConnectionId().equals(XMPPConnectionEntity.DEFAULT_GOID)) {
                        XMPPConnectionEntityAdmin xmppConnectionEntityAdmin = Registry.getDefault().getExtensionInterface(XMPPConnectionEntityAdmin.class, null);
                        try {
                            XMPPConnectionEntity entity = xmppConnectionEntityAdmin.find(assertion.getXMPPConnectionId());
                            if(entity != null && !entity.isInbound()) {
                                xmppConnection = entity.getName();
                            }
                        } catch(FindException e) {
                            // Ignore
                        }
                    }

                    return displayName + " [" + xmppConnection + "]";
                } else {
                    return displayName;
                }
            }
        });

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Mllp" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
