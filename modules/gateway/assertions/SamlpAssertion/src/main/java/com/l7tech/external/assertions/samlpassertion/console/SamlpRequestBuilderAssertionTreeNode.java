/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.tree.policy.LeafAssertionTreeNode;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;

public class SamlpRequestBuilderAssertionTreeNode extends LeafAssertionTreeNode {

    public SamlpRequestBuilderAssertionTreeNode(SamlpRequestBuilderAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
//        HttpRoutingAssertion assertion = ((HttpRoutingAssertion)getUserObject());
//        String actor = "";
//        XmlSecurityRecipientContext context = assertion.getRecipientContext();
//        if (context != null)
//            actor = context.localRecipient()
//                    ? ""
//                    : " [\'" + assertion.getRecipientContext().getActor() + "\' actor]";
//        String url = assertion.getProtectedServiceUrl();
//        if (url != null) {
//            return "Route to " + url + actor;
//        }
//        return "Route" + actor;
        return "SAMLP Request Builder";
    }

//    /**
//     * Get the set of actions associated with this node.
//     * This may be used e.g. in constructing a context menu.
//     *
//     * @return actions appropriate to the node
//     */
//    public Action[] getActions() {
//        EditKeyAliasForAssertion privateKeyAction = new EditKeyAliasForAssertion(this);
//        if (!isUsingPrivateKey()) {
//            privateKeyAction.setEnabled(false);
//            privateKeyAction.putValue(Action.SHORT_DESCRIPTION, "Disabled because the URL is not HTTPS");
//        }
//
//        java.util.List<Action> list = new ArrayList<Action>();
//        list.add(privateKeyAction);
//        if (getUserObject() instanceof SecurityHeaderAddressable) {
//            list.add(new EditXmlSecurityRecipientContextAction(this));
//        }
//        list.addAll(Arrays.asList(super.getActions()));
//        return list.toArray(new Action[0]);
//    }
//
//    protected boolean isUsingPrivateKey() {
//        HttpRoutingAssertion assertion = ((HttpRoutingAssertion)getUserObject());
//        if (assertion == null)
//            return false;
//        String url = assertion.getProtectedServiceUrl();
//        return url != null && url.toLowerCase().startsWith("https:");
//    }
//
//    /**
//     * Gets the default action for this node.
//     *
//     * @return <code>null</code> indicating there should be none default action
//     */
//    public Action getPreferredAction() {
//        return new HttpRoutingAssertionPropertiesAction(this);
//    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/server16.gif";
    }

}