package com.l7tech.console.tree.wsdl;


import com.l7tech.common.xml.Wsdl;

import javax.wsdl.*;
import java.util.*;

/**
 * Class DefinitionsTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefinitionsTreeNode extends WsdlTreeNode {
    private final Definition definition;
    private Wsdl wsdl;

    DefinitionsTreeNode(Definition def, Options options) {
        super(null, options);
        this.definition = def;
        this.wsdl = new Wsdl(definition);
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
                Collection messages = wsdl.getMessages();
                for (Iterator i = messages.iterator(); i.hasNext();) {
                    list.add(new MessageTreeNode((Message)i.next()));
                }
                return list;
            }
        }, wsdlOptions);

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

                Collection portTypes = wsdl.getPortTypes();

                for (Iterator i = portTypes.iterator(); i.hasNext();) {
                    list.add(new PortTypeTreeNode((PortType)i.next(), wsdlOptions));
                }

                return list;
            }
        }, wsdlOptions);

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
                Collection bindings = wsdl.getBindings();

                for (Iterator i = bindings.iterator(); i.hasNext();) {
                    list.add(new BindingTreeNode((Binding)i.next(), wsdlOptions));
                }
                return list;
            }
        }, wsdlOptions);

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
                Collection services = wsdl.getServices();
                for (Iterator i = services.iterator(); i.hasNext();) {
                    list.add(new ServiceTreeNode((Service)i.next(), wsdlOptions));
                }
                return list;
            }
        }, wsdlOptions);

        insert(svc, index++);
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return definition.getTargetNamespace();
    }
}

