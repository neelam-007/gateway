package com.l7tech.console.tree.policy;


import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.JmsRoutingAssertionPropertiesAction;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class JmsRoutingAssertionTreeNode.
 * 
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 */
public class JmsRoutingAssertionTreeNode extends DefaultAssertionPolicyNode<JmsRoutingAssertion> {

    public JmsRoutingAssertionTreeNode(JmsRoutingAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName(final boolean decorate) {
        final JmsRoutingAssertion ass = (JmsRoutingAssertion) getUserObject();
        final String assertionName = ass.meta().get(AssertionMetadata.SHORT_NAME).toString();
        if(!decorate) return assertionName;

        String actor = SecurityHeaderAddressableSupport.getActorSuffix(ass);

        if (ass.getEndpointOid() == null) {
            return assertionName + " (Not Yet Specified)" + actor;
        }
        String endpointName;
        try {
            JmsEndpoint endpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(ass.getEndpointOid());
            if(endpoint != null) {
                endpointName = endpoint.getName();
            } else {
                endpointName = ass.getEndpointName();
            }
        } catch(FindException e) {
            endpointName = ass.getEndpointName();
        }
        return assertionName +" Queue " + (endpointName == null ? "(unnamed)" : endpointName) + actor;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        if (getUserObject() instanceof SecurityHeaderAddressable) {
            list.add(new EditXmlSecurityRecipientContextAction(this));
        }
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }
}