package com.l7tech.console.tree.wsdl;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.common.xml.Wsdl;

import javax.swing.tree.MutableTreeNode;
import javax.wsdl.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see com.l7tech.console.tree.AbstractTreeNode
 */
public abstract class WsdlTreeNode extends AbstractTreeNode {

    protected WsdlTreeNode(Object userObject) {
        super(userObject);
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
        return new DefinitionsTreeNode(wsdl.getDefinition());
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

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }

    protected void loadChildren() {
        int index = 0;
        children = null;
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
                      list.add(new MessageTreeNode((Message) i.next()));
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
                      list.add(new PortTypeTreeNode((PortType) i.next()));
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
                      list.add(new BindingTreeNode((Binding) i.next()));
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
                      list.add(new ServiceTreeNode((Service) i.next()));
                  }
                  return list;
              }
          });

        insert(svc, index++);
    }

    /**
     * @return  a string representation of the object.
     */
    public String toString() {
        return definition.getTargetNamespace();
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
        children = null;
        for (Iterator i = lister.list().iterator(); i.hasNext();) {
            insert((MutableTreeNode) i.next(), index++);
        }
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
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
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }


    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/SendMail16.gif";
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
        children = null;
        for (Iterator i = portType.getOperations().iterator(); i.hasNext();) {
            insert(new OperationTreeNode((Operation) i.next()), index++);
        }
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/interface.gif";
    }


    /** @return  a string representation of the object.  */
    public String toString() {
        return portType.getQName().getLocalPart();
    }
}

class BindingTreeNode extends WsdlTreeNode {
    private Binding binding;

    BindingTreeNode(Binding b) {
        super(null);
        this.binding = b;
    }

    protected void loadChildren() {
        int index = 0;
        children = null;
        for (Iterator i = binding.getBindingOperations().iterator(); i.hasNext();) {
            insert(new BindingOperationTreeNode((BindingOperation) i.next()), index++);
        }
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Forward16.gif";
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
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Forward16.gif";
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
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Forward16.gif";
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return operation.getName();
    }

}

class ServiceTreeNode extends WsdlTreeNode {
    private Service service;

    ServiceTreeNode(Service s) {
        super(null);
        this.service = s;
    }

    protected void loadChildren() {
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/services16.png";
    }

    /** @return  a string representation of the object.  */
    public String toString() {
        return service.getQName().getLocalPart();
    }

}
