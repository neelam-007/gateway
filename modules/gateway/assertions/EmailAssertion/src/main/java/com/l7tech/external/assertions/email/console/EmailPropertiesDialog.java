/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.external.assertions.email.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.email.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.Syntax;
import org.apache.commons.lang.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.l7tech.external.assertions.email.EmailMessage.EmailMessageBuilder;

/**
 * Properties dialog for {@link EmailAssertion}.
 */
public class EmailPropertiesDialog extends AssertionPropertiesOkCancelSupport<EmailAssertion> {

    private final InputValidator validator;
    private EmailAssertion assertion;

    private JPanel mainPanel;
    private JTextField toAddressesField;
    private JTextField fromAddressField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField subjectField;
    private JTextArea messageField;
    private JComboBox protocolCombo;
    private JLabel authUsernameLabel;
    private JTextField authUsernameField;
    private JLabel authPasswordLabel;
    private JPasswordField authPasswordField;
    private JTextField ccAddressesField;
    private JTextField bccAddressesField;
    private JCheckBox authenticateCheckBox;
    private JButton sendTestEmailButton;
    private JCheckBox contextVarPasswordCheckBox;
    private JButton manageAttachmentsButton;
    private JRadioButton plainTextRadioButton;
    private JRadioButton htmlRadioButton;
    private JLabel attachmentsSummaryLabel;
    private JPanel attachmentsPanel;
    private char echoChar;
    private List<EmailAttachment> attachments;

    private static final String ATTACHMENTS_SUMMARY_EMPTY_TEXT = "None";
    private static final int ATTACHMENTS_SUMMARY_TEXT_MAX_LENGTH = 80;
    private static final int MIN_PORT_NUM = 1;
    private static final int MAX_PORT_NUM = 65535;

    /* Reads the EmailAssertion.properties to get messages. */
    private static ResourceBundle resources = ResourceBundle.getBundle(EmailAssertion.class.getSimpleName());
    private static final String EMAIL_TEST_TITLE = resources.getString("email.test.title");

    /* Character limit in a single line for the email list (To, Cc and Bcc) shown in Confirm Email Test dialig. */
    private static int CHARACTER_LIMIT_IN_LINE_FOR_EMAIL_LIST = 100;
    /**
     * Creates a new EmailPropertiesDialog object backed by the provided EmailAssertion object.
     *
     * @param parent The window that owns this dialog
     * @param assertion The backing EmailAssertion object
     * @throws HeadlessException
     */
    public EmailPropertiesDialog(final Frame parent, final EmailAssertion assertion) throws HeadlessException {
        super(EmailAssertion.class, parent, assertion, true);
        this.validator = new InputValidator(this, getTitle());
        this.assertion = assertion;
        initComponents();
    }

    /**
     * Initializes this dialog and sets the fields to the values from the EmailAssertion object.
     */
    @Override
    protected void initComponents() {
        super.initComponents();

        sendTestEmailButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    DialogDisplayer.showConfirmDialog(sendTestEmailButton, constructRecipientEmailMessage(),
                    "Confirm Email Test", JOptionPane
                            .OK_CANCEL_OPTION, new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.OK_OPTION) {
                                doSendTestMail();
                            }
                        }
                    });
                }catch (AddressException e1) {
                    DialogDisplayer.showMessageDialog(sendTestEmailButton,
                            "Unable to parse email addresses: " + e1.getMessage(),
                            EMAIL_TEST_TITLE, JOptionPane.ERROR_MESSAGE, null);
                }
            }

            private String constructRecipientEmailMessage() throws AddressException {
                final StringBuilder msg = new StringBuilder("This will send an email to the following recipients.\n\n");
                msg.append("To:  ").append(constructEmailList(toAddressesField.getText())).append("\n");

                if (!"".equals(ccAddressesField.getText())) {
                    msg.append("Cc:  ").append(constructEmailList(ccAddressesField.getText())).append("\n");
                }

                if (!"".equals(bccAddressesField.getText())) {
                    msg.append("Bcc: ").append(constructEmailList(bccAddressesField.getText())).append("\n");
                }
                msg.append("\nNote: Attachments will be ignored.");
                return msg.toString();
            }
        });

        protocolCombo.setModel(new DefaultComboBoxModel(EmailProtocol.values()));
        protocolCombo.addActionListener(e -> {
            final EmailProtocol proto = (EmailProtocol) protocolCombo.getSelectedItem();
            portField.setText(Integer.toString(proto.getDefaultSmtpPort()));
            updateEnableDisableState();
        });

        authenticateCheckBox.addActionListener(e -> updateEnableDisableState());

        echoChar = authPasswordField.getEchoChar();
        contextVarPasswordCheckBox.addItemListener(e -> {
            authPasswordField.enableInputMethods(contextVarPasswordCheckBox.isSelected());
            authPasswordField.setEchoChar(contextVarPasswordCheckBox.isSelected() ? (char)0 : echoChar);
        });

        validator.constrainTextFieldToBeNonEmpty("Host", hostField, null);
        validator.constrainTextFieldToBeNonEmpty("Port", portField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(!Syntax.isAnyVariableReferenced(portField.getText())) {
                    final String errorMessage = "Port field must be a valid number between " + MIN_PORT_NUM + " and " +
                            MAX_PORT_NUM + " or a valid context variable reference";
                    try {
                        final int port = Integer.parseInt(portField.getText());
                        if (port < MIN_PORT_NUM || port > MAX_PORT_NUM){
                            return errorMessage;
                        }
                    } catch (NumberFormatException nf) {
                        return errorMessage;
                    }
                }
                return null;
            }
        });
        validator.constrainTextFieldToBeNonEmpty("From", fromAddressField, null);
        validator.constrainTextFieldToBeNonEmpty("To", toAddressesField, null);

        updateEnableDisableState();

        final DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            public void documentUpdate(DocumentEvent e, DocumentUpdateType updateType) {
                updateEnableDisableState();
            }
        };

        hostField.getDocument().addDocumentListener(documentAdapter);
        toAddressesField.getDocument().addDocumentListener(documentAdapter);
        portField.getDocument().addDocumentListener(documentAdapter);
        authUsernameField.getDocument().addDocumentListener(documentAdapter);
        authPasswordField.getDocument().addDocumentListener(documentAdapter);
        fromAddressField.getDocument().addDocumentListener(documentAdapter);
        subjectField.getDocument().addDocumentListener(documentAdapter);
        messageField.getDocument().addDocumentListener(documentAdapter);

        manageAttachmentsButton.addActionListener(e -> onManageAttachmentsAction());
    }

    private void onManageAttachmentsAction() {
        final EmailAttachmentsManagerDialog dialog = EmailAttachmentsManagerDialog.createDialog(this, assertion, getPreviousAssertion(), attachments);
        DialogDisplayer.display(dialog, () -> {
            if (dialog.isConfirmed()) {
                attachments.clear();
                attachments.addAll(dialog.getData());
                updateAttachmentsSummary();
            }
        });
    }

    private void updateAttachmentsSummary() {
        final StringBuilder summary = new StringBuilder();

        for (final EmailAttachment item : attachments) {
            summary.append(summary.length() > 0 ? ", " : "");
            if(item.isMimePartVariable()) {
                summary.append("${" + item.getSourceVariable() + "} (MimePart)");
            } else {
                summary.append(item.getName());
            }
        }

        if (summary.length() == 0) {
            summary.append(ATTACHMENTS_SUMMARY_EMPTY_TEXT);
        }

        attachmentsSummaryLabel.setText(StringUtils.abbreviate(summary.toString(), ATTACHMENTS_SUMMARY_TEXT_MAX_LENGTH));
        attachmentsSummaryLabel.setToolTipText(summary.toString());
    }

    private void doSendTestMail() {
        try {
            //collect email config and email content information
            final EmailAssertion eaa = getData(new EmailAssertion());

            // check if using context variables
            if (usesContextVars(eaa)) {
                DialogDisplayer.showMessageDialog(sendTestEmailButton,
                        "Unable to send email containing context variable",
                        EMAIL_TEST_TITLE,
                        JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            //execute email test
            final EmailAdmin emailAdmin = Registry.getDefault().getExtensionInterface(EmailAdmin.class, null);
            final EmailMessage emailMessage = new EmailMessageBuilder(eaa).build();
            emailAdmin.sendTestEmail(emailMessage, new EmailConfig(eaa));
            DialogDisplayer.showMessageDialog(sendTestEmailButton,
                    "Email sent successfully.",
                    EMAIL_TEST_TITLE,
                    JOptionPane.INFORMATION_MESSAGE, null);
        } catch (Exception ete) {
            DialogDisplayer.showMessageDialog(sendTestEmailButton,
                    "Failed to send email.\n" + ete.getMessage(),
                    EMAIL_TEST_TITLE,
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private boolean usesContextVars(final EmailAssertion eaa) {
        return Syntax.isAnyVariableReferenced(
                eaa.getSmtpHost(), eaa.getSmtpPort(),
                eaa.getSourceEmailAddress(), eaa.getTargetEmailAddress(), eaa.getTargetCCEmailAddress(), eaa.getTargetBCCEmailAddress(),
                eaa.getSubject(), eaa.messageString(),
                authenticateCheckBox.isSelected() ? eaa.getAuthUsername() : "",
                authenticateCheckBox.isSelected() && contextVarPasswordCheckBox.isSelected() ? eaa.getAuthPassword() : "");
    }

    /**
     * Ensure correct button states through view configuration.
     */
    @Override
    protected void configureView() {
        super.configureView();
        updateEnableDisableState();
    }

    /**
     * Sets the fields to the values from the EmailAssertion object.
     * @param assertion
     */
    @Override
    public void setData(final EmailAssertion assertion) {
        protocolCombo.setSelectedItem(assertion.getProtocol());
        hostField.setText(assertion.getSmtpHost());
        portField.setText(assertion.getSmtpPort());
        toAddressesField.setText(assertion.getTargetEmailAddress());
        ccAddressesField.setText(assertion.getTargetCCEmailAddress());
        bccAddressesField.setText(assertion.getTargetBCCEmailAddress());
        subjectField.setText(assertion.getSubject());
        messageField.setText(assertion.messageString());
        messageField.setCaretPosition(0);
        fromAddressField.setText(assertion.getSourceEmailAddress());
        authenticateCheckBox.setSelected(assertion.isAuthenticate());
        contextVarPasswordCheckBox.setSelected(assertion.isContextVarPassword());
        authUsernameField.setText(assertion.getAuthUsername());
        authPasswordField.setText(assertion.getAuthPassword());
        htmlRadioButton.setSelected(assertion.getFormat() == EmailFormat.HTML);
        attachments = new ArrayList<>(assertion.getAttachments());
        updateAttachmentsSummary();
    }

    /**
     * Sets the EmailAssertion properties to the values from the fields in this dialog.
     */
    @Override
    public EmailAssertion getData(final EmailAssertion assertion) throws ValidationException {
        final String error = validator.validate();
        if (error != null) {
            throw new ValidationException(error);
        }

        assertion.setSmtpHost(hostField.getText());
        assertion.setSmtpPort(portField.getText());
        assertion.setTargetEmailAddress(toAddressesField.getText());
        assertion.setTargetCCEmailAddress(ccAddressesField.getText());
        assertion.setTargetBCCEmailAddress(bccAddressesField.getText());
        assertion.setSubject(subjectField.getText());
        assertion.messageString(messageField.getText());
        assertion.setSourceEmailAddress(fromAddressField.getText());
        assertion.setProtocol((EmailProtocol) protocolCombo.getSelectedItem());
        assertion.setFormat(htmlRadioButton.isSelected() ? EmailFormat.HTML : EmailFormat.PLAIN_TEXT);
        assertion.setAttachments(attachments);

        final boolean authenticate = authenticateCheckBox.isSelected();
        assertion.setAuthenticate(authenticate);
        if (authenticate) {
            assertion.setAuthUsername(authUsernameField.getText());
            assertion.setAuthPassword(new String(authPasswordField.getPassword()));
            assertion.setContextVarPassword(contextVarPasswordCheckBox.isSelected());
        } else {
            assertion.setAuthUsername(null);
            assertion.setAuthPassword(null);
            assertion.setContextVarPassword(false);
        }

        return assertion;
    }

    /**
     * If the authentication combo box is set to none, then the username and password fields are disabled,
     * otherwise they are enabled. If all of the necessary fields have values, then the OK button is
     * enabled, otherwise it is diabled.
     */
    private void updateEnableDisableState() {
        authUsernameLabel.setEnabled(authenticateCheckBox.isSelected());
        authUsernameField.setEnabled(authenticateCheckBox.isSelected());
        authPasswordLabel.setEnabled(authenticateCheckBox.isSelected());
        authPasswordField.setEnabled(authenticateCheckBox.isSelected());
        contextVarPasswordCheckBox.setEnabled(authenticateCheckBox.isSelected());

        getOkButton().setEnabled(areMandatoryFieldsEntered());
        sendTestEmailButton.setEnabled(getOkButton().isEnabled());
    }

    private boolean areMandatoryFieldsEntered() {
        boolean mandatoryFieldsEntered = StringUtils.isNotBlank(hostField.getText()) &&
                StringUtils.isNotBlank(portField.getText()) &&
                StringUtils.isNotBlank(fromAddressField.getText()) &&
                StringUtils.isNotBlank(toAddressesField.getText());

        if (authenticateCheckBox.isSelected()) {
            mandatoryFieldsEntered = mandatoryFieldsEntered &&
                    StringUtils.isNotBlank(authUsernameField.getText()) &&
                    StringUtils.isNotBlank(new String(authPasswordField.getPassword()));
        }

        return mandatoryFieldsEntered;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    /**
     * This method formats the list of emails in To, Cc and Bcc list for the Confirm Email Test dialog. It wraps the
     * emails in to 100 character limit and display the list in multiple lines.
     * @param emails List of emails in To or Cc or Bcc list
     * @return Formatted string
     * @throws AddressException if there was an error parsing the email list
     */
    private static String constructEmailList(String emails) throws AddressException {
        final InternetAddress[] emailAddresses = InternetAddress.parse(emails);
        final StringBuilder listToDisplay = new StringBuilder();
        int lineCharCount = 0;
        for (final InternetAddress address : emailAddresses) {
            final String emailAddress = address.getAddress();
            if(lineCharCount > 0 && (lineCharCount + emailAddress.length()) > CHARACTER_LIMIT_IN_LINE_FOR_EMAIL_LIST) {
                listToDisplay.append(",").append("\n       ").append(emailAddress);
                lineCharCount = emailAddress.length();
            } else {
                if(listToDisplay.length() > 0) {
                    listToDisplay.append(", ");
                }
                listToDisplay.append(emailAddress);
                lineCharCount += emailAddress.length();
            }
        }
        return listToDisplay.toString();
    }
}
