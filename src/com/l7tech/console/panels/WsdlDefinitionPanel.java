package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import java.awt.*;
import java.util.Iterator;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlDefinitionPanel extends WizardStepPanel {
    private static final String DEFAULT_NAME_SPACE = "http://schemas.xmlsoap.org/wsdl/";
    private static final String XSD_NAME_SPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String SOAP_NAME_SPACE = "http://schemas.xmlsoap.org/wsdl/soap/";
    private JPanel mainPanel;
    private JPanel namePanel;
    private JPanel targetNameSpacePanel;
    private JLabel nameLabel;
    private JTextField nameField;
    private JTextField targetNameSpaceField;
    private JTextField defaultNameSpaceField;
    private JTable namespaceDetails;
    private JScrollPane namespaceDetailsScrollPane;
    private DefaultTableModel nameSpaceDetailsModel;

    public WsdlDefinitionPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        defaultNameSpaceField.setText(DEFAULT_NAME_SPACE);
        nameSpaceDetailsModel = new DefaultTableModel(
          new String[]{
              "Prefix",
              "Namespace"
          },
          0
        );

        nameSpaceDetailsModel.addRow(new String[] {"xsd", XSD_NAME_SPACE});
        nameSpaceDetailsModel.addRow(new String[] {"soap", SOAP_NAME_SPACE});
        namespaceDetails.setModel(nameSpaceDetailsModel);
        namespaceDetailsScrollPane.getViewport().setBackground(namespaceDetails.getBackground());
        targetNameSpaceField.setText("http://tempuri.org/");
        nameField.getDocument().
          addDocumentListener(new DocumentListener() {
              public void changedUpdate(DocumentEvent e) {
                  updateNameSpaceSuffix(e.getDocument());
              }

              public void insertUpdate(DocumentEvent e) {
                  updateNameSpaceSuffix(e.getDocument());
              }

              public void removeUpdate(DocumentEvent e) {
                  updateNameSpaceSuffix(e.getDocument());
              }

              private void updateNameSpaceSuffix(Document doc) {
                  try {
                      String name = doc.getText(0, doc.getLength());
                      String s = targetNameSpaceField.getText();
                      int pos = s.lastIndexOf('/');
                      s = s.substring(0, pos+1);
                      targetNameSpaceField.setText(s + name + ".wsdl");
                  } catch (BadLocationException ex) {
                      // swallow?
                  }
              }
          });
        nameField.setText("NewService");
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Definition";
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
        if (!(settings instanceof Definition)) {
            throw new IllegalArgumentException("expected "+Definition.class);
        }
        Definition def = (Definition)settings;
        def.setTargetNamespace(targetNameSpaceField.getText());
        def.setQName(new QName(nameField.getText()));
        def.getNamespaces().clear();
        for (Iterator iterator =
          nameSpaceDetailsModel.getDataVector().iterator(); iterator.hasNext();) {
            java.util.List nsRow = (java.util.List)iterator.next();
            def.addNamespace((String)nsRow.get(0), (String)nsRow.get(1));
        }
        def.addNamespace("tns", targetNameSpaceField.getText());

        //default name space
        def.addNamespace(null, DEFAULT_NAME_SPACE);
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Definition";
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
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(12, 4, new Insets(0, 0, 0, 0), -1, -1));
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
        _5.setText("Target Namespace");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 1, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _6;
        _6 = new JTextField();
        targetNameSpaceField = _6;
        _6.setText("");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JLabel _7;
        _7 = new JLabel();
        _7.setText("Definition");
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JPanel _8;
        _8 = new JPanel();
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _9;
        _9 = new JLabel();
        _9.setText("Default Namespace");
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _10;
        _10 = new JTextField();
        defaultNameSpaceField = _10;
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JScrollPane _15;
        _15 = new JScrollPane();
        namespaceDetailsScrollPane = _15;
        _2.add(_15, new com.intellij.uiDesigner.core.GridConstraints(10, 1, 1, 2, 0, 3, 7, 7, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTable _16;
        _16 = new JTable();
        namespaceDetails = _16;
        _16.setShowVerticalLines(true);
        _16.setShowHorizontalLines(true);
        _16.setAutoCreateColumnsFromModel(true);
        _15.setViewportView(_16);
        JLabel _17;
        _17 = new JLabel();
        _17.setText("Namespace details");
        _2.add(_17, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
    }

}
