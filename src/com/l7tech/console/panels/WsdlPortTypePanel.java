package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.wsdl.Definition;
import java.awt.*;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class WsdlPortTypePanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField portTypeNameField;
    private JTable operationsTable;
    private JScrollPane operationsTableScrollPane;
    private DefaultTableModel operationsModel;
    private JButton addOperationButton;
    private JButton removeOperatonButton;
    private Definition definition;

    public WsdlPortTypePanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        operationsModel = new DefaultTableModel(
          new String[]{
              "Name",
              "Request message",
              "Response message"
          },
          0
        );
        portTypeNameField.setText("NewPortType");
        operationsTable.setModel(operationsModel);
        operationsTableScrollPane.getViewport().setBackground(operationsTable.getBackground());
        removeOperatonButton.setEnabled(false);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Port Type";
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Port Type/Operations";
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

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
            if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type "+settings.getClass());
        }
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * This is a noop version that subclasses implement.
     *
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
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
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 6, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Name");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _4;
        _4 = new JTextField();
        portTypeNameField = _4;
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 2, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _5;
        _5 = new JLabel();
        _5.setText("Port Type");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JScrollPane _6;
        _6 = new JScrollPane();
        operationsTableScrollPane = _6;
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 3, 0, 3, 7, 7, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTable _7;
        _7 = new JTable();
        operationsTable = _7;
        _6.setViewportView(_7);
        JPanel _8;
        _8 = new JPanel();
        _8.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(8, 4, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _9;
        _9 = new JButton();
        removeOperatonButton = _9;
        _9.setText("Remove");
        _8.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _10;
        _10 = new JButton();
        addOperationButton = _10;
        _10.setText("Add");
        _10.setLabel("Add");
        _8.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _8.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JLabel _14;
        _14 = new JLabel();
        _14.setText("Operations");
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
    }


}
