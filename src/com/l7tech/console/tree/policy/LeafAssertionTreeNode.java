/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Cookie;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.wsdl.WSDLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.rmi.RemoteException;

/**
 * Leaf policy nodes extend this node
 */
abstract class LeafAssertionTreeNode extends AssertionTreeNode {
    private static final Logger log =
      Logger.getLogger(LeafAssertionTreeNode .class.getName());
    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public LeafAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    public boolean receive(AbstractTreeNode node) {
        if (true == super.receive(node)) {
            return true;
        }
        JTree tree =
          (JTree)ComponentRegistry.
          getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion nass = node.asAssertion();

            if (nass != null) {
                AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(nass);
                as = prepareNode(as);
                final MutableTreeNode parent = (MutableTreeNode)getParent();

                int index = parent.getIndex(this);
                if (index == -1) {
                    throw new IllegalStateException("Unknown node to the three model "+this);
                }
                model.insertNodeInto(as, parent, index+1);
            } else {
                log.log(Level.WARNING, "The node has no associated assertion " + node);
            }
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }

    /**
        * some nodes need to be additionally prepared
        * @param node to prapre
        * @return the preapred node
        */
       protected AssertionTreeNode prepareNode(AssertionTreeNode node) {
           if (node instanceof RoutingAssertionTreeNode) {
               String url = "Unable to determine the service url. Please edit";
               for (Iterator i = ((AbstractTreeNode)this.getRoot()).cookies(); i.hasNext();) {
                   Object value = ((Cookie)i.next()).getValue();
                   if (value instanceof ServiceNode) {
                       ServiceNode sn = (ServiceNode)value;
                       try {
                           url = sn.getPublishedService().parsedWsdl().getServiceURI();
                       } catch (WSDLException e) {
                           log.log(Level.WARNING, "Wsdl error", e);
                       } catch (FindException e) {
                           log.log(Level.WARNING, "Service lookup failed, service gone?  " + sn.getEntityHeader().getOid());
                       } catch (RemoteException e) {
                           log.log(Level.WARNING, "Remote service error", e);
                       }
                   }
               }
               RoutingAssertionTreeNode rn = (RoutingAssertionTreeNode)node;
               ((RoutingAssertion)rn.asAssertion()).setProtectedServiceUrl(url);
           }

           return node;
       }



    /**
     * By default, the leaf node never accepts a node.
     *
     * @param node the node to accept
     * @return always false
     */
    public boolean accept(AbstractTreeNode node) {
        return node instanceof PolicyTemplateNode || getParent() != null;
    }
}
