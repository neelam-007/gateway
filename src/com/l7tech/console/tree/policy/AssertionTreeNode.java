package com.l7tech.console.tree.policy;


import com.l7tech.console.action.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.PolicyImporter;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Class <code>AssertionTreeNode</code> is the base superclass for the
 * asserttion tree policy nodes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class AssertionTreeNode extends AbstractTreeNode {
    private List validatorMessages = new ArrayList();
    private List viewValidatorMessages = null;

    /**
     * package private constructor accepting the asseriton
     * this node represents.
     *
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
     *
     * @param messages the messages
     */
    public void setValidatorMessages(Collection messages) {
        this.validatorMessages = new ArrayList();
        if (messages != null) {
            validatorMessages.addAll(messages);
        }
        viewValidatorMessages = null;
    }

    /**
     * Get the full collection of validator messages.
     *
     * @return the collection of validator messages
     */
    public Collection getAllValidatorMessages() {
        if (validatorMessages != null) {
            return validatorMessages;
        }
        return Collections.EMPTY_LIST;
    }


    /**
     * Get the validator messages for this node. Returns the messages
     * depending on the view. For the identity view only the messages
     * that rlate to the set of paths that are associated with the view
     * will be returned.
     *
     * @return the list of validator messages
     */
    public List getValidatorMessages() {
        if (viewValidatorMessages != null) {
            return viewValidatorMessages;
        }

        if (!PolicyTree.isIdentityView(this)) {
            viewValidatorMessages = this.validatorMessages;
        } else {
            //select only the path messages
            List pathMessages = new ArrayList();
            TreeNode[] path = getPath();
            if (path.length >= 2) {
                IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[1];
                for (Iterator it = validatorMessages.iterator(); it.hasNext();) {
                    PolicyValidatorResult.Message message = (PolicyValidatorResult.Message)it.next();
                    if (in.contains(message.getAssertionPathOrder())) {
                        pathMessages.add(message);
                    }
                }
            }
            viewValidatorMessages = pathMessages;
        }
        return viewValidatorMessages;
    }

    /**
     * Tooltip override, if there is an validator message, shows as tooltip.
     *
     * @return the tooltip text or null
     */
    public String getTooltipText() {
        List messages = getValidatorMessages();
        if (messages.isEmpty()) {
            return super.getTooltipText();
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<html><strong> There are {0}<br>");
        Iterator it = messages.iterator();
        boolean first = true;
        boolean hasWarnings = false;
        boolean hasErrors = false;
        for (; it.hasNext();) {
            if (!first) {
                sb.append("<br>");
            }
            first = false;
            PolicyValidatorResult.Message pm = (PolicyValidatorResult.Message)it.next();
            if (pm instanceof PolicyValidatorResult.Error) {
                hasErrors = true;
            } else if ((pm instanceof PolicyValidatorResult.Warning)) {
                hasWarnings = true;
            }
            sb.append("<i>" + pm.getMessage() + "</i>");
        }
        sb.append("</strong></html>");
        String format = sb.toString();
        String msg = "warnings and errors, the policy might be invalid";
        if (hasWarnings && !hasErrors) {
            msg = "warnings, the policy might be invalid";
        } else if (!hasWarnings && hasErrors) {
            msg = "errors, the policy might be invalid";
        }
        return MessageFormat.format(format, new Object[]{msg});
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
        int position = (this instanceof CompositeAssertionTreeNode) ? 0 : this.getParent().getIndex(this) + 1;
        Action a = new AddAllAssertionAction(ca, position);
        list.add(a);
        a = new AddOneOrMoreAssertionAction(ca, position);
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
        final JTree tree = TopComponents.getInstance().getPolicyTree();
        final PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)this.getParent();

        int indexThis = parent.getIndex(this);
        model.removeNodeFromParent(this);
        int indexThat = parent.getIndex(target);
        model.removeNodeFromParent(target);

        model.moveNodeInto(this, parent, indexThat);
        model.moveNodeInto(target, parent, indexThis);

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
        try {
            Assertion newRoot = PolicyImporter.importPolicy(pn.getFile());
            // for some reason, the PublishedService class does not allow to set a policy
            // directly, it must be set through the XML
            if (newRoot != null) {
                String oldPolicyXml = sn.getPublishedService().getPolicyXml();
                sn.getPublishedService().setPolicyXml(WspWriter.getPolicyXml(newRoot));
                sn.firePropertyChange(this, "policy", oldPolicyXml, sn.getPublishedService().getPolicyXml());
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Could not import the policy", e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Could not import the policy", e);
        } catch (InvalidPolicyStreamException e) {
            logger.log(Level.WARNING, "Could not import the policy", e);
        }
    }


    /**
     * @return the published service cookie or null if not found
     */
    protected ServiceNode getServiceNodeCookie() {
        return getServiceNode(this);
    }

    public PublishedService getService() throws RemoteException, FindException {
        ServiceNode sn = getServiceNodeCookie();
        return sn.getPublishedService();
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
     * Does the assertion node accepts the abstract tree node
     *
     * @param node the node to accept
     * @return true if the node can be accepted, false otherwise
     */
    public abstract boolean accept(AbstractTreeNode node);
}

