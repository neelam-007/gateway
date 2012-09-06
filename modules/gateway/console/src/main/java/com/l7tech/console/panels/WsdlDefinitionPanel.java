package com.l7tech.console.panels;

import com.l7tech.console.util.WsdlComposer;
import com.l7tech.gui.util.DialogDisplayer;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.xml.namespace.QName;
import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlDefinitionPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JTextField nameField;
    private JTextField targetNameSpaceField;
    private JTextField defaultNameSpaceField;
    private JTable namespaceDetails;
    private JScrollPane namespaceDetailsScrollPane;
    private DefaultTableModel nameSpaceDetailsModel;
    private JLabel panelHeader;
    Vector<String> namespacesModel = new Vector<String>();
    private WsdlComposer wsdlComposer;

    public WsdlDefinitionPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {

        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));
        nameSpaceDetailsModel =
          new DefaultTableModel(new String[]{"Prefix", "Namespace"},
            0) {
              public boolean isCellEditable(int row, int column) {
                  return false;
              }
          };

        namespaceDetails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        namespaceDetails.getTableHeader().setReorderingAllowed(false);
        namespaceDetails.setModel(nameSpaceDetailsModel);
        namespaceDetailsScrollPane.getViewport().setBackground(namespaceDetails.getBackground());
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
                      Object o = targetNameSpaceField.getText();
                      if (o != null) {
                          String s = (String) o;
                          int pos = s.lastIndexOf('/');
                          s = s.substring(0, pos + 1);
                          s += name + ".wsdl";
                      }
                  } catch (BadLocationException ex) {
                      // swallow?
                  }
              }
          });
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

    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof WsdlComposer)) {
            throw new IllegalArgumentException("expected " + WsdlComposer.class);
        }
        wsdlComposer = (WsdlComposer) settings;

        QName qn = wsdlComposer.getQName();

        nameField.setText(qn == null?"":qn.getLocalPart());

        String targetNamespace = wsdlComposer.getTargetNamespace();
        if (StringUtils.isEmpty(targetNamespace))
            targetNamespace = "";

        targetNameSpaceField.setText(targetNamespace);

        // setup namespaces
        nameSpaceDetailsModel.getDataVector().clear();
        for (Iterator nsIter=wsdlComposer.getNamespaces().entrySet().iterator(); nsIter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) nsIter.next();

            if ("tns".equals(entry.getKey()))
                continue;

            if ("".equals(entry.getKey()) || entry.getKey()==null) {
                defaultNameSpaceField.setText((String)entry.getValue());
            } else {
                nameSpaceDetailsModel.addRow(new String[]{(String)entry.getKey(), (String)entry.getValue()});
            }
        }
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
        if (!(settings instanceof WsdlComposer)) {
            throw new IllegalArgumentException("expected " + WsdlComposer.class);
        }
        wsdlComposer = (WsdlComposer) settings;

        String ns = targetNameSpaceField.getText();

        wsdlComposer.setTargetNamespace(ns);
        wsdlComposer.setQName(new QName(nameField.getText()));
        wsdlComposer.getNamespaces().clear();
        for (Iterator iterator =
          nameSpaceDetailsModel.getDataVector().iterator(); iterator.hasNext();) {
            java.util.List nsRow = (java.util.List)iterator.next();
            wsdlComposer.addNamespace((String)nsRow.get(0), (String)nsRow.get(1));
        }
        wsdlComposer.addNamespace("tns", wsdlComposer.getTargetNamespace());

        //default name space
        wsdlComposer.addNamespace(null, defaultNameSpaceField.getText());
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Definition";
    }

    @Override
    public boolean onNextButton() {
        if(targetNameSpaceField.getText().trim().isEmpty()){
            DialogDisplayer.display(new JOptionPane("Target Namespace can not be empty."), this, "Error", null);
            return false;
        }
        return super.onNextButton();
    }

}
