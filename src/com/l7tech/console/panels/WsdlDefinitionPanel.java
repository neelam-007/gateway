package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

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
    private JLabel panelHeader;

    public WsdlDefinitionPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));
        defaultNameSpaceField.setText(DEFAULT_NAME_SPACE);
        nameSpaceDetailsModel =
          new DefaultTableModel(new String[]{"Prefix", "Namespace"},
            0) {
              public boolean isCellEditable(int row, int column) {
                  return false;
              }
          };

        nameSpaceDetailsModel.addRow(new String[]{"xsd", XSD_NAME_SPACE});
        nameSpaceDetailsModel.addRow(new String[]{"soap", SOAP_NAME_SPACE});
        namespaceDetails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        namespaceDetails.getTableHeader().setReorderingAllowed(false);
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
                      s = s.substring(0, pos + 1);
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
        return "<html>" +
               "The root element in the WSDL document, called the \"definition\" element, contains " +
               "child elements that define a particular Web service. The \"Target Namespace\" element " +
               "distinguishes the definitions in the WSDL document from definitions in other documents. " +
               "Any additional namespace declarations facilitate the identification of the types and " +
               "elements included in the Web service." +
               "</html>";
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
     * Test whether the step is finished and it is safe to finish the wizard.
     * 
     * @return true if the panel is valid, false otherwis
     */

    public boolean canFinish() {
        return false;
    }


    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     * 
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof Definition)) {
            throw new IllegalArgumentException("expected " + Definition.class);
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

}
