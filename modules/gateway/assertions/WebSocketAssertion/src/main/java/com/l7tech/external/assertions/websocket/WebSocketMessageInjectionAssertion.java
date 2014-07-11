package com.l7tech.external.assertions.websocket;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME;

/**
 * Purpose: This assertion will validate WebSocket message with associated XML Schema,
 * If validation success and Send Response Immediately is not checked it will return WebSocket message in a context variable
 * If Send Response Immediately is checked it will exit the policy immediately
 * If validation fail assertion will fail
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 6/29/12
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketMessageInjectionAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(WebSocketMessageInjectionAssertion.class.getName());

    // Updated for 8.0 to support GOIDs
    private Goid serviceOid;
    private String clientIds;
    private boolean inbound = true;
    private boolean textMessage = true;
    private boolean broadcast;
    private String message;
    private String subprotocol;
    private boolean deliveryFailure;

    public Goid getServiceOid() {
        return serviceOid;
    }

    public void setServiceOid(long serviceOid) {
        this.serviceOid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, serviceOid);
    }
    public void setServiceOid(Goid serviceOid) {
        this.serviceOid = serviceOid;
    }

    public String getClientIds() {
        return clientIds;
    }

    public void setClientIds(String clientIds) {
        this.clientIds = clientIds;
    }

    public boolean isInbound() {
        return inbound;
    }

    /*
     * True = Inbound, False = Outbound
     */
    public void setInbound(boolean inbound) {
        this.inbound = inbound;
    }

    public boolean isTextMessage() {
        return textMessage;
    }

    /*
    * True = Text, False = Binary
    */
    public void setTextMessage(boolean textMessage) {
        this.textMessage = textMessage;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSubprotocol() {
        return subprotocol;
    }

    public void setSubprotocol(String subprotocol) {
        this.subprotocol = subprotocol;
    }

    public boolean isDeliveryFailure() {
        return deliveryFailure;
    }

    public void setDeliveryFailure(boolean deliveryFailure) {
        this.deliveryFailure = deliveryFailure;
    }

    @Override
    public String[] getVariablesUsed() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClientIds());
        sb.append(" ").append(this.getMessage());
        sb.append(" ").append(this.getSubprotocol());
        return Syntax.getReferencedNames(sb.toString());
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = WebSocketMessageInjectionAssertion.class.getName() + ".metadataInitialized";

    @Override
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
//        meta.put(AssertionMetadata.SHORT_NAME, "Send WebSocket Message");
//        meta.put(AssertionMetadata.LONG_NAME, "Send WebSocket Message into with xml schema");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
//        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{""});
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.websocket.console.WebSocketMessageInjectionDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put( MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.websocket.server.WebSocketInjectionLoadListener" );

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WebSocket" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
