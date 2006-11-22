package com.l7tech.console.panels;

import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.service.SampleMessage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Used to edit {@link com.l7tech.service.SampleMessage}s.
 */
public class SampleMessageDialog extends JDialog {
    private final SampleMessage message;
    private final boolean allowOperationChange;
    private boolean ok = false;

    private JTextField operationNameField;
    private JPanel xmlEditorPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField nameField;
    private XMLContainer xmlContainer;

    private static final String TITLE = "Sample Message";

    public SampleMessageDialog(Dialog owner, SampleMessage message, boolean allowOperationChange) throws HeadlessException {
        super(owner, TITLE, true);
        this.message = message;
        this.allowOperationChange = allowOperationChange;
        init();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    public SampleMessageDialog(Frame owner, SampleMessage message, boolean allowOperationChange) throws HeadlessException {
        super(owner, TITLE, true);
        this.message = message;
        this.allowOperationChange = allowOperationChange;
        init();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void init() {
        xmlContainer = new XMLContainer(true);
        xmlContainer.setErrorPanelAvailable(false);
        xmlContainer.setStatusBarAvailable(false);
        xmlContainer.getUIAccessibility().setTreeAvailable(false);
        xmlContainer.getUIAccessibility().setToolBarAvailable(false);

        xmlEditorPanel.setLayout(new BorderLayout());
        xmlEditorPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

        operationNameField.setEnabled(allowOperationChange);
        String opname = message.getOperationName();
        if (!allowOperationChange && (opname == null || opname.length() == 0)) {
            opname = "<all operations>";
        }
        operationNameField.setText(opname);

        xmlContainer.getAccessibility().setText(message.getXml());
        nameField.setText(message.getName());

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                message.setName(nameField.getText());
                if (allowOperationChange) message.setOperationName(operationNameField.getText());
                String text = xmlContainer.getAccessibility().getText();
                try {
                    XmlUtil.stringToDocument(text);
                    message.setXml(text);
                    ok = true;
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            SampleMessageDialog.this,
                            "The XML is not valid: " + ex.toString(),
                            "Invalid XML",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                enableButtons();
            }

            public void removeUpdate(DocumentEvent e) {
                enableButtons();
            }

            public void changedUpdate(DocumentEvent e) {
                enableButtons();
            }
        };

        operationNameField.getDocument().addDocumentListener(docListener);
        xmlContainer.getDocument().addDocumentListener(docListener);
        nameField.getDocument().addDocumentListener(docListener);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        add(mainPanel);
    }

    private void enableButtons() {
        boolean ok = nameField.getText().length() > 0;
        ok = ok && xmlContainer.getAccessibility().getText().length() > 0;
        if (allowOperationChange) ok = ok && operationNameField.getText().length() > 0;
        okButton.setEnabled(ok);
    }

    public void dispose() {
        xmlContainer.dispose();
        super.dispose();
    }

    public SampleMessage getMessage() {
        return message;
    }

    public boolean isOk() {
        return ok;
    }
}
