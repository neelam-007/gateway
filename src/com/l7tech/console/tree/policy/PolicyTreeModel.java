package com.l7tech.console.tree.policy;

import com.l7tech.console.event.PolicyWillChangeListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyChangeVetoException;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.AssertionPath;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.EventListener;
import java.rmi.RemoteException;


/**
 * <code>PolicyTreeModel</code> is the policy assrtions tree data model.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyTreeModel extends DefaultTreeModel {
    private EventListenerList eventListenerList = new WeakEventListenerList();

    /**
     * Creates a new instance of PolicyTreeModel with root set
     * to the node represnting the root assertion.
     * 
     * @param root 
     */
    public PolicyTreeModel(Assertion root) {
        super(AssertionTreeNodeFactory.asTreeNode(root));
    }

    /**
     * Creates a new instance of the PolicyTreeModel for the Published service.
     * 
     * @param service 
     */
    public static PolicyTreeModel make(PublishedService service) {
        try {
            PolicyTreeModel model = new PolicyTreeModel(WspReader.parse(service.getPolicyXml()));
            return model;
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing the service policy - service " + service.getName(), e);
        }
    }

    /**
     * Creates a new instance of PolicyTreeModel with root set
     * to the abstract tree node. This is a protected constructor
     * that is used for models such as identity viewl.
     * 
     * @param root 
     */
    protected PolicyTreeModel(AbstractTreeNode root) {
        super(root);
    }

    /**
     * Creates a new full policy view of PolicyTreeModel for the asserton
     * tree.
     * 
     * @param root the assertion root
     */
    public static PolicyTreeModel policyModel(Assertion root) {
        return new PolicyTreeModel(root);
    }

    /**
     * Creates a new identity view of PolicyTreeModel for the asserton
     * tree.
     * 
     * @param root the assertion root
     */
    public static PolicyTreeModel identitityModel(Assertion root) {
        Set paths = IdentityPath.getPaths(root);
        return new PolicyTreeModel(new IdentityViewRootNode(paths, root));
    }

    /**
     * add the PolicyWillChangeListener
     * 
     * @param listener the PolicyWillChangeListener
     */
    public void addPolicyListener(PolicyWillChangeListener listener) {
        eventListenerList.add(PolicyWillChangeListener.class, listener);
    }

    /**
     * remove the the PolicyWillChangeListener
     * 
     * @param listener the PolicyWillChangeListener
     */
    public void removePolicyListener(PolicyWillChangeListener listener) {
        eventListenerList.remove(PolicyWillChangeListener.class, listener);
    }

    private void fireWillReceiveListeners(PolicyEvent event)
      throws PolicyChangeVetoException {
        EventListener[] listeners = eventListenerList.getListeners(PolicyWillChangeListener.class);
        for (int i = listeners.length - 1; i >= 0; i--) {
            ((PolicyWillChangeListener)listeners[i]).policyWillReceive(event);
        }
    }


    private void fireWillRemoveListeners(PolicyEvent event)
      throws PolicyChangeVetoException {
        EventListener[] listeners = eventListenerList.getListeners(PolicyWillChangeListener.class);
        for (int i = listeners.length - 1; i >= 0; i--) {
            ((PolicyWillChangeListener)listeners[i]).policyWillRemove(event);
        }
    }

    public static class IdentityNodeFilter implements NodeFilter {
        /**
         * @param node the <code>TreeNode</code> to examine
         * @return true if filter accepts the node, false otherwise
         */
        public boolean accept(TreeNode node) {
            if (node instanceof SpecificUserAssertionTreeNode ||
              node instanceof MemberOfGroupAssertionTreeNode)
                return false;

            if (node instanceof CompositeAssertionTreeNode) {
                if (((CompositeAssertionTreeNode)node).getChildCount(this) == 0)
                    return false;
            }

            TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
            IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[1];
            AssertionTreeNode an = (AssertionTreeNode)node;
            IdentityPath ip = in.getIdentityPath();
            Set paths = ip.getPaths();
            for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
                Assertion[] apath = (Assertion[])iterator.next();
                for (int i = apath.length - 1; i >= 0; i--) {
                    Assertion assertion = apath[i];
                    if (assertion.equals(an.asAssertion())) return true;
                }
            }
            return false;
        }
    }

    /**
     * Invoked this to insert newChild at location index in parents children.
     * Overriden to support the policy will change lsteners.
     */
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        checkArgumentIsAssertionTreeNode(newChild);
        checkArgumentIsAssertionTreeNode(parent);
        Assertion p = ((AssertionTreeNode)parent).asAssertion();
        Assertion a = ((AssertionTreeNode)newChild).asAssertion();
        PolicyEvent event = new PolicyEvent(this,
                                            new AssertionPath(p.getPath()),
                                            new int[]{index}, new Assertion[]{a});

        try {
            fireWillReceiveListeners(event);
            AssertionTreeNode assertionTreeNode = (AssertionTreeNode)getRoot();
            ServiceNode sn = assertionTreeNode.getServiceNodeCookie();
            PublishedService service = null;
            if (sn !=null) {
                service = sn.getPublishedService();
            }
            Assertion policy = assertionTreeNode.asAssertion();
            PolicyTreeModelChange pc = new PolicyTreeModelChange(policy,
                                                                 event,
                                                                 service,
                                                                 this, (AssertionTreeNode)newChild,
                                                                 (AssertionTreeNode)parent, index);
            pc.proceed();
        } catch (PolicyChangeVetoException e) {
            // vetoed
        } catch (PolicyException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoked this to insert newChild at location index in parents children with
     * advice support.
     */
    private void insertNodeIntoAdvised(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        super.insertNodeInto(newChild, parent, index);
    }


    /**
     * Advices invocation. Supports invoking the policy change advice chain.
     */
    private static class PolicyTreeModelChange extends PolicyChange {
        private PolicyTreeModel treeModel = null;
        private Advice[] advices = null;
        private int adviceIndex = 0;
        private AssertionTreeNode newChild;
        private AssertionTreeNode parent;
        private int childLocation;

        /**
         * Construct the policy change that will invoke advices for a given policy
         * change.
         * 
         * @param policy  the policy that will be changed
         * @param event   the policy event describing the change
         * @param service the service this policy belongs to
         */
        public PolicyTreeModelChange(Assertion policy, PolicyEvent event, PublishedService service,
                                     PolicyTreeModel treeModel, AssertionTreeNode newChild,
                                     AssertionTreeNode parent, int childLocation) {
            super(policy, event, service);
            this.treeModel = treeModel;
            this.newChild = newChild;
            this.parent = parent;
            this.childLocation = childLocation;
        }

        public void proceed() throws PolicyException {
            advices = Advices.getAdvices(newChild.asAssertion());
            if (advices == null || this.adviceIndex == advices.length) {
                treeModel.insertNodeIntoAdvised(newChild, parent, childLocation);
            } else
                this.advices[this.adviceIndex++].proceed(this);
        }
    }


    /**
     * Message this to remove node from its parent.
     * Overriden to support the policy will change lsteners.
     */
    public void removeNodeFromParent(MutableTreeNode node) {
        checkArgumentIsAssertionTreeNode(node);
        AssertionTreeNode parent = (AssertionTreeNode)node.getParent();
        if (parent == null)
            throw new IllegalArgumentException("node does not have a parent.");

        int[] childIndex = new int[1];
        childIndex[0] = parent.getIndex(node);
        Assertion p = parent.asAssertion();
        Assertion a = ((AssertionTreeNode)node).asAssertion();
        PolicyEvent event = new PolicyEvent(this,
          new AssertionPath(p.getPath()),
          childIndex, new Assertion[]{a});
        try {
            fireWillRemoveListeners(event);
            super.removeNodeFromParent(node);
        } catch (PolicyChangeVetoException e) {
            // vetoed
        }
    }

    private void checkArgumentIsAssertionTreeNode(MutableTreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        if (!(node instanceof AssertionTreeNode)) {
            throw new
              IllegalArgumentException("Assertion tree node expected, received " + node.getClass());
        }
    }

}

