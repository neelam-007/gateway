package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class WsdlServicePanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JPanel namePanel;
    private JLabel nameLabel;
    private JTextField nameField;
    private JTextField portNameField;
    private JTextField portAddressField;


    public WsdlServicePanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Service";
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
        namePanel = _2;
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(10, 4, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _3;
        _3 = new JTextField();
        nameField = _3;
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JLabel _4;
        _4 = new JLabel();
        _4.setText("Name");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 1, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _5;
        _5 = new JLabel();
        _5.setText("Port");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 1, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _6;
        _6 = new JTextField();
        portNameField = _6;
        _6.setText("");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JLabel _7;
        _7 = new JLabel();
        _7.setText("Service");
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(100, -1), new Dimension(100, -1), new Dimension(-1, -1)));
        JLabel _8;
        _8 = new JLabel();
        _8.setText("Address");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _9;
        _9 = new JTextField();
        portAddressField = _9;
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JLabel _10;
        _10 = new JLabel();
        _10.setText("<Port Binding>");
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, 0, 2, 1, 6, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
    }


}
