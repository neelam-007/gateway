package com.l7tech.console.tree.wsdl;


import javax.wsdl.*;
import java.util.*;

/**
 * Class DefinitionsTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefinitionsTreeNode extends WsdlTreeNode {
    private final Definition definition;
    private static final int ELEMENT_TYPE_MESSAGE = 1;
    private static final int ELEMENT_TYPE_BINDING = 2;
    private static final int ELEMENT_TYPE_PORT_TYPE = 3;
    private static final int ELEMENT_TYPE_SERVICE = 4;



    DefinitionsTreeNode(Definition def, Options options) {
        super(null, options);
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
                Map messages = getElements(definition, ELEMENT_TYPE_MESSAGE);
                for (Iterator i = messages.values().iterator(); i.hasNext();) {
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
                Map portTypes = getElements(definition, ELEMENT_TYPE_PORT_TYPE);

                for (Iterator i = portTypes.values().iterator(); i.hasNext();) {
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
                Map bindings = getElements(definition, ELEMENT_TYPE_BINDING);

                for (Iterator i = bindings.values().iterator(); i.hasNext();) {
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
                Map services = getElements(definition, ELEMENT_TYPE_SERVICE);
                for (Iterator i = services.values().iterator(); i.hasNext();) {
                    list.add(new ServiceTreeNode((Service)i.next(), wsdlOptions));
                }
                return list;
            }
        }, wsdlOptions);

        insert(svc, index++);
    }

    /**
     * Retrieve the all elements of the specified type
     * @param def  the wsdl defintion
     * @param elementType  the element type (ELEMENT_TYPE_MESSAGE, ELEMENT_TYPE_BINDING, & ELEMENT_TYPE_PORT_TYPE)
     * @return Map the list of elements of the specified type. Never null.
     */
    private Map getElements(Definition def, int elementType) {
        Map allElements = new HashMap();
        switch(elementType) {
            case ELEMENT_TYPE_MESSAGE:
                allElements = def.getMessages();

                break;
            case ELEMENT_TYPE_BINDING:
                allElements = def.getBindings();

                break;
            case ELEMENT_TYPE_PORT_TYPE:
                allElements = def.getPortTypes();
                break;
            case ELEMENT_TYPE_SERVICE:
                allElements = def.getServices();
                break;
            default:
                return new HashMap();
        }
        Import imp = null;
        if(def.getImports().size() > 0) {
            Iterator itr = def.getImports().keySet().iterator();
            while(itr.hasNext()) {
                Object importDef = itr.next();
                Vector importList = (Vector) def.getImports().get(importDef);
                for (int k = 0; k < importList.size(); k++) {
                    imp = (Import) importList.elementAt(k);
                    Map elements = null;
                    switch(elementType) {
                        case ELEMENT_TYPE_MESSAGE:
                            elements = imp.getDefinition().getMessages();

                            break;
                        case ELEMENT_TYPE_BINDING:
                            elements = imp.getDefinition().getBindings();

                            break;
                        case ELEMENT_TYPE_PORT_TYPE:
                            elements = imp.getDefinition().getPortTypes();
                            break;
                        case ELEMENT_TYPE_SERVICE:
                            elements = imp.getDefinition().getServices();
                            break;
                        default:
                            return new HashMap();
                    }
                    if(elements.size() > 0) {
                        allElements.putAll(elements);
                    }
                }
            }
        }

        if(imp != null && imp.getDefinition() != null) {
            Map moreElements = getElements(imp.getDefinition(), elementType);
            if(moreElements.size() > 0) {
                allElements.putAll(moreElements);
            }
        }
        return allElements;
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return definition.getTargetNamespace();
    }
}

