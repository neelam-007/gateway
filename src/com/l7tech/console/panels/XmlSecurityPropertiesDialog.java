package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

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

    public XmlSecurityPropertiesDialog(JFrame owner, boolean modal) {
        super(owner, modal);
    }

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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(0, 10, 0, 10), -1, -1));
        final JLabel _2;
        _2 = new JLabel();
        _2.setText("Signature and encryption properties");
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _3;
        _3 = new JPanel();
        _3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JRadioButton _4;
        _4 = new JRadioButton();
        entireMessage = _4;
        _4.setText("Entire message");
        _3.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _5;
        _5 = new JRadioButton();
        messageParts = _5;
        _5.setText("Message parts");
        _3.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JLabel _6;
        _6 = new JLabel();
        _6.setText("Signature and ecryption scope");
        _3.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _7;
        _7 = new com.intellij.uiDesigner.core.Spacer();
        _3.add(_7, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _8;
        _8 = new JPanel();
        _8.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_8, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JTable _9;
        _9 = new JTable();
        securedItemsTable = _9;
        _8.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 6, 6, null, new Dimension(150, 50), null));
        final JTree _10;
        _10 = new JTree();
        wsdlMessagesTree = _10;
        _10.setShowsRootHandles(false);
        _8.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 6, 6, null, new Dimension(150, 50), null));
        final JPanel _11;
        _11 = new JPanel();
        _11.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(5, 0, 5, 0), -1, -1));
        _1.add(_11, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _12;
        _12 = new JButton();
        okButton = _12;
        _12.setText("OK");
        _11.add(_12, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _11.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _14;
        _14 = new JButton();
        helpButton = _14;
        _14.setText("Help");
        _11.add(_14, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _15;
        _15 = new JButton();
        cancelButton = _15;
        _15.setText("Cancel");
        _11.add(_15, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _16;
        _16 = new com.intellij.uiDesigner.core.Spacer();
        _11.add(_16, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    }
}
