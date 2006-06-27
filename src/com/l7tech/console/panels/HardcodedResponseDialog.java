package com.l7tech.console.panels;

import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Config dialog for HardcodedResponseAssertion.
 */
public class HardcodedResponseDialog extends JDialog {
    private static final String TITLE = "Template Response Properties";
    private final InputValidator validator = new InputValidator(this, TITLE);
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField httpStatus;
    private JTextArea responseBody;
    private JTextField contentType;

    private HardcodedResponseAssertion assertion;
    private boolean modified;
    private boolean confirmed = false;

    public HardcodedResponseDialog(Frame owner, HardcodedResponseAssertion assertion, boolean modal) throws HeadlessException {
        super(owner, TITLE, modal);
        doInit(assertion);
    }

    private void doInit(HardcodedResponseAssertion assertion) {
        this.assertion = assertion;
        httpStatus.setDocument(new NumberField(String.valueOf(Long.MAX_VALUE).length()));

        validator.constrainTextFieldToNumberRange("HTTP status", httpStatus, 1, Integer.MAX_VALUE);
        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        updateView();

        getContentPane().add(mainPanel);
    }

    private void updateView() {
        httpStatus.setText(String.valueOf(assertion.getResponseStatus()));
        String body = assertion.responseBodyString();
        responseBody.setText(body == null ? "" : body);
        String ctype = assertion.getResponseContentType();
        contentType.setText(ctype == null ? "" : ctype);
    }

    public boolean isModified() {
        return modified;
    }

    public boolean wasConfirmed() {
        return confirmed;
    }

    private void doSave() {
        int status = Integer.parseInt(httpStatus.getText());
        if (status < 1) {
            status = 1;
        }
        assertion.setResponseStatus(status);
        final String ctype = contentType.getText();
        assertion.setResponseContentType(ctype);
        assertion.responseBodyString(getResponseBodyText(ctype, responseBody.getText()));
        modified = true;
        confirmed = true;
        setVisible(false);
    }

    private String getResponseBodyText(String contentType, String responseBody) {
        String txt = responseBody;

        // See if it wants to be XML
        ContentTypeHeader ctype;
        try {
            ctype = ContentTypeHeader.parseValue(contentType);
            if (ctype.isXml()) {
                // It claims to be XML.  Make sure it really is.
                try {
                    XmlUtil.stringToDocument(txt);
                    // FALLTHROUGH and save it as-is
                } catch (SAXException e) {
                    // Try trimming it
                    try {
                        XmlUtil.stringToDocument(txt.trim());

                        // Yep, trimming it is Ok
                        txt = txt.trim();
                    } catch (SAXException e1) {
                        // Nope, FALLTHROUGH and save the bogus XML as-is
                    }
                }
            }
        } catch (IOException e) {
            // Not XML; FALLTHROUGH and let it through as-is, whatever it is
        }

        return txt;
    }

    private void doCancel() {
        modified = false;
        confirmed = false;
        setVisible(false);
    }

    public HardcodedResponseAssertion getAssertion() {
        return assertion;
    }
}
