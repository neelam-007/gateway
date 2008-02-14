package com.l7tech.external.assertions.multipartassembly.console;

import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.SquigglyTextField;
import com.l7tech.common.gui.FilterDocument;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.external.assertions.multipartassembly.MultipartAssemblyAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;

public class MultipartAssemblyPropertiesDialog extends JDialog implements AssertionPropertiesEditor<MultipartAssemblyAssertion> {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton rbRequest;
    private JRadioButton rbReply;
    private JTextField variablePrefixField;
    private JLabel contentTypesLabel;
    private JLabel partIdsLabel;
    private JLabel payloadsLabel;

    private boolean confirmed = false;

    public MultipartAssemblyPropertiesDialog(Frame parent) {
        super(parent, "Multipart Assembly Properties", true);
        init();
    }

    private void init() {
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        InputValidator validator = new InputValidator(this, "Multipart Assembly - Error");
        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        variablePrefixField.setDocument(new FilterDocument(40, new FilterDocument.Filter() {
            final Pattern legal = Pattern.compile("^[a-zA-Z0-9_\\-\\.]*$");
            public boolean accept(String s) {
                return legal.matcher(s).matches();
            }
        }));
        variablePrefixField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateLabels();
            }

            public void removeUpdate(DocumentEvent e) {
                updateLabels();
            }

            public void changedUpdate(DocumentEvent e) {
                updateLabels();
            }
        });
        Utilities.attachDefaultContextMenu(variablePrefixField);
        validator.constrainTextFieldToBeNonEmpty("Context Variable Name Prefix", variablePrefixField, null);
        updateLabels();
    }

    private void updateLabels() {
        String prefix = variablePrefixField.getText();
        contentTypesLabel.setText(prefix + ".contentTypes");
        partIdsLabel.setText(prefix + ".partIds");
        payloadsLabel.setText(prefix + ".payloads");
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(MultipartAssemblyAssertion assertion) {
        boolean isReq = assertion.isActOnRequest();
        rbRequest.setSelected(isReq);
        rbReply.setSelected(!isReq);
        variablePrefixField.setText(assertion.getVariablePrefix());
        updateLabels();
        confirmed = false;
    }

    public MultipartAssemblyAssertion getData(MultipartAssemblyAssertion assertion) {
        assertion.setActOnRequest(rbRequest.isSelected());
        assertion.setVariablePrefix(variablePrefixField.getText());
        return assertion;
    }

    public void setParameter(String name, Object value) {
    }

    public Object getParameter(String name) {
        return null;
    }

    private void createUIComponents() {
        variablePrefixField = new SquigglyTextField();
    }
}
