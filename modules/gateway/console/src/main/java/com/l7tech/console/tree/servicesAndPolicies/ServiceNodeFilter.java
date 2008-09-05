package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.gateway.common.service.ServiceHeader;

import javax.swing.tree.TreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 27, 2008
 * Time: 11:56:25 AM
 * Will check the user object to ensure its a ServiceHeader and not just accept based on the nodes type
 */
public class ServiceNodeFilter implements NodeFilter {
    public boolean accept(TreeNode node) {

        if(node instanceof FolderNode) return true;

        //All children of a ServiceNode are WsdlTreeNode
        if(node instanceof WsdlTreeNode) return true;
        
        if(!(node instanceof ServiceNode)) return false;

        ServiceNode serviceNode = (ServiceNode) node;
        Object userObj = serviceNode.getUserObject();

        if(!(userObj instanceof ServiceHeader)) return false;
        return true;
    }
}
