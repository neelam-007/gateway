package com.l7tech.console.tree.policy;


import com.l7tech.console.action.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Cookie;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.wsdl.WSDLException;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Class <code>AssertionTreeNode</code> is the base superclass for the
 * asserttion tree policy nodes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class AssertionTreeNode extends AbstractTreeNode {
    private List validatorMessages = new ArrayList();
    /**
     * package private constructor accepting the asseriton
     * this node represents.

     * @param assertion that this node represents
     */
    AssertionTreeNode(Assertion assertion) {
        super(assertion);
        this.setAllowsChildren(false);
    }

    /**
     * @return the assertion this node represents
     */
    public final Assertion asAssertion() {
        return (Assertion)getUserObject();
    }

    /**
     * @return the node name that is displayed
     */
    abstract public String getName();

    /**
     * Set the validator messages for this node.
     * @param messages the messages
     */
    public void setValidatorMessages(List messages) {
        this.validatorMessages = new ArrayList();
        if (messages !=null) {
            validatorMessages.addAll(messages);
        }
    }

    /**
     * Set the validator messages for this node.
     * @return tthe list of validator messages
     */
    public List getValidatorMessages() {
        return this.validatorMessages;
    }

    /**
     * Tooltip override, if there is an validator message, shows as tooltip.
     *
     * @return the tooltip text or null
     */
    public String getTooltipText() {
        if (validatorMessages.isEmpty()) {
            return super.getTooltipText();
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<html><strong>");
        Iterator it = this.validatorMessages.iterator();
        boolean first = true;
        for (;it.hasNext();) {
            if (!first) {
                sb.append("<br>");
            }
            first = false;
            PolicyValidatorResult.Message pm = (PolicyValidatorResult.Message)it.next();
            sb.append(pm.getMessage());
        }
        sb.append("</strong></html>");
        return sb.toString();
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.addAll(Arrays.asList(super.getActions()));
        CompositeAssertionTreeNode ca =
          (CompositeAssertionTreeNode)((this instanceof CompositeAssertionTreeNode) ? this : this.getParent());

        Action a = new AddAllAssertionAction(ca);
        list.add(a);
        a = new AddOneOrMoreAssertionAction(ca);
        list.add(a);

        Action da = new DeleteAssertionAction(this);
        da.setEnabled(canDelete());
        list.add(da);

/*      commented out as it is currently NOT supported
        Action ea = new ExplainAssertionAction();
        ea.setEnabled(canDelete());
        list.add(ea);
        */

        Action mu = new AssertionMoveUpAction(this);
        mu.setEnabled(canMoveUp());
        list.add(mu);

        Action md = new AssertionMoveDownAction(this);
        md.setEnabled(canMoveDown());
        list.add(md);

    /*
        Action sp = new SavePolicyAction(this);
        list.add(sp);

        Action vp = new ValidatePolicyAction((AssertionTreeNode)getRoot());
        list.add(vp);
    */

        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Can the node move up in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveUp() {
        return
          getParent() != null && getPreviousSibling() != null;
    }

    /**
     * Can the node move down in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveDown() {
        return getNextSibling() != null;
    }

    /**
     * Swap the position of this node with the target
     * node.
     */
    public void swap(AssertionTreeNode target) {
        final JTree tree = ComponentRegistry.getInstance().getPolicyTree();
        final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)this.getParent();

        int indexThis = parent.getIndex(this);
        model.removeNodeFromParent(this);
        int indexThat = parent.getIndex(target);
        model.removeNodeFromParent(target);

        model.insertNodeInto(this, parent, indexThat);
        model.insertNodeInto(target, parent, indexThis);

        Runnable runnable = new Runnable() {
            public void run() {
                TreeNode[] path =
                  ((DefaultMutableTreeNode)AssertionTreeNode.this).getPath();
                if (path != null) {
                    tree.setSelectionPath(new TreePath(path));
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     * @return true if the node has been received by this assertion node
     *         false otherwise
     */
    public boolean receive(AbstractTreeNode node) {
        if (node instanceof PolicyTemplateNode) {
            assignPolicyTemplate((PolicyTemplateNode)node);
            return true;
        }
        return false;
    }

    /**
     * assign the policy template.
     * todo: find a better place for this
     *
     * @param pn
     */
    private void assignPolicyTemplate(PolicyTemplateNode pn) {
        ServiceNode sn = getServiceNodeCookie();
        if (sn == null)
            throw new IllegalArgumentException("No edited service specified");
        ByteArrayOutputStream bo = null;
        InputStream fin = null;
        try {

            String oldPolicyXml = sn.getPublishedService().getPolicyXml();
            bo = new ByteArrayOutputStream();
            fin = new FileInputStream(pn.getFile());

            byte[] buff = new byte[1024];
            int nread = -1;
            while ((nread = fin.read(buff)) != -1) {
                bo.write(buff, 0, nread);
            }
            sn.getPublishedService().setPolicyXml(bo.toString());
            sn.firePropertyChange(this, "policy", oldPolicyXml, sn.getPublishedService().getPolicyXml());
        } catch (Exception e) {

        } finally {
            if (bo != null) {
                try {
                    bo.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing stream", e);
                }
            }
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing stream", e);
                }
            }
        }
    }


    /**
     * @return the published service cookie or null if not found
     */
    protected ServiceNode getServiceNodeCookie() {
        return getServiceNode(this);
    }

    /**
     * Get the service that this assertion tree node belongs to
     *
     * @param node the assertion tree node
     * @return the published service or null if not found
     */
    public static ServiceNode getServiceNode(AssertionTreeNode node) {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext();) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof ServiceNode) return (ServiceNode)value;
        }
        return null;
    }


    /**
     * some nodes need to be additionally prepared
     * @param node to prapre
     * @return the preapred node
     */
    protected final AssertionTreeNode prepareNode(AssertionTreeNode node) {
        if (node instanceof RoutingAssertionTreeNode) {
            String url = "Unable to determine the service url. Please edit";
            for (Iterator i = ((AbstractTreeNode)this.getRoot()).cookies(); i.hasNext();) {
                Object value = ((Cookie)i.next()).getValue();
                if (value instanceof ServiceNode) {
                    ServiceNode sn = (ServiceNode)value;
                    try {
                        url = sn.getPublishedService().parsedWsdl().getServiceURI();
                    } catch (WSDLException e) {
                        logger.log(Level.WARNING, "Wsdl error", e);
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Service lookup failed, service gone?  " + sn.getEntityHeader().getOid());
                    } catch (RemoteException e) {
                        logger.log(Level.WARNING, "Remote service error", e);
                    }
                }
            }
            RoutingAssertionTreeNode rn = (RoutingAssertionTreeNode)node;
            ((RoutingAssertion)rn.asAssertion()).setProtectedServiceUrl(url);
        }
        return node;
    }

    /**
     * Does the assertion node accepts the abstract tree node
     *
     * @param node the node to accept
     * @return true if the node can be accepted, false otherwise
     */
    public abstract boolean accept(AbstractTreeNode node);
}

