package com.l7tech.console.tree.policy;

import com.l7tech.console.event.PolicyChangeVetoException;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyWillChangeListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.tree.policy.advice.Advices;
import com.l7tech.console.tree.policy.advice.PolicyValidatorAdvice;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Set;


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
            if (sn != null) {
                service = sn.getPublishedService();
            }
            Assertion policy = assertionTreeNode.asAssertion();
            PolicyTreeModelChange pc = new PolicyTreeModelChange(policy,
              event,
              service,
              this, (AssertionTreeNode)newChild,
              (AssertionTreeNode)parent, index);
            pc.advices = Advices.getAdvices(a);
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
     * Invoke this to insert newChild at location index in parents children with
     * advice support.
     */
    private void insertNodeIntoAdvised(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        super.insertNodeInto(newChild, parent, index);
    }

     /**
     * Invoke this to move (after remove node) to insert newChild at location index in parents
      * children without advice support. Will trigger only the validation advice
     */
    void moveNodeInto(AssertionTreeNode newChild, DefaultMutableTreeNode parent, int index) {
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
            if (sn != null) {
                service = sn.getPublishedService();
            }
            Assertion policy = assertionTreeNode.asAssertion();
            PolicyTreeModelChange pc = new PolicyTreeModelChange(policy,
              event,
              service,
              this, (AssertionTreeNode)newChild,
              (AssertionTreeNode)parent, index);
            pc.advices = new Advice[]{new PolicyValidatorAdvice()};
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
     * Advices invocation. Supports invoking the policy change advice chain.
     */
    private static class PolicyTreeModelChange extends PolicyChange {
        private PolicyTreeModel treeModel = null;
        private AssertionTreeNode newChild;
        private AssertionTreeNode parent;
        private int childLocation;
        protected Advice[] advices = null;
        protected int adviceIndex = 0;

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
            if (advices == null || this.adviceIndex == advices.length) {
                treeModel.insertNodeIntoAdvised(newChild, parent, childLocation);
            } else
                this.advices[this.adviceIndex++].proceed(this);
        }
    }


    /**
     * Advices invocation. Supports invoking the policy change advice chain.
     */
    private static class PolicyTreeModelRemoveChange extends PolicyTreeModelChange {
        private AssertionTreeNode childNode;

        /**
         * Construct the policy change that will invoke advices for a given policy
         * change.
         * 
         * @param policy  the policy that will be changed
         * @param event   the policy event describing the change
         * @param service the service this policy belongs to
         */
        public PolicyTreeModelRemoveChange(Assertion policy, PolicyEvent event, PublishedService service,
                                           PolicyTreeModel treeModel, AssertionTreeNode childNode,
                                           AssertionTreeNode parent, int childLocation) {
            super(policy, event, service, treeModel, childNode, parent, childLocation);
            this.childNode = childNode;
        }

        public void proceed() throws PolicyException {
            if (advices == null || this.adviceIndex == advices.length) {
            } else
                this.advices[this.adviceIndex++].proceed(this);
        }
    }


    /**
     * Message this to remove node from its parent.
     * Overriden to support the policy will change lsteners.
     */
    public void removeNodeFromParent(final MutableTreeNode node) {
        checkArgumentIsAssertionTreeNode(node);
        final AssertionTreeNode parent = (AssertionTreeNode)node.getParent();
        if (parent == null)
            throw new IllegalArgumentException("node does not have a parent.");

        final int[] childIndex = new int[1];
        childIndex[0] = parent.getIndex(node);
        Assertion p = parent.asAssertion();
        Assertion a = ((AssertionTreeNode)node).asAssertion();
        final PolicyEvent event = new PolicyEvent(this, new AssertionPath(p.getPath()), childIndex, new Assertion[]{a});
        try {
            fireWillRemoveListeners(event);
            super.removeNodeFromParent(node);
            AssertionTreeNode assertionTreeNode = (AssertionTreeNode)getRoot();
            ServiceNode sn = assertionTreeNode.getServiceNodeCookie();
            PublishedService service = null;
            if (sn != null) {
                try {
                    service = sn.getPublishedService();
                } catch (FindException e) {
                    throw new RuntimeException(e);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            Assertion policy = assertionTreeNode.asAssertion();
            PolicyTreeModelRemoveChange pc =
              new PolicyTreeModelRemoveChange(policy,
                event,
                service,
                PolicyTreeModel.this, (AssertionTreeNode)node,
                (AssertionTreeNode)parent, childIndex[0]);
            pc.advices = new Advice[]{new PolicyValidatorAdvice()};
            try {
                pc.proceed();
            } catch (PolicyException e) {
                throw new RuntimeException(e);
            }
        } catch (PolicyChangeVetoException e) {
            throw new RuntimeException(e);
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

