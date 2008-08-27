package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.policy.PolicyHeader;
import javax.swing.tree.TreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 27, 2008
 * Time: 2:18:14 PM
 */
public class PolicyNodeFilter implements NodeFilter {

    public boolean accept(TreeNode node) {

        if(node instanceof FolderNode) return true;

        if(!(node instanceof PolicyEntityNode)) return false;

        PolicyEntityNode entityNode = (PolicyEntityNode) node;
        Object userObj = entityNode.getUserObject();

        if(!(userObj instanceof PolicyHeader)) return false;
        return true;
    }
}