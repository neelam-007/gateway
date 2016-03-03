package com.l7tech.console.tree.policy;


import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Class JmsRoutingAssertionTreeNode.
 * 
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 */
public class JmsRoutingAssertionTreeNode extends DefaultAssertionPolicyNode<JmsRoutingAssertion> {

    //- PUBLIC

    public JmsRoutingAssertionTreeNode( final JmsRoutingAssertion assertion ) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName( final boolean decorate, final boolean withComments ) {
        final JmsRoutingAssertion ass = (JmsRoutingAssertion) getUserObject();
        final String assertionName = ass.meta().get(AssertionMetadata.SHORT_NAME).toString();
        if(!decorate) return assertionName;

        String actor = SecurityHeaderAddressableSupport.getActorSuffix(ass);

        String displayText;
        if (ass.getEndpointOid() == null) {
            displayText = assertionName + " (Not Yet Specified)" + actor;
        } else {
            final String endpointDescription = endpointDescription();
            displayText = assertionName + " " + (endpointDescription == null ? "Destination (unnamed)" : endpointDescription) + actor;
        }

        if (!withComments) {
            return displayText;
        }

        return addCommentToDisplayText(assertion, displayText);
    }
    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<>(Arrays.asList(super.getActions()));

        boolean found = false;
        for (Action action: list) {
            if (action instanceof EditXmlSecurityRecipientContextAction) {
                found = true;
                break;
            }
        }
        // If not found EditXmlSecurityRecipientContextAction, create a new one and add into the list at the first position.
        if (! found) {
            if (getUserObject() instanceof SecurityHeaderAddressable) {
                list.add(0, new EditXmlSecurityRecipientContextAction(this));
            }
        }
        
        return list.toArray(new Action[list.size()]);
    }

    @Override
    public void setUserObject( final Object userObject ) {
        invalidateCachedInfo();
        super.setUserObject( userObject );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(JmsRoutingAssertionTreeNode.class.getName());
    private String endpointDescription;

    private String endpointDescription() {
        String endpointDescription = this.endpointDescription;

        if ( endpointDescription == null ) {
            final JmsRoutingAssertion ass = (JmsRoutingAssertion) getUserObject();
            try {
                final JmsEndpoint endpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(ass.getEndpointOid());
                if( endpoint != null ) {
                    endpointDescription = (endpoint.isQueue() ? "Queue " : "Topic ") + endpoint.getName();
                }
            } catch(FindException e) {
                // Use name from assertion
            }

            if ( endpointDescription == null ) {
                endpointDescription = "Destination " + ass.getEndpointName();
            }

            this.endpointDescription = endpointDescription;
        }

        return endpointDescription;
    }

    private void invalidateCachedInfo() {
        endpointDescription = null;
    }
}
