package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class WsdlOperationPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField operationNameField;
    private JTable messagesTable;
    private JComboBox operationType;
    private JComboBox requestMessageCombo;
    private JComboBox responseMessageCombo;

    public WsdlOperationPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Messages";
    }

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean isValid() {
        return true;
    }

    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /** generated code, do not edit or call this method manually !!! */
    private void $$$setupUI$$$() {
        JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(13, 4, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Name");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _4;
        _4 = new JTextField();
        operationNameField = _4;
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JLabel _5;
        _5 = new JLabel();
        _5.setText("Operation");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        _6.setBorder(BorderFactory.createTitledBorder("Request"));
        JComboBox _7;
        _7 = new JComboBox();
        requestMessageCombo = _7;
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 1, 1, 2, 0, new Dimension(-1, -1), new Dimension(80, -1), new Dimension(-1, -1)));
        JTable _8;
        _8 = new JTable();
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 6, 6, new Dimension(-1, -1), new Dimension(100, 50), new Dimension(-1, -1)));
        JLabel _9;
        _9 = new JLabel();
        _9.setText("Messages");
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _10;
        _10 = new JLabel();
        _10.setText("Type");
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JPanel _11;
        _11 = new JPanel();
        _11.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        _11.setBorder(BorderFactory.createTitledBorder("Response"));
        JComboBox _12;
        _12 = new JComboBox();
        responseMessageCombo = _12;
        _11.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 1, 1, 2, 0, new Dimension(80, -1), new Dimension(80, -1), new Dimension(-1, -1)));
        JTable _13;
        _13 = new JTable();
        _11.add(_13, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 6, 6, new Dimension(-1, -1), new Dimension(100, 50), new Dimension(-1, -1)));
        JComboBox _14;
        _14 = new JComboBox();
        operationType = _14;
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 8, 0, 2, 0, new Dimension(100, -1), new Dimension(100, -1), new Dimension(-1, -1)));
    }


}
