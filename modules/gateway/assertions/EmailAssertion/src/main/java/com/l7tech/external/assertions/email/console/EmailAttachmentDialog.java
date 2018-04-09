package com.l7tech.external.assertions.email.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.email.EmailAssertion;
import com.l7tech.external.assertions.email.EmailAttachment;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.event.*;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Represents Email Attachment Configuration Dialog.
 * It is used to capture name and source variable of attachment.
 */
public class EmailAttachmentDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameTextField;
    private JLabel nameCheckLabel;
    private TargetVariablePanel sourceVariablePanel;
    private JCheckBox mimePartVariable;

    private final EmailAssertion assertion;
    private final Assertion previousAssertion;
    private boolean confirmed = false;

    private boolean isEditMode;
    private int editSelectedIndex;

    private List<EmailAttachment> existingAttachments;

    /* Reads the EmailAssertion.properties to get messages. */
    private static ResourceBundle resources = ResourceBundle.getBundle(EmailAssertion.class.getSimpleName());
    private static final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

    public EmailAttachmentDialog(final JDialog owner, final EmailAssertion assertion, final Assertion
            previousAssertion, final List<EmailAttachment> existingAttachments) {
        super(owner, resources.getString("email.attachment.dialog.title"));
        this.assertion = assertion;
        this.previousAssertion = previousAssertion;
        this.existingAttachments = existingAttachments;
        initDialog();
    }

    private void initDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        initDialogCloseEvents();

        nameCheckLabel.setVisible(false);
        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());
        nameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentUpdate(DocumentEvent e, DocumentUpdateType updateType) {
                updateEnableDisableState();
            }
        });
        mimePartVariable.addItemListener(e -> {
            nameTextField.setEnabled(e.getStateChange() != ItemEvent.SELECTED);
            updateEnableDisableState();
        });

        TextComponentPauseListenerManager.registerPauseListener(nameTextField, new PauseListener() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                if (isAttachmentNameDuplicate()) {
                    nameCheckLabel.setText(resources.getString("email.attachment.error.duplicate.names"));
                    nameCheckLabel.setIcon(WARNING_ICON);
                    nameCheckLabel.setVisible(true);
                    okButton.setEnabled(false);
                } else {
                    nameCheckLabel.setVisible(false);
                    nameCheckLabel.setIcon(null);
                    nameCheckLabel.setText("");
                    updateEnableDisableState();
                }
                EmailAttachmentDialog.this.pack();
            }

            @Override
            public void textEntryResumed(JTextComponent component) {
                nameCheckLabel.setVisible(false);
                nameCheckLabel.setText("");
            }
        }, 300);
        sourceVariablePanel.setValueWillBeRead(true);
        sourceVariablePanel.setValueWillBeWritten(false);
        sourceVariablePanel.setAssertion(assertion, previousAssertion);
        sourceVariablePanel.addChangeListener(e -> updateEnableDisableState());
    }

    /**
     *
     * @return true if the attachment name is duplicate, false otherwise
     */
    private boolean isAttachmentNameDuplicate() {
        String name = nameTextField.getText().trim();
        for (int i = 0; i<existingAttachments.size(); i++) {
            if (isEditMode && editSelectedIndex == i) {
                continue;
            }

            EmailAttachment attachment = existingAttachments.get(i);
            if (StringUtils.isNotBlank(attachment.getName())) {
                if (attachment.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void initDialogCloseEvents() {
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        confirmed = true;
        if (mimePartVariable.isSelected()) {
            nameTextField.setText("");
        }
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    private void updateEnableDisableState() {
        boolean isMultipartCheck = mimePartVariable.isSelected() || StringUtils.isNotBlank(nameTextField.getText());
        okButton.setEnabled(isMultipartCheck && sourceVariablePanel.isEntryValid() &&
                !isAttachmentNameDuplicate());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(final EmailAttachment attachment, final boolean isEditMode, final int editSelectedIndex) {
        nameTextField.setText(attachment.getName().trim());
        sourceVariablePanel.setVariable(attachment.getSourceVariable());
        mimePartVariable.setSelected(attachment.isMimePartVariable());
        this.editSelectedIndex = editSelectedIndex;
        this.isEditMode = isEditMode;
    }

    public EmailAttachment getData() {
        return new EmailAttachment(nameTextField.getText().trim(), sourceVariablePanel.getVariable().trim(),
                mimePartVariable.isSelected());
    }

    public static EmailAttachmentDialog createDialog(final JDialog owner, final EmailAssertion assertion, final
            Assertion previousAssertion, final List<EmailAttachment> existingAttachments) {
        final EmailAttachmentDialog dialog = new EmailAttachmentDialog(owner, assertion, previousAssertion, existingAttachments);

        dialog.setTitle(resources.getString("email.attachment.add.dialog.title"));
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);

        return dialog;
    }

    public static EmailAttachmentDialog createDialog(final JDialog owner, final EmailAssertion assertion, final
            Assertion previousAssertion, final List<EmailAttachment> existingAttachments, final EmailAttachment
            attachment, int editSelectedIndex) {
        final EmailAttachmentDialog dialog = createDialog(owner, assertion, previousAssertion, existingAttachments);

        dialog.setTitle(resources.getString("email.attachment.edit.dialog.title"));
        dialog.setData(attachment, true, editSelectedIndex);
        return dialog;
    }

}
