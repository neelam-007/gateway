package com.l7tech.console.tree;

import com.l7tech.service.Wsdl;
import org.apache.log4j.Category;

import javax.swing.tree.TreeNode;
import javax.wsdl.*;
import java.util.*;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @see BasicTreeNode
 */
public abstract class WsdlTreeNode implements TreeNode {
    private TreeNode parent;
    protected List children = new ArrayList(0);
    private boolean hasLoadedChildren = false;

    protected WsdlTreeNode(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * creates a <CODE>TreeNode</CODE> with the given Wsdl
     * as a user object.
     *
     * @param wsdl the tree node this node points to
     */
    public static TreeNode newInstance(Wsdl wsdl) {
        if (wsdl == null) {
            throw new IllegalArgumentException();
        }
        return new DefinitionsTreeNode(wsdl.getDefinition());
    }

    /**
     * Returns the child <code>TreeNode</code> at index
     * <code>childIndex</code>.
     */
    public TreeNode getChildAt(int childIndex) {
        return (TreeNode)children.get(childIndex);
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        if (!hasLoadedChildren) {
            loadChildren();
            hasLoadedChildren = true;
        }
        return children.size();

    }


    public boolean isFolder() {
        return this instanceof FolderTreeNode;
    }

    public boolean isMessage() {
        return this instanceof MessageTreeNode;
    }

    public boolean isService() {
        return this instanceof ServiceTreeNode;
    }

    public boolean isOperation() {
        return this instanceof OperationTreeNode;
    }

    public boolean isBinding() {
        return this instanceof BindingTreeNode;
    }

    public boolean isBindingOperation() {
        return this instanceof BindingOperationTreeNode;
    }

    public boolean isPortType() {
        return this instanceof PortTypeTreeNode;
    }

    protected abstract void loadChildren();


    /**
     * Returns the parent <code>TreeNode</code> of the receiver.
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Returns the index of <code>node</code> in the receivers children.
     * If the receiver does not contain <code>node</code>, -1 will be
     * returned.
     */
    public int getIndex(TreeNode node) {
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).equals(node)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Returns true if the receiver is a leaf.
     */
    public abstract boolean isLeaf();

    /**
     * Returns the children of the receiver as an <code>Enumeration</code>.
     */
    public Enumeration children() {
        return Collections.enumeration(children);
    }

    public interface FolderLister {
        List list();
    }
}

class DefinitionsTreeNode extends WsdlTreeNode {
    private final Definition definition;

    DefinitionsTreeNode(Definition def) {
        super(null);
        this.definition = def;
    }

    protected void loadChildren() {
        FolderTreeNode ms = new FolderTreeNode(this,
                new FolderLister() {
                    /** @return  a string representation of the object.  */
                    public String toString() {
                        return "Messages";
                    }

                    public List list() {
                        List list = new ArrayList();
                        Map messages = definition.getMessages();
                        for (Iterator i = messages.values().iterator(); i.hasNext();) {
                            list.add(new MessageTreeNode(null, (Message)i.next()));
                        }
                        return list;
                    }
                });

        children.add(ms);

        FolderTreeNode pt = new FolderTreeNode(this,
                new FolderLister() {
                    /** @return  a string representation of the object.  */
                    public String toString() {
                        return "Port Types";
                    }

                    public List list() {
                        List list = new ArrayList();
                        Map portTypes = definition.getPortTypes();
                        for (Iterator i = portTypes.values().iterator(); i.hasNext();) {
                            list.add(new PortTypeTreeNode(null, (PortType)i.next()));
                        }

                        return list;
                    }
                });

        children.add(pt);

        FolderTreeNode bn = new FolderTreeNode(this,
                new FolderLister() {
                    /** @return  a string representation of the object.  */
                    public String toString() {
                        return "Bindings";
                    }

                    public List list() {
                        List list = new ArrayList();
                        Map bindings = definition.getBindings();
                        for (Iterator i = bindings.values().iterator(); i.hasNext();) {
                            list.add(new BindingTreeNode(null, (Binding)i.next()));
                        }
                        return list;
                    }
                });

        children.add(bn);
        FolderTreeNode svc = new FolderTreeNode(this,
                new FolderLister() {
                    /** @return  a string representation of the object.  */
                    public String toString() {
                        return "Services";
                    }

                    public List list() {
                        List list = new ArrayList();


                        Map services = definition.getServices();
                        for (Iterator i = services.values().iterator(); i.hasNext();) {
                            list.add(new ServiceTreeNode(null, (Service)i.next()));
                        }
                        return list;
                    }
                });

        children.add(svc);


    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return false;
    }

    /**
     * @return  a string representation of the object.
     */
    public String toString() {
        return definition.getQName().getNamespaceURI();
    }
}

class FolderTreeNode extends WsdlTreeNode {
    private FolderLister lister;

    FolderTreeNode(TreeNode parent, FolderLister l) {
        super(parent);
        this.lister = l;
    }

    protected void loadChildren() {
        children.addAll(lister.list());
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return false;
    }

    /**
     * @return  a string representation of the object.
     */
    public String toString() {
        return lister.toString();
    }
}


class MessageTreeNode extends WsdlTreeNode {
    private Message message;

    MessageTreeNode(TreeNode parent, Message m) {
        super(parent);
        this.message = m;
    }

    protected void loadChildren() {
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /**
     * @return  a string representation of the object.
     */
    public String toString() {
        return message.getQName().getLocalPart();
    }
}

class PortTypeTreeNode extends WsdlTreeNode {
    private PortType portType;

    PortTypeTreeNode(TreeNode parent, PortType p) {
        super(parent);
        this.portType = p;
    }

    protected void loadChildren() {
        for (Iterator i = portType.getOperations().iterator(); i.hasNext();) {
            children.add(new OperationTreeNode(this, (Operation)i.next()));
        }
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return false;
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return portType.getQName().getLocalPart();
    }
}

class BindingTreeNode extends WsdlTreeNode {
    private Binding binding;

    BindingTreeNode(TreeNode parent, Binding b) {
        super(parent);
        this.binding = b;
    }

    protected void loadChildren() {
        for (Iterator i = binding.getBindingOperations().iterator(); i.hasNext();) {
            children.add(new BindingOperationTreeNode(this, (BindingOperation)i.next()));
        }
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return false;
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return binding.getQName().getLocalPart();
    }

}

class OperationTreeNode extends WsdlTreeNode {
    private Operation operation;

    OperationTreeNode(TreeNode parent, Operation o) {
        super(parent);
        this.operation = o;
    }

    protected void loadChildren() {
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return operation.getName();
    }

}

class BindingOperationTreeNode extends WsdlTreeNode {
    private BindingOperation operation;

    BindingOperationTreeNode(TreeNode parent, BindingOperation bo) {
        super(parent);
        this.operation = bo;
    }

    protected void loadChildren() {
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return operation.getName();
    }

}

class ServiceTreeNode extends WsdlTreeNode {
    private Service service;

    ServiceTreeNode(TreeNode parent, Service s) {
        super(parent);
        this.service = s;
    }

    protected void loadChildren() {
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return service.getQName().getLocalPart();
    }

}