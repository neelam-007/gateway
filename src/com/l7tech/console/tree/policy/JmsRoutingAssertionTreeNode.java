package com.l7tech.console.tree.policy;


import com.l7tech.console.action.JmsRoutingAssertionPropertiesAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class JmsRoutingAssertionTreeNode.
 * 
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 */
public class JmsRoutingAssertionTreeNode extends LeafAssertionTreeNode {

    public JmsRoutingAssertionTreeNode(RoutingAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        final String name = "Route to JMS Queue ";
        JmsRoutingAssertion ass = (JmsRoutingAssertion) getUserObject();
        String actor = "";
        XmlSecurityRecipientContext context = ass.getRecipientContext();
        if (context != null)
            actor = context.localRecipient()
                    ? ""
                    : " [\'" + ass.getRecipientContext().getActor() + "\' actor]";
        String endpointName = null;
        if (ass.getEndpointOid() == null) {
            return name + "(Not Yet Specified)" + actor;
        } else {
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
        }
        return name + (endpointName == null ? "(unnamed)" : endpointName) + actor;
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

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new JmsRoutingAssertionPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/server16.gif";
    }

}