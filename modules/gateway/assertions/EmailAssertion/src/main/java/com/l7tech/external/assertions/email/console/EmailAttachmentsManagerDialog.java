package com.l7tech.external.assertions.email.console;

import com.l7tech.external.assertions.email.EmailAssertion;
import com.l7tech.external.assertions.email.EmailAttachment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Represents Email Attachments Manager Dialog.
 * It is used to manage one or more attachments configuration details.
 */
public class EmailAttachmentsManagerDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTable attachmentsTable;
    private JLabel attachmentsCountLabel;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private final EmailAssertion assertion;
    private final Assertion previousAssertion;
    private boolean confirmed = false;
    private SimpleTableModel<EmailAttachment> attachmentsTableModel;

    private static ResourceBundle resources = ResourceBundle.getBundle(EmailAssertion.class.getSimpleName());

    public EmailAttachmentsManagerDialog(final JDialog owner, final EmailAssertion assertion, final Assertion previousAssertion) {
        super(owner, resources.getString("email.attachment.manage.dialog.title"));
        this.assertion = assertion;
        this.previousAssertion = previousAssertion;
        initDialog();
    }

    private void initDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        initDialogCloseEvents();

        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());
        initAttachmentsTable();
        initAttachmentsButtons();
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

    private void initAttachmentsTable() {
        attachmentsTableModel = TableUtil.configureTable(
                attachmentsTable,
                TableUtil.column(resources.getString("email.attachment.manage.attachment.name"), 40, 350,
                        100000, new Functions.Unary<String, EmailAttachment>() {
                            @Override
                            public String call(EmailAttachment emailAttachment) {
                                if(emailAttachment.isMimePartVariable()) {
                                    return "<n/a>";
                                }
                                return emailAttachment.getName();
                            }
                        }, String.class),
                TableUtil.column(resources.getString("email.attachment.manage.attachment.source"), 40, 350,
                        100000, Functions.propertyTransform(EmailAttachment.class, "sourceVariable"), String.class),
                TableUtil.column(resources.getString("email.attachment.manage.attachment.parts"), 20, 200,
                        100000, Functions.propertyTransform(EmailAttachment.class, "mimePartVariable"), Boolean.class)
        );
        attachmentsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attachmentsTable.getTableHeader().setReorderingAllowed(false);
        attachmentsTable.getSelectionModel().addListSelectionListener(e -> {
            editButton.setEnabled(attachmentsTable.getSelectedRow() != -1);
            removeButton.setEnabled(editButton.isEnabled());
        });
        attachmentsTableModel.addTableModelListener(e -> {
            attachmentsCountLabel.setText(Integer.toString(attachmentsTableModel.getRowCount()));
            editButton.setEnabled(attachmentsTable.getSelectedRow() != -1);
            removeButton.setEnabled(editButton.isEnabled());
        });
    }

    private void initAttachmentsButtons() {
        addButton.addActionListener(e -> {
            final EmailAttachmentDialog dialog = EmailAttachmentDialog.createDialog(
                    EmailAttachmentsManagerDialog.this,
                    EmailAttachmentsManagerDialog.this.assertion,
                    EmailAttachmentsManagerDialog.this.previousAssertion, attachmentsTableModel.getRows());
            DialogDisplayer.display(dialog, () -> {
                if (dialog.isConfirmed()) {
                    attachmentsTableModel.addRow(dialog.getData());
                }
            });
        });

        editButton.addActionListener(e -> {
            final int selectedRowIndex = attachmentsTable.getSelectedRow();
            final EmailAttachment selectedAttachment = attachmentsTableModel.getRowObject(selectedRowIndex);
            final EmailAttachmentDialog dialog = EmailAttachmentDialog.createDialog(
                    EmailAttachmentsManagerDialog.this,
                    EmailAttachmentsManagerDialog.this.assertion,
                    EmailAttachmentsManagerDialog.this.previousAssertion, attachmentsTableModel.getRows(),
                    selectedAttachment, selectedRowIndex);
            DialogDisplayer.display(dialog, () -> {
                if (dialog.isConfirmed()) {
                    attachmentsTableModel.setRowObject(selectedRowIndex, dialog.getData());
                }
            });
        });

        removeButton.addActionListener(e -> attachmentsTableModel.removeRowAt(attachmentsTable.getSelectedRow()));
        Utilities.setDoubleClickAction(attachmentsTable, editButton);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(final List<EmailAttachment> attachments) {
        attachmentsTableModel.setRows(attachments);
    }

    public List<EmailAttachment> getData() {
        return attachmentsTableModel.getRows();
    }

    public static EmailAttachmentsManagerDialog createDialog(final JDialog owner, final EmailAssertion assertion,
                                                             final Assertion previousAssertion, final List<EmailAttachment> attachments) {
        final EmailAttachmentsManagerDialog dialog = new EmailAttachmentsManagerDialog(owner, assertion, previousAssertion);

        dialog.setData(attachments);
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);

        return dialog;
    }
}
