package com.l7tech.console.tree.policy;


import com.l7tech.console.action.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Cookie;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class AssertionTreeNode extends AbstractTreeNode {
    private static final Logger log =
      Logger.getLogger(AssertionTreeNode.class.getName());

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
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.addAll(Arrays.asList(super.getActions()));

        Action sp = new SavePolicyAction(this);
        //sp.setEnabled(false);
        list.add(sp);

        Action da = new DeleteAssertionAction(this);
        da.setEnabled(canDelete());
        list.add(da);

        Action ea = new ExplainAssertionAction();
        ea.setEnabled(canDelete());
        list.add(ea);

        Action vp = new ValidatePolicyAction((AssertionTreeNode)getRoot());
        list.add(vp);

        Action mu = new AssertionMoveUpAction(this);
        mu.setEnabled(canMoveUp());
        list.add(mu);

        Action md = new AssertionMoveDownAction(this);
        md.setEnabled(canMoveDown());
        list.add(md);

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

        TreeNode[] path =
          ((DefaultMutableTreeNode)AssertionTreeNode.this).getPath();
        if (path != null) {
            tree.setSelectionPath(new TreePath(path));
        }
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
            if (!confirmApplyPolicyTemplate(sn)) return;

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
                    log.log(Level.WARNING, "Error closing stream", e);
                }
            }
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Error closing stream", e);
                }
            }
        }
    }

    private boolean confirmApplyPolicyTemplate(ServiceNode sn) {
        if ((JOptionPane.showConfirmDialog(
          ComponentRegistry.getInstance().getMainWindow(),
          "<html><center><b>Are you sure you wish to apply policy template ?<br> (This will permanently overwrite the " +
          "policy for '" + sn.getName() + "')</b></center></html>",
          "Overwrite Service policy",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }
        return true;

    }

    /**
     * @return the published service cookie or null if not founds
     */
    private ServiceNode getServiceNodeCookie() {
        for (Iterator i = ((AbstractTreeNode)getRoot()).cookies(); i.hasNext();) {
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

