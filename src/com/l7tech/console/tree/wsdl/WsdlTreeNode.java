package com.l7tech.console.tree.wsdl;

import com.l7tech.service.Wsdl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.wsdl.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @see com.l7tech.console.tree.BasicTreeNode
 */
public abstract class WsdlTreeNode extends DefaultMutableTreeNode {

    protected boolean hasLoadedChildren = false;

    protected WsdlTreeNode(MutableTreeNode parent) {
        setParent(parent);
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
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        if (!hasLoadedChildren) {
            loadChildren();
        }
        return super.getChildCount();
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
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Returns true if the receiver is a leaf.
     */
    public abstract boolean isLeaf();

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
        int index = 0;
        FolderTreeNode ms = new FolderTreeNode(
          new FolderLister() {
                    /** @return  a string representation of the object.  */
                    public String toString() {
                        return "Messages";
                    }

                    public List list() {
                        List list = new ArrayList();
                        Map messages = definition.getMessages();
                        for (Iterator i = messages.values().iterator(); i.hasNext();) {
                            list.add(new MessageTreeNode((Message)i.next()));
                        }
                        return list;
                    }
                });

        insert(ms, index++);

        FolderTreeNode pt = new FolderTreeNode(
          new FolderLister() {
                    /** @return  a string representation of the object.  */
                    public String toString() {
                        return "Port Types";
                    }

                    public List list() {
                        List list = new ArrayList();
                        Map portTypes = definition.getPortTypes();
                        for (Iterator i = portTypes.values().iterator(); i.hasNext();) {
                            list.add(new PortTypeTreeNode((PortType)i.next()));
                        }

                        return list;
                    }
                });

        insert(pt, index++);

        FolderTreeNode bn = new FolderTreeNode(
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

        insert(bn, index++);
        FolderTreeNode svc = new FolderTreeNode(
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

        insert(svc, index++);
        hasLoadedChildren = true;
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

    FolderTreeNode(FolderLister l) {
        super(null);
        this.lister = l;
    }

    protected void loadChildren() {
        int index = 0;
        for (Iterator i = lister.list().iterator(); i.hasNext();) {
            insert((MutableTreeNode)i.next(), index++);
        }
        hasLoadedChildren = true;
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

    MessageTreeNode(Message m) {
        super(null);
        this.message = m;
    }

    protected void loadChildren() {
        hasLoadedChildren = true;
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

    PortTypeTreeNode(PortType p) {
        super(null);
        this.portType = p;
    }

    protected void loadChildren() {
        int index = 0;
        for (Iterator i = portType.getOperations().iterator(); i.hasNext();) {
            insert(new OperationTreeNode((Operation)i.next()), index++);
        }
        hasLoadedChildren = true;
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

    BindingTreeNode(MutableTreeNode parent, Binding b) {
        super(parent);
        this.binding = b;
    }

    protected void loadChildren() {
        int index = 0;
        for (Iterator i = binding.getBindingOperations().iterator(); i.hasNext();) {
            insert(new BindingOperationTreeNode((BindingOperation)i.next()), index++);
        }
        hasLoadedChildren = true;
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

    OperationTreeNode(Operation o) {
        super(null);
        this.operation = o;
    }

    protected void loadChildren() {
        hasLoadedChildren = true;
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

    BindingOperationTreeNode(BindingOperation bo) {
        super(null);
        this.operation = bo;
    }

    protected void loadChildren() {
        hasLoadedChildren = true;
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

    ServiceTreeNode(MutableTreeNode parent, Service s) {
        super(null);
        this.service = s;
    }

    protected void loadChildren() {
        hasLoadedChildren = true;
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