package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 23-Mar-2009
 * Time: 6:11:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderDialog extends AssertionPropertiesEditorSupport<XacmlRequestBuilderAssertion> {
    private JPanel mainPanel;
    private JScrollPane treePane;
    private JPanel nodeSettingsPanel;
    private JButton cancelButton;
    private JButton okButton;

    private JTree tree;
    private DefaultTreeModel treeModel;
    private JPopupMenu popupMenu;

    private XacmlRequestBuilderAssertion assertion;
    private boolean confirmed = false;

    private XacmlRequestBuilderNodePanel lastNodePanel = null;
    private XacmlRequestBuilderNodePanel lastErrorNodePanel = null;
    private static final String XACML_REQUEST_ELEMENT = "Request";

    public XacmlRequestBuilderDialog(Window owner, XacmlRequestBuilderAssertion a) {
        super(owner, "XACML Request Builder");
        createPopupMenu();
        initComponents();

        assertion = a;
    }

    public void initComponents() {
        treeModel = new DefaultTreeModel(buildInitialTree());
        tree = new JTree(treeModel);
        treePane.setViewportView(tree);
        treePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent evt) {
                if(lastNodePanel != null && lastNodePanel == lastErrorNodePanel) {
                    lastErrorNodePanel = null;
                    return;
                }

                TreePath path = evt.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();

                if(lastNodePanel != null && !lastNodePanel.handleDispose()) {
                    lastErrorNodePanel = lastNodePanel;
                    tree.setSelectionPath(evt.getOldLeadSelectionPath());
                    return;
                } else {
                    lastErrorNodePanel = null;
                }

                nodeSettingsPanel.removeAll();
                // The request node does not have any properties
                Object item = node.getUserObject();

                if(item instanceof XacmlRequestBuilderAssertion.Subject) {
                    XacmlRequestBuilderSubjectPanel panel = new XacmlRequestBuilderSubjectPanel((XacmlRequestBuilderAssertion.Subject)item);
                    lastNodePanel = panel;
                    nodeSettingsPanel.add(panel.getPanel());
                } else if(item instanceof XacmlRequestBuilderAssertion.Attribute) {
                    XacmlRequestBuilderAttributePanel panel = new XacmlRequestBuilderAttributePanel((XacmlRequestBuilderAssertion.Attribute)item, assertion.getXacmlVersion());
                    lastNodePanel = panel;
                    nodeSettingsPanel.add(panel.getPanel());
                } else if(item instanceof XacmlRequestBuilderAssertion.GenericXmlElementHolder) {
                    //this currently covers both AttributeValue and ResourceContent
                    XacmlRequestBuilderXmlContentPanel panel = new XacmlRequestBuilderXmlContentPanel(
                            (XacmlRequestBuilderAssertion.GenericXmlElementHolder)item,
                            assertion.getXacmlVersion(),
                            XacmlRequestBuilderDialog.this);
                    lastNodePanel = panel;
                    nodeSettingsPanel.add(panel.getPanel());
                } else if(item instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                    XacmlRequestBuilderXpathMultiAttrPanel panel =
                            new XacmlRequestBuilderXpathMultiAttrPanel(
                                    (XacmlRequestBuilderAssertion.MultipleAttributeConfig)item,
                                    assertion.getXacmlVersion(),
                                    XacmlRequestBuilderDialog.this);
                    lastNodePanel = panel;
                    nodeSettingsPanel.add(panel.getPanel());
                }else if(item instanceof String && XACML_REQUEST_ELEMENT.equals(node.getUserObject())) {
                    XacmlRequestBuilderRequestPanel panel = new XacmlRequestBuilderRequestPanel(assertion);
                    lastNodePanel = panel;
                    nodeSettingsPanel.add(panel.getPanel());
                } else {
                    lastNodePanel = null;
                }
                //todo [Donal] what about ResourceContent?
                nodeSettingsPanel.repaint();
                
                XacmlRequestBuilderDialog.this.pack();
            }
        });

        tree.addMouseListener(new MouseListener() {
            public void mouseEntered(MouseEvent evt) {
                //
            }

            public void mouseReleased(MouseEvent evt) {
                if(popupMenu.isPopupTrigger(evt)) {
                    handlePopupTrigger(evt);
                }
            }

            public void mouseExited(MouseEvent evt) {
                //
            }

            public void mouseClicked(MouseEvent evt) {
                if(popupMenu.isPopupTrigger(evt)) {
                    popupMenu.show((Component)evt.getSource(), evt.getX(), evt.getY());
                }
            }

            public void mousePressed(MouseEvent evt) {
                //
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(lastNodePanel != null && !lastNodePanel.handleDispose()) {
                    return;
                }

                java.util.List<String> messages = new ArrayList<String>();
                if(!isTreeValid(messages)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<html><body><h3>Correct the following errors.</h3><ul>");
                    for(String message : messages) {
                        sb.append("<li>");
                        sb.append(message);
                        sb.append("</li>");
                    }
                    sb.append("</ul></body></html>");

                    JOptionPane.showMessageDialog(XacmlRequestBuilderDialog.this, sb.toString(), "Message Errors", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                getData(assertion);
                confirmed = true;
                XacmlRequestBuilderDialog.this.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderDialog.this.dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] {okButton, cancelButton});

        setContentPane(mainPanel);
        pack();
    }

    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
    }

    private void handlePopupTrigger(MouseEvent evt) {
        if(!popupMenu.isPopupTrigger(evt)) {
            return;
        }

        TreePath path = tree.getPathForLocation(evt.getX(), evt.getY());
        DefaultMutableTreeNode node = null;
        try {
            node = (DefaultMutableTreeNode)path.getLastPathComponent();
        } catch(Exception e) {
            // Ignore
        }

        if(node != null) {
            popupMenu.removeAll();

            if(node.getUserObject() instanceof String && XACML_REQUEST_ELEMENT.equals(node.getUserObject())) {
                addRequestMenuItems(node);
            } else {
                Object nodeSettings = node.getUserObject();
                if(nodeSettings instanceof XacmlRequestBuilderAssertion.Subject || nodeSettings instanceof XacmlRequestBuilderAssertion.Action)
                {
                    addAttributeHolderMenuItems(node);
                } else if(nodeSettings instanceof XacmlRequestBuilderAssertion.Environment) {
                    addEnvironmentMenuItems(node);
                } else if(nodeSettings instanceof XacmlRequestBuilderAssertion.Resource) {
                    addResourceMenuItems(node);
                } else if(nodeSettings instanceof XacmlRequestBuilderAssertion.Attribute) {
                    addAttributeMenuItems(node);
                } else if(nodeSettings instanceof XacmlRequestBuilderAssertion.AttributeValue) {
                    addValueMenuItems(node);
                } else if(nodeSettings instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                    addXpathMultiAttrMenuItems(node);
                } else if(nodeSettings instanceof XacmlRequestBuilderAssertion.ResourceContent) {
                    addResourceContentMenuItems(node);
                } else {
                    return;
                }
            }

            popupMenu.show((Component)evt.getSource(), evt.getX(), evt.getY());
        }
    }

    private void addRequestMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem("Add Subject");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = 0;
                for(int i = 0;i < node.getChildCount();i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
                    XacmlRequestBuilderAssertion.RequestChildElement nodeObj = (XacmlRequestBuilderAssertion.RequestChildElement)child.getUserObject();

                    if(nodeObj instanceof XacmlRequestBuilderAssertion.Resource || nodeObj instanceof XacmlRequestBuilderAssertion.Action) {
                        index = i;
                        break;
                    }
                }

                treeModel.insertNodeInto(buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Subject()), node, index);
            }
        });
        popupMenu.add(item);

        if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0) {
            item = new JMenuItem("Add Resource");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    int index = 0;
                    for(int i = 0;i < node.getChildCount();i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);

                        if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.Action) {
                            index = i;
                            break;
                        }
                    }

                    treeModel.insertNodeInto(buildResourceNode(new XacmlRequestBuilderAssertion.Resource()), node, index);
                }
            });
            popupMenu.add(item);
        }

        boolean haveEnvironment = false;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)treeModel.getRoot();
        for(int i = 0;i < root.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);

            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.Environment) {
                haveEnvironment = true;
                break;
            }
        }

        if(!haveEnvironment) {
            item = new JMenuItem("Add Environment");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    treeModel.insertNodeInto(buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Environment()), node, node.getChildCount());
                }
            });
            popupMenu.add(item);
        }
    }

    private void addAttributeHolderMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem("Add Attribute");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = node.getChildCount();
                for(int i = 0;i < node.getChildCount();i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);

                    if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                        index = i;
                        break;
                    }
                }

                treeModel.insertNodeInto(buildAttributeNode(new XacmlRequestBuilderAssertion.Attribute()), node, index);
            }
        });
        popupMenu.add(item);

        item = new JMenuItem("Add XPath Multiple Attributes");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                treeModel.insertNodeInto(buildXpathMultiAttrNode(new XacmlRequestBuilderAssertion.MultipleAttributeConfig()), node, node.getChildCount());
            }
        });
        popupMenu.add(item);

        if(node.getUserObject() instanceof XacmlRequestBuilderAssertion.Subject || node.getUserObject() instanceof XacmlRequestBuilderAssertion.Resource) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode)treeModel.getRoot();
            int typeCount = 0;
            Class type = node.getUserObject().getClass();
            for(int i = 0;i < root.getChildCount();i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);
                if(type.equals(child.getUserObject().getClass())) {
                    typeCount++;
                }
            }

            if(typeCount > 1) {
                popupMenu.add(new JPopupMenu.Separator());
                String name = node.getUserObject().toString();

                item = new JMenuItem("Remove " + name);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        treeModel.removeNodeFromParent(node);
                    }
                });
                popupMenu.add(item);
            }
        }
    }

    private void addResourceMenuItems(final DefaultMutableTreeNode node) {
        boolean haveResourceContent = false;
        for(int i = 0;i < node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.ResourceContent) {
                haveResourceContent = true;
            }
        }

        if(!haveResourceContent) {
            JMenuItem item = new JMenuItem("Add ResourceContent");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    int index = node.getChildCount();
                    for(int i = 0;i < node.getChildCount();i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);

                        if(!(child.getUserObject() instanceof XacmlRequestBuilderAssertion.ResourceContent)) {
                            index = i;
                            break;
                        }
                    }

                    treeModel.insertNodeInto(buildResourceContentNode(new XacmlRequestBuilderAssertion.ResourceContent()), node, index);
                }
            });
            popupMenu.add(item);
        }
        
        addAttributeHolderMenuItems(node);
    }

    private void addEnvironmentMenuItems(final DefaultMutableTreeNode node) {
        addAttributeHolderMenuItems(node);

        if(assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            popupMenu.add(new JPopupMenu.Separator());
            JMenuItem item = new JMenuItem("Remove Environment");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    treeModel.removeNodeFromParent(node);
                }
            });
            popupMenu.add(item);
        }
    }

    private void addAttributeMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem("Add Attribute Value");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                treeModel.insertNodeInto(buildValueNode(new XacmlRequestBuilderAssertion.AttributeValue()), node, node.getChildCount());
            }
        });
        popupMenu.add(item);

        popupMenu.add(new JPopupMenu.Separator());
        item = new JMenuItem("Remove Attribute");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private void addValueMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem("Remove Attribute Value");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private void addXpathMultiAttrMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem("Remove XPath Multiple Attributes");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private void addResourceContentMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem("Remove Resource Content");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private TreeNode buildInitialTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(XACML_REQUEST_ELEMENT, true);
        root.add(buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Subject()));
        root.add(buildResourceNode(new XacmlRequestBuilderAssertion.Resource()));
        root.add(buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Action()));
        root.add(buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Environment()));

        return root;
    }

    private DefaultMutableTreeNode buildValueNode(XacmlRequestBuilderAssertion.AttributeValue attributeValue) {
        return new DefaultMutableTreeNode(attributeValue, false);
    }

    private DefaultMutableTreeNode buildAttributeNode(XacmlRequestBuilderAssertion.Attribute attribute) {
        DefaultMutableTreeNode attributeNode = new DefaultMutableTreeNode(attribute, true);
        for(XacmlRequestBuilderAssertion.AttributeValue attributeValue : attribute.getValues()) {
            attributeNode.add(buildValueNode(attributeValue));
        }

        return attributeNode;
    }

    private DefaultMutableTreeNode buildXpathMultiAttrNode(XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig) {
        return new DefaultMutableTreeNode(multipleAttributeConfig, false);
    }

    private DefaultMutableTreeNode buildResourceContentNode(XacmlRequestBuilderAssertion.ResourceContent resourceContent) {
        return new DefaultMutableTreeNode(resourceContent, false);
    }

    private DefaultMutableTreeNode buildAttributeHolderNode(XacmlRequestBuilderAssertion.RequestChildElement requestChildElement) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(requestChildElement, true);
        for(XacmlRequestBuilderAssertion.AttributeTreeNodeTag attributeTreeNode : requestChildElement.getAttributes()) {
            if(attributeTreeNode instanceof XacmlRequestBuilderAssertion.Attribute) {
                node.add(buildAttributeNode((XacmlRequestBuilderAssertion.Attribute) attributeTreeNode));
            } else if(attributeTreeNode instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                node.add(buildXpathMultiAttrNode((XacmlRequestBuilderAssertion.MultipleAttributeConfig) attributeTreeNode));
            }
        }

        return node;
    }

    private DefaultMutableTreeNode buildResourceNode(XacmlRequestBuilderAssertion.Resource resource) {
        DefaultMutableTreeNode resourceNode = buildAttributeHolderNode(resource);
        if(resource.getResourceContent() != null) {
            resourceNode.insert(buildResourceContentNode(resource.getResourceContent()), 0);
        }

        return resourceNode;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(XacmlRequestBuilderAssertion assertion) {
        this.assertion = (XacmlRequestBuilderAssertion)assertion.clone();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(XACML_REQUEST_ELEMENT, true);
        for(XacmlRequestBuilderAssertion.Subject subject : this.assertion.getSubjects()) {
            root.add(buildAttributeHolderNode(subject));
        }
        for(XacmlRequestBuilderAssertion.Resource resource : this.assertion.getResources()) {
            root.add(buildResourceNode(resource));
        }
        root.add(buildAttributeHolderNode(this.assertion.getAction()));
        if(this.assertion.getEnvironment() != null) {
            root.add(buildAttributeHolderNode(this.assertion.getEnvironment()));
        }

        treeModel = new DefaultTreeModel(root);
        tree.setModel(treeModel);
    }

    private XacmlRequestBuilderAssertion.Attribute extractAttribute(DefaultMutableTreeNode node) {
        if(!(node.getUserObject() instanceof XacmlRequestBuilderAssertion.Attribute)) {
            return null;
        }

        XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute)node.getUserObject();
        attribute.getValues().clear();
        for(int i = 0;i < node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.AttributeValue) {
                attribute.getValues().add((XacmlRequestBuilderAssertion.AttributeValue)child.getUserObject());
            }
        }

        return attribute;
    }

    private XacmlRequestBuilderAssertion.RequestChildElement extractAttributeHolder(DefaultMutableTreeNode node) {
        if(!(node.getUserObject() instanceof XacmlRequestBuilderAssertion.RequestChildElement)) {
            return null;
        }

        XacmlRequestBuilderAssertion.RequestChildElement attributeHolder = (XacmlRequestBuilderAssertion.RequestChildElement)node.getUserObject();
        attributeHolder.getAttributes().clear();
        for(int i = 0;i < node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.Attribute) {
                attributeHolder.getAttributes().add(extractAttribute(child));
            } else if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                attributeHolder.getAttributes().add((XacmlRequestBuilderAssertion.MultipleAttributeConfig)child.getUserObject());
            }
        }

        return attributeHolder;
    }

    private XacmlRequestBuilderAssertion.Resource extractResource(DefaultMutableTreeNode node) {
        if(!(node.getUserObject() instanceof XacmlRequestBuilderAssertion.Resource)) {
            return null;
        }

        XacmlRequestBuilderAssertion.Resource resource = (XacmlRequestBuilderAssertion.Resource)extractAttributeHolder(node);
        for(int i = 0;i < node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.ResourceContent) {
                resource.setResourceContent((XacmlRequestBuilderAssertion.ResourceContent)child.getUserObject());
            }
        }

        return resource;
    }

    public XacmlRequestBuilderAssertion getData(XacmlRequestBuilderAssertion assertion) {
        assertion.getSubjects().clear();
        assertion.getResources().clear();
        assertion.setAction(null);
        assertion.setEnvironment(null);

        assertion.setXacmlVersion(this.assertion.getXacmlVersion());
        assertion.setSoapEncapsulation(this.assertion.getSoapEncapsulation());
        assertion.setOutputMessageDestination(this.assertion.getOutputMessageDestination());
        if(this.assertion.getOutputMessageDestination() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            assertion.setOutputMessageVariableName(this.assertion.getOutputMessageVariableName());
        } else {
            assertion.setOutputMessageVariableName(null);
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode)treeModel.getRoot();
        for(int i = 0;i < root.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);
            Object obj = child.getUserObject();

            if(obj instanceof XacmlRequestBuilderAssertion.Subject) {
                assertion.getSubjects().add((XacmlRequestBuilderAssertion.Subject)extractAttributeHolder(child));
            } else if(obj instanceof XacmlRequestBuilderAssertion.Resource) {
                assertion.getResources().add(extractResource(child));
            } else if(obj instanceof XacmlRequestBuilderAssertion.Action) {
                assertion.setAction((XacmlRequestBuilderAssertion.Action)extractAttributeHolder(child));
            } else if(obj instanceof XacmlRequestBuilderAssertion.Environment) {
                assertion.setEnvironment((XacmlRequestBuilderAssertion.Environment)extractAttributeHolder(child));
            }
        }
        
        return assertion;
    }

    private boolean isTreeValid(java.util.List<String> messages) {
        int resourceCount = 0;
        int environmentCount = 0;

        DefaultMutableTreeNode root = (DefaultMutableTreeNode)treeModel.getRoot();
        for(int i = 0;i < root.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);
            Object obj = child.getUserObject();

            if(obj instanceof XacmlRequestBuilderAssertion.Resource) {
                resourceCount++;
            } else if(obj instanceof XacmlRequestBuilderAssertion.Environment) {
                environmentCount++;
            }
        }

        if(resourceCount == 0) {
            if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0) {
                messages.add("There must be at least one Resource element");
            } else {
                messages.add("The Resource element is mandatory");
            }
        } else if(assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0 && resourceCount > 1) {
            messages.add("There cannot be more than one Resource element");
        }

        if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0 && environmentCount < 1) {
            messages.add("The Environment element is mandatory");
        }

        boolean haveAttributeValueErrors = false;
        for(int i = 0;i < root.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);

            for(int j = 0;j < child.getChildCount();j++) {
                DefaultMutableTreeNode grandChild = (DefaultMutableTreeNode)child.getChildAt(j);

                if(grandChild.getUserObject() instanceof XacmlRequestBuilderAssertion.Attribute) {
                    if(containsAttributeValueErrors(grandChild)) {
                        haveAttributeValueErrors = true;
                        break;
                    }
                }
            }
        }

        if(haveAttributeValueErrors) {
            if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V1_0) {
                messages.add("There cannot be more than one AttributeValue element for each Attribute element");
            } else if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V1_1) {
                messages.add("The must be one and only one AttributeValue element for each Attribute element");
            } else if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0) {
                messages.add("There must be at least one AttributeValue element for each Attribute element");
            }
        }

        return messages.size() == 0;
    }

    private boolean containsAttributeValueErrors(DefaultMutableTreeNode attributeNode) {
        int valueCount = 0;
        for(int i = 0;i < attributeNode.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)attributeNode.getChildAt(i);
            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.AttributeValue) {
                valueCount++;
            }
        }

        switch(assertion.getXacmlVersion()) {
            case V1_0:
                if(valueCount > 1) {
                    return true;
                }
                break;
            case V1_1:
                if(valueCount != 1) {
                    return true;
                }
                break;
            case V2_0:
                if(valueCount < 1) {
                    return true;
                }
                break;
        }

        return false;
    }

    public static void main(String[] args) {
        Frame f = new JFrame();
        f.setVisible(true);
        XacmlRequestBuilderDialog d = new XacmlRequestBuilderDialog(f, null);
        d.setVisible(true);
        d.dispose();
        f.dispose();
    }
}
