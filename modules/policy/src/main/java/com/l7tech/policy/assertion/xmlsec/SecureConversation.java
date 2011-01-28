package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * This assertion requires the client to establish a secure conversation prior
 * to consuming the service and use this secure conversation context to secure
 * requests for the consumption of the service.
 * <p/>
 * It can be used in conjonction with RequestWss* and ResponseWss* assertions
 * if the the administrator wishes to specify which elements are signed and
 * or encrypted.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class SecureConversation extends SecurityHeaderAddressableSupport implements SetsVariables {
    public static final String VARIABLE_INBOUND_SC_SESSION_ID = "inboundSC.session.id";

    /**
     *Secure Conversation is always credential source
     *
     * @return always true
     */
    public boolean isCredentialSource() {
        return true;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Require WS-SecureConversation");
        meta.put(AssertionMetadata.DESCRIPTION, "Requires that requests and responses be secured using the WS-SecureConversation protocol.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(VARIABLE_INBOUND_SC_SESSION_ID, false, false, null, false, DataType.STRING),
        };
    }
}