package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.util.Set;
import java.util.Iterator;


/**
 * <code>PolicyTreeModel</code> is the policy assrtions tree data model.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyTreeModel extends DefaultTreeModel {
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
            // TODO: FIXME Emil!
            throw new IllegalArgumentException("Policy was unparseable");
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
     * Creates a new identity view of PolicyTreeModel for the asserton
     * tree.
     * 
     * @param root the assertion root
     */
    public static PolicyTreeModel identitityModel(Assertion root) {
        Set paths = IdentityPath.getPaths(root);
        return new PolicyTreeModel(new IdentityViewRootNode(paths, root));
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

}

