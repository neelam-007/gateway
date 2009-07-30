package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Collections;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 23-Mar-2009
 * Time: 6:11:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderDialog extends AssertionPropertiesEditorSupport<XacmlRequestBuilderAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle( XacmlRequestBuilderDialog.class.getName() );

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
        super(owner, resources.getString( "xacml.request.builder" ) );
        createPopupMenu();
        initComponents();

        assertion = a;
    }

    public void initComponents() {
        Utilities.setEscKeyStrokeDisposes( this );
        treeModel = new DefaultTreeModel(buildInitialTree());
        tree = new JTree(treeModel);
        tree.setExpandsSelectedPaths(true);
        treePane.setViewportView(tree);
        treePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
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

                if ( evt.getNewLeadSelectionPath() == null ) {
                    lastNodePanel = null;
                } else if(item instanceof XacmlRequestBuilderAssertion.Subject) {
                    XacmlRequestBuilderSubjectPanel panel = new XacmlRequestBuilderSubjectPanel((XacmlRequestBuilderAssertion.Subject)item);
                    lastNodePanel = panel;
                    nodeSettingsPanel.add(panel.getPanel());
                } else if(item instanceof XacmlRequestBuilderAssertion.Attribute) {
                    XacmlRequestBuilderAttributePanel panel = new XacmlRequestBuilderAttributePanel(
                            (XacmlRequestBuilderAssertion.Attribute)item,
                            assertion.getXacmlVersion(),
                            getIdOptions(((DefaultMutableTreeNode)path.getPath()[1]).getUserObject()));
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
                                    getIdOptions(((DefaultMutableTreeNode)path.getPath()[1]).getUserObject()),
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
                nodeSettingsPanel.repaint();
                mainPanel.validate();
            }
        });

        tree.addMouseListener(new MouseListener() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                //
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                if(popupMenu.isPopupTrigger(evt)) {
                    handlePopupTrigger(evt);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                //
            }

            @Override
            public void mouseClicked(MouseEvent evt) {
                if(popupMenu.isPopupTrigger(evt)) {
                    popupMenu.show((Component)evt.getSource(), evt.getX(), evt.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                //
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(lastNodePanel != null && !lastNodePanel.handleDispose()) {
                    return;
                }

                java.util.List<String> messages = new ArrayList<String>();
                if(!isTreeValid(messages)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<html><body><h3>");
                    sb.append(resources.getString( "correct.errors" ));
                    sb.append("</h3><ul>");
                    for(String message : messages) {
                        sb.append("<li>");
                        sb.append(message);
                        sb.append("</li>");
                    }
                    sb.append("</ul></body></html>");

                    JOptionPane.showMessageDialog(XacmlRequestBuilderDialog.this, sb.toString(), resources.getString( "message.errors" ), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                getData(assertion);
                confirmed = true;
                XacmlRequestBuilderDialog.this.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderDialog.this.dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] {okButton, cancelButton});

        setContentPane(mainPanel);
        pack();
    }

    private Set<String> getIdOptions( final Object object ) {
        Set<String> values = Collections.emptySet();

        if ( object instanceof XacmlRequestBuilderAssertion.Subject ) {
            values = XacmlConstants.XACML_10_SUBJECT_IDS;
        } else if ( object instanceof XacmlRequestBuilderAssertion.Action ) {
            values = XacmlConstants.XACML_10_ACTION_IDS;
        } else if ( object instanceof XacmlRequestBuilderAssertion.Resource ) {
            values = XacmlConstants.XACML_10_RESOURCE_IDS;
        } else if ( object instanceof XacmlRequestBuilderAssertion.Environment ) {
            values = XacmlConstants.XACML_10_ENVIRONMENT_IDS;    
        }

        return values;
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
        JMenuItem item = new JMenuItem( resources.getString( "add.subject" ) );
        item.addActionListener(new ActionListener() {
            @Override
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

                insertAndSelectNode(node, buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Subject()), index);
            }
        });
        popupMenu.add(item);

        if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0) {
            item = new JMenuItem( resources.getString( "add.resource" ) );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    int index = 0;
                    for(int i = 0;i < node.getChildCount();i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);

                        if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.Action) {
                            index = i;
                            break;
                        }
                    }

                    insertAndSelectNode(node, buildResourceNode(new XacmlRequestBuilderAssertion.Resource()), index);
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
            item = new JMenuItem( resources.getString( "add.environment" ) );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    insertAndSelectNode(node, buildAttributeHolderNode(new XacmlRequestBuilderAssertion.Environment()), node.getChildCount());
                }
            });
            popupMenu.add(item);
        }
    }

    private void addAttributeHolderMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem( resources.getString( "add.attribute" ) );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int index = node.getChildCount();
                for(int i = 0;i < node.getChildCount();i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);

                    if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                        index = i;
                        break;
                    }
                }

                XacmlRequestBuilderAssertion.Attribute attribute = new XacmlRequestBuilderAssertion.Attribute();
                attribute.setDataType( XacmlConstants.XACML_10_DATATYPE_STRING );
                insertAndSelectNode(node, buildAttributeNode(attribute), index);
            }
        });
        popupMenu.add(item);

        item = new JMenuItem( resources.getString( "add.xpath.multiple.attributes" ) );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderAssertion.MultipleAttributeConfig multiAttribute = new XacmlRequestBuilderAssertion.MultipleAttributeConfig();
                XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field field =
                        multiAttribute.getField( XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName.DATA_TYPE);
                if ( field != null ) {
                    field.setValue( XacmlConstants.XACML_10_DATATYPE_STRING );
                }
                insertAndSelectNode(node, buildXpathMultiAttrNode(multiAttribute), node.getChildCount());
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

                item = new JMenuItem( "Remove " + name);
                item.addActionListener(new ActionListener() {
                    @Override
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
            JMenuItem item = new JMenuItem( resources.getString( "add.resourcecontent" ) );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    int index = node.getChildCount();
                    for(int i = 0;i < node.getChildCount();i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);

                        if(!(child.getUserObject() instanceof XacmlRequestBuilderAssertion.ResourceContent)) {
                            index = i;
                            break;
                        }
                    }

                    insertAndSelectNode(node, buildResourceContentNode(new XacmlRequestBuilderAssertion.ResourceContent()), index);
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
            JMenuItem item = new JMenuItem( resources.getString( "remove.environment" ) );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    treeModel.removeNodeFromParent(node);
                }
            });
            popupMenu.add(item);
        }
    }

    private void addAttributeMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item;

        if (assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0 || countAttributeValues(node) == 0) {
            item = new JMenuItem( resources.getString( "add.attribute.value" ) );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    insertAndSelectNode(node, buildValueNode(new XacmlRequestBuilderAssertion.AttributeValue()), node.getChildCount());
                }
            });
            popupMenu.add(item);
            popupMenu.add(new JPopupMenu.Separator());
        }

        item = new JMenuItem( resources.getString( "remove.attribute" ) );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private void insertAndSelectNode(MutableTreeNode parent, DefaultMutableTreeNode child, int index) {
        treeModel.insertNodeInto(child, parent, index);
        tree.setSelectionPath(new TreePath(child.getPath()));

    }

    private void addValueMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem( resources.getString( "remove.attribute.value" ) );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private void addXpathMultiAttrMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem( resources.getString( "remove.xpath.multiple.attributes" ) );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                treeModel.removeNodeFromParent(node);
            }
        });
        popupMenu.add(item);
    }

    private void addResourceContentMenuItems(final DefaultMutableTreeNode node) {
        JMenuItem item = new JMenuItem( resources.getString( "remove.resource.content" ) );
        item.addActionListener(new ActionListener() {
            @Override
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

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(XacmlRequestBuilderAssertion assertion) {
        this.assertion = assertion.clone();

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
        tree.setSelectionInterval( 0, 0 );
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
        resource.setResourceContent(null);
        for(int i = 0;i < node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            if(child.getUserObject() instanceof XacmlRequestBuilderAssertion.ResourceContent) {
                resource.setResourceContent((XacmlRequestBuilderAssertion.ResourceContent)child.getUserObject());
            }
        }

        return resource;
    }

    @Override
    public XacmlRequestBuilderAssertion getData(XacmlRequestBuilderAssertion assertion) {
        assertion.getSubjects().clear();
        assertion.getResources().clear();
        assertion.setAction(null);
        assertion.setEnvironment(null);

        assertion.setXacmlVersion(this.assertion.getXacmlVersion());
        assertion.setSoapEncapsulation(this.assertion.getSoapEncapsulation());
        assertion.setOutputMessageDestination(this.assertion.getOutputMessageDestination());
        if(this.assertion.getOutputMessageDestination() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            assertion.setOutputMessageVariableName( VariablePrefixUtil.fixVariableName( this.assertion.getOutputMessageVariableName() ) );
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
                messages.add( resources.getString( "error.resource.missing" ) );
            } else {
                messages.add( resources.getString( "error.resource.required" ) );
            }
        } else if(assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0 && resourceCount > 1) {
            messages.add( resources.getString( "error.resource.toomany" ) );
        }

        if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0 && environmentCount < 1) {
            messages.add( resources.getString( "error.environment.required" ) );
        }

        boolean haveAttributeValueErrors = false;
        int v1ResourceIdCount = 0;
        boolean v1ResourceIdMultipleAttribute = false;
        for(int i = 0;i < root.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);

            for(int j = 0;j < child.getChildCount();j++) {
                DefaultMutableTreeNode grandChild = (DefaultMutableTreeNode)child.getChildAt(j);

                if(grandChild.getUserObject() instanceof XacmlRequestBuilderAssertion.Attribute) {
                    if(containsAttributeValueErrors(grandChild)) {
                        haveAttributeValueErrors = true;
                    }

                    if (child.getUserObject() instanceof XacmlRequestBuilderAssertion.Resource &&
                        XacmlConstants.XACML_10_RESOURCE_ID.equals(((XacmlRequestBuilderAssertion.Attribute) grandChild.getUserObject()).getId())) {
                        v1ResourceIdCount++;
                    }
                } else if (grandChild.getUserObject() instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                    if ( child.getUserObject() instanceof XacmlRequestBuilderAssertion.Resource &&
                         ((XacmlRequestBuilderAssertion.MultipleAttributeConfig)grandChild.getUserObject()).hasV1ResourceId() ) {
                        v1ResourceIdCount++;
                        v1ResourceIdMultipleAttribute = true;

                    }
                }
            }
        }

        if (v1ResourceIdCount > 1 && XacmlAssertionEnums.XacmlVersionType.V1_0 == assertion.getXacmlVersion()) {
            messages.add( resources.getString( "error.resource.multiple.id" ) );
        }

        if(haveAttributeValueErrors) {
            if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V1_0) {
                messages.add( resources.getString( "error.attributevalue.multiple" ) );
            } else if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V1_1) {
                messages.add( resources.getString( "error.attributevalue.wrongnumberof" ) );
            } else if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0) {
                messages.add( resources.getString( "error.attributevalue.required" ) );
            }
        }

        if ( messages.size() == 0 && // only warn if there are no errors
             v1ResourceIdMultipleAttribute &&
             XacmlAssertionEnums.XacmlVersionType.V1_0 == assertion.getXacmlVersion()) {
            DialogDisplayer.showMessageDialog( this, resources.getString( "warning.resource.multiple.id" ), "Validation Warning", JOptionPane.WARNING_MESSAGE, null );
        }

        return messages.size() == 0;
    }

    private boolean containsAttributeValueErrors(DefaultMutableTreeNode attributeNode) {
        int valueCount = countAttributeValues(attributeNode);

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

    private int countAttributeValues(DefaultMutableTreeNode attributeNode) {
        int count = 0;
        for (int i = 0; i < attributeNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)attributeNode.getChildAt(i);
            if (child.getUserObject() instanceof XacmlRequestBuilderAssertion.AttributeValue) {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        XacmlRequestBuilderAssertion xacmlRequestBuilderAssertion = new XacmlRequestBuilderAssertion();
        XacmlRequestBuilderDialog d = new XacmlRequestBuilderDialog(null, xacmlRequestBuilderAssertion);
        d.setData( xacmlRequestBuilderAssertion );
        d.setVisible(true);
        d.dispose();
    }
}
