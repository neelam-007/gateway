package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SchemaValidationPropertiesAction;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Policy tree node for Schema Validation Assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 5, 2004<br/>
 * $Id$<br/>
 */
public class SchemaValidationTreeNode extends LeafAssertionTreeNode {
    public SchemaValidationTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof SchemaValidation) {
            nodeAssertion = (SchemaValidation)assertion;
        } else
            throw new IllegalArgumentException("assertion passed must be of type " +
              SchemaValidation.class.getName());
        try {
            if (getServiceNodeCookie() != null) {
                service = getServiceNodeCookie().getPublishedService();
            } else {
                log.log(Level.WARNING, "no access to service node cookie");
            }

        } catch (FindException e) {
            log.log(Level.WARNING, "cannot get service", e);
        } catch (RemoteException e) {
            log.log(Level.WARNING, "cannot get service", e);
        }

    }

    public String getName() {
        return "[Validate message's schema]";
    }

    protected String iconResource(boolean open) {
        // todo, a special icon for this assertion?
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();

        final PublishedService publishedService;
        try {
            publishedService = getServiceNodeCookie().getPublishedService();
            list.add(new SchemaValidationPropertiesAction(this, publishedService));
        } catch (Exception e) {
            // tango down!
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
        try {
            final PublishedService publishedService = getServiceNodeCookie().getPublishedService();
            return new SchemaValidationPropertiesAction(this, publishedService);
        } catch (Exception e) {
            // tango down!
        }
        return null;
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    public SchemaValidation getAssertion() {return nodeAssertion;}

    private SchemaValidation nodeAssertion;
    private PublishedService service;

    private final Logger log = Logger.getLogger(getClass().getName());
}
