package com.l7tech.console.tree.wsdl;

import javax.wsdl.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class DefinitionsTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefinitionsTreeNode extends WsdlTreeNode {
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
        FolderTreeNode ms = new FolderTreeNode(new FolderLister() {
            /**
             * @return a string representation of the object.
             */
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

        FolderTreeNode pt = new FolderTreeNode(new FolderLister() {
            /**
             * @return a string representation of the object.
             */
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

        FolderTreeNode bn = new FolderTreeNode(new FolderLister() {
            /**
             * @return a string representation of the object.
             */
            public String toString() {
                return "Bindings";
            }

            public List list() {
                List list = new ArrayList();
                Map bindings = definition.getBindings();
                for (Iterator i = bindings.values().iterator(); i.hasNext();) {
                    list.add(new BindingTreeNode((Binding)i.next()));
                }
                return list;
            }
        });

        insert(bn, index++);

        FolderTreeNode svc = new FolderTreeNode(new FolderLister() {
            /**
             * @return a string representation of the object.
             */
            public String toString() {
                return "Services";
            }

            public List list() {
                List list = new ArrayList();
                Map services = definition.getServices();
                for (Iterator i = services.values().iterator(); i.hasNext();) {
                    list.add(new ServiceTreeNode((Service)i.next()));
                }
                return list;
            }
        });

        insert(svc, index++);
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return definition.getTargetNamespace();
    }
}

