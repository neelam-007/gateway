package com.l7tech.console.tree.wsdl;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.tree.AbstractTreeNode;

import java.util.List;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see com.l7tech.console.tree.AbstractTreeNode
 */
public abstract class WsdlTreeNode extends AbstractTreeNode {
    protected Options wsdlOptions;

    protected WsdlTreeNode(Object userObject, Options options) {
        super(userObject);
        wsdlOptions = options;
    }

    /**
     * creates a <CODE>TreeNode</CODE> with the given Wsdl
     * as a user object.
     * 
     * @param wsdl the tree node this node points to
     */
    public static WsdlTreeNode newInstance(Wsdl wsdl) {
        if (wsdl == null) {
            throw new IllegalArgumentException();
        }
        return newInstance(wsdl, new Options());
    }

    /**
     * creates a <CODE>TreeNode</CODE> with the given Wsdl
     * as a user object, with rendering options.
     *
     * @param wsdl the tree node this node points to
     */
    public static WsdlTreeNode newInstance(Wsdl wsdl, Options options) {
        if (wsdl == null || options == null) {
            throw new IllegalArgumentException();
        }
        return new DefinitionsTreeNode(wsdl.getDefinition(), options);
    }

    /**
     * Returns true if the receiver is a leaf.
     * 
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return this.toString();
    }

    /**
     * the <code>Options</code> class customizes the WSDL rendering
     */
    public static class Options {
        public boolean isShowMessageParts() {
            return showMessageParts;
        }

        public void setShowMessageParts() {
            this.showMessageParts = true;
        }

        public boolean isShowInputMessages() {
            return showInputMessages;
        }

        public void setShowInputMessages() {
            this.showInputMessages = true;
        }

        public boolean isShowOutputMessages() {
            return showOutputMessages;
        }

        public void setShowOutputMessages() {
            this.showOutputMessages = true;
        }

        private boolean showInputMessages = false;
        private boolean showOutputMessages = false;
        private boolean showMessageParts = false;
    }


    public interface FolderLister {
        List list();
    }
}
