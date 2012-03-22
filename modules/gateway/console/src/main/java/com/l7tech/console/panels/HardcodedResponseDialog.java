package com.l7tech.console.panels;

import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.variable.Syntax;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Config dialog for HardcodedResponseAssertion.
 */
public class HardcodedResponseDialog extends AssertionPropertiesEditorSupport<HardcodedResponseAssertion> {
    public static final String RESPONSE_HTTP_STATUS = "Response HTTP Status";
    private final InputValidator validator;
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField httpStatus;
    private JTextArea responseBody;
    private JTextField contentType;
    private JCheckBox earlyResponseCheckBox;
    private JScrollPane responseScrollPane;

    private HardcodedResponseAssertion assertion;
    private boolean modified;
    private boolean confirmed = false;

    public HardcodedResponseDialog(Window owner, HardcodedResponseAssertion assertion) throws HeadlessException {
        super(owner, assertion);
        validator = new InputValidator(this, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        doInit(assertion);
    }

    private void doInit(HardcodedResponseAssertion assertion) {
        this.assertion = assertion;
        validator.constrainTextFieldToBeNonEmpty(RESPONSE_HTTP_STATUS, httpStatus, null);
        validator.addRule(getVariableValidationRule());
        validator.addRule(new IntegerOrContextVariableValidationRule(1, Integer.MAX_VALUE, "Response HTTP Status", httpStatus));
        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        updateView();

        getContentPane().add(mainPanel);
        Utilities.setEscKeyStrokeDisposes( this );
    }

    private void updateView() {
        httpStatus.setText(assertion.getResponseStatus());
        earlyResponseCheckBox.setSelected(assertion.isEarlyResponse());
        String body = assertion.responseBodyString();
        if (body == null || body.trim().isEmpty()) {
            responseScrollPane.setPreferredSize(new Dimension(500, 70));
            responseBody.setText("");
        } else {
            String[] lines = body.split("\n");
            responseScrollPane.setPreferredSize(new Dimension(500, lines.length >= 30? 400 : 150));
            responseBody.setText(body);
            responseBody.setCaretPosition(0);
        }
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
        assertion.setResponseStatus(httpStatus.getText().trim());
        assertion.setEarlyResponse(earlyResponseCheckBox.isSelected());
        final String ctype = contentType.getText();
        assertion.setResponseContentType(ctype);
        assertion.responseBodyString(getResponseBodyText(ctype, responseBody.getText()));
        modified = true;
        confirmed = true;
        dispose();
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
        dispose();
    }

    public HardcodedResponseAssertion getAssertion() {
        return assertion;
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(HardcodedResponseAssertion assertion) {
        this.assertion = assertion;
        updateView();
    }

    @Override
    public HardcodedResponseAssertion getData(HardcodedResponseAssertion assertion) {
        return getAssertion();
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }

    private InputValidator.ValidationRule getVariableValidationRule() {
        return new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                String error = null;

                try {
                    Syntax.getReferencedNames( responseBody.getText() );
                } catch (IllegalArgumentException iae) {
                    error = "Error with template variable '"+ iae.getMessage() +"'.";
                }

                return error;
            }
       };        
    }
}
