package com.l7tech.console.panels;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.XmlSecurityTreeNode;
import com.l7tech.console.tree.wsdl.BindingOperationTreeNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertion;

import javax.swing.*;
import javax.swing.tree.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XmlSecurityPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JRadioButton entireMessage;
    private JRadioButton messageParts;
    private JTable securedItemsTable;
    private JTree wsdlMessagesTree;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private XmlSecurityTreeNode node;
    private XmlSecurityAssertion xmlSecAssertion;
    private ServiceNode serviceNode;

    public XmlSecurityPropertiesDialog(JFrame owner, boolean modal, XmlSecurityTreeNode n) {
        super(owner, modal);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        node = n;
        setTitle("XML security properties");

        xmlSecAssertion = (XmlSecurityAssertion)node.asAssertion();
        serviceNode = AssertionTreeNode.getServiceNode(node);
        if (serviceNode == null) {
            throw new IllegalStateException("Unable to determine the service node for " + xmlSecAssertion);
        }

        initialize();
    }

    private void initialize() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(entireMessage);
        bg.add(messageParts);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                XmlSecurityPropertiesDialog.this.dispose();
            }
        });

        try {
            final Wsdl wsdl = serviceNode.getPublishedService().parsedWsdl();
            final MutableTreeNode root = new DefaultMutableTreeNode();
            final DefaultTreeModel treeModel = new DefaultTreeModel(root);
            Collection collection = wsdl.getBindings();
            for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
                Binding b = (Binding)iterator.next();
                java.util.List operations = b.getBindingOperations();
                int index = 0;
                for (Iterator itop = operations.iterator(); itop.hasNext();) {
                    BindingOperation bo = (BindingOperation)itop.next();
                    treeModel.insertNodeInto(new BindingOperationTreeNode(bo), root, index++);
                }
                break; // todo: just the first binding, review this....
            }
            wsdlMessagesTree.setModel(treeModel);
            wsdlMessagesTree.setRootVisible(false);
            wsdlMessagesTree.setShowsRootHandles(true);
            wsdlMessagesTree.setCellRenderer(wsdlTreeRenderer);
            setContentPane(mainPanel);
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private final
    TreeCellRenderer wsdlTreeRenderer = new DefaultTreeCellRenderer() {
        /**
         * Sets the value of the current tree cell to <code>value</code>.
         * 
         * @return	the <code>Component</code> that the renderer uses to draw the value
         */
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            this.setBackgroundNonSelectionColor(tree.getBackground());
            if (!(value instanceof WsdlTreeNode)) return this;

            WsdlTreeNode node = (WsdlTreeNode)value;
            setText(node.toString());
            Image image = expanded ? node.getOpenedIcon() : node.getIcon();
            Icon icon = null;
            if (image != null) {
                icon = new ImageIcon(image);
            }
            setIcon(icon);
            return this;
        }
    };

    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /**
     * generated code, do not edit or call this method manually !!!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(5, 10, 0, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(5, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JRadioButton _3;
        _3 = new JRadioButton();
        entireMessage = _3;
        _3.setText("Entire message");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _4;
        _4 = new JRadioButton();
        messageParts = _4;
        _4.setText("Message parts");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JLabel _5;
        _5 = new JLabel();
        _5.setText("Signature and ecryption scope");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _6;
        _6 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _7;
        _7 = new JPanel();
        _7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_7, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JScrollPane _8;
        _8 = new JScrollPane();
        _7.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, new Dimension(150, -1), new Dimension(150, -1), null));
        final JTree _9;
        _9 = new JTree();
        wsdlMessagesTree = _9;
        _9.setShowsRootHandles(false);
        _8.setViewportView(_9);
        final JScrollPane _10;
        _10 = new JScrollPane();
        _7.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTable _11;
        _11 = new JTable();
        securedItemsTable = _11;
        _10.setViewportView(_11);
        final JPanel _12;
        _12 = new JPanel();
        _12.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        _7.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _13;
        _13 = new JButton();
        _13.setText("Remove");
        _12.add(_13, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _14;
        _14 = new JButton();
        _14.setText("Add");
        _12.add(_14, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_15, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _16;
        _16 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_16, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 0, new Dimension(-1, 20), new Dimension(-1, 20), null));
        final JPanel _17;
        _17 = new JPanel();
        _17.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(5, 0, 5, 0), -1, -1));
        _1.add(_17, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _18;
        _18 = new JButton();
        okButton = _18;
        _18.setText("OK");
        _17.add(_18, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _19;
        _19 = new com.intellij.uiDesigner.core.Spacer();
        _17.add(_19, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _20;
        _20 = new JButton();
        helpButton = _20;
        _20.setText("Help");
        _17.add(_20, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _21;
        _21 = new JButton();
        cancelButton = _21;
        _21.setText("Cancel");
        _17.add(_21, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _22;
        _22 = new com.intellij.uiDesigner.core.Spacer();
        _17.add(_22, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _23;
        _23 = new JPanel();
        _23.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 5, 0, 0), -1, -1));
        _1.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _23.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel _24;
        _24 = new JLabel();
        _24.setText("Signature and encryption properties");
        _23.add(_24, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }


}
