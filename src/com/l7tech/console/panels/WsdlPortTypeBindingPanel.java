package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class WsdlPortTypeBindingPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField portTypeBindingNameField;
    private JLabel portTypeName;
    private JTable bindingOperationsTable;
    private JTextField portTypeBindingTransportField; // http://schemas.xmlsoap.org/soap/http
    private JComboBox portTypeBindingStyle;

    public WsdlPortTypeBindingPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Port Type Binding";
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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(11, 4, new Insets(10, 5, 5, 5), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Name");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _4;
        _4 = new JTextField();
        portTypeBindingNameField = _4;
        _4.setMargin(new Insets(0, 0, 0, 0));
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JTable _5;
        _5 = new JTable();
        bindingOperationsTable = _5;
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1, 0, 3, 6, 6, new Dimension(-1, -1), new Dimension(150, 50), new Dimension(-1, -1)));
        JLabel _6;
        _6 = new JLabel();
        _6.setText("Port Type Binding");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _7;
        _7 = new JLabel();
        _7.setText("Operations");
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _8;
        _8 = new JLabel();
        _8.setText("Style");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _9;
        _9 = new JLabel();
        portTypeName = _9;
        _9.setText("<Port Type Name>");
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JComboBox _10;
        _10 = new JComboBox();
        portTypeBindingStyle = _10;
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 8, 0, 2, 0, new Dimension(100, -1), new Dimension(100, -1), new Dimension(-1, -1)));
        JLabel _11;
        _11 = new JLabel();
        _11.setText("Transport");
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _12;
        _12 = new JTextField();
        portTypeBindingTransportField = _12;
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
    }


}
