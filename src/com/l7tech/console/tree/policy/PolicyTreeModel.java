package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.util.Set;


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
        return new PolicyTreeModel(new IdentityViewsRootNode(paths, root));
    }
}

