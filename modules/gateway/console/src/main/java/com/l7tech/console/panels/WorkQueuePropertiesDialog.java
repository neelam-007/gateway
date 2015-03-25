package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.variable.Syntax.getReferencedNames;
import static com.l7tech.util.ValidationUtils.isValidInteger;

public class WorkQueuePropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(WorkQueuePropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.WorkQueuePropertiesDialog");
    private static final String WINDOW_TITLE = "Work Queue Properties";
    private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;
    private static final int DEFAULT_MAX_THREAD_POOL_SIZE = 100;

    private JPanel mainPanel;
    private JTextField nameTextField;
    private JTextField maxQueueSizeTextField;
    private JTextField threadPoolMaxTextField;
    private JButton cancelButton;
    private JButton okButton;
    private ButtonGroup rejectPolicyButtonGroup;
    private JRadioButton failImmediatelyRadioButton;
    private JRadioButton waitForRoomRadioButton;
    private SecurityZoneWidget zoneControl;

    private InputValidator validator;
    private final WorkQueue workQueue;
    private boolean confirmed;

    public WorkQueuePropertiesDialog(Frame owner, WorkQueue workQueue) {
        super(owner, WINDOW_TITLE, true);
        this.workQueue = workQueue;
        initialize();
    }

    public WorkQueuePropertiesDialog(Dialog owner, WorkQueue workQueue) {
        super(owner, WINDOW_TITLE, true);
        this.workQueue = workQueue;
        initialize();
    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        setContentPane(mainPanel);
        validator = new InputValidator(this, this.getTitle());
        Utilities.setEscKeyStrokeDisposes(this);

        rejectPolicyButtonGroup = new ButtonGroup();
        rejectPolicyButtonGroup.add(failImmediatelyRadioButton);
        rejectPolicyButtonGroup.add(waitForRoomRadioButton);

        nameTextField.setDocument(new MaxLengthDocument(128));
        maxQueueSizeTextField.setDocument(new MaxLengthDocument(32));
        threadPoolMaxTextField.setDocument(new MaxLengthDocument(32));
        validator.constrainTextFieldToBeNonEmpty("Queue Name", nameTextField, null);
        validator.constrainTextFieldToBeNonEmpty("Max Queue Size", maxQueueSizeTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String errMsg = null;
                final String max = maxQueueSizeTextField.getText().trim();
                if (!isValidInteger(max, true, 1, 1000000) && getReferencedNames(max).length == 0) {
                    errMsg = "The value for the max queue size must be between 1 and 1000000.";
                }
                return errMsg;
            }
        });
        validator.constrainTextFieldToBeNonEmpty("Thread Pool Max", threadPoolMaxTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String errMsg = null;
                final String max = threadPoolMaxTextField.getText().trim();
                if (!isValidInteger(max, true, 1, 10000) && getReferencedNames(max).length == 0) {
                    errMsg = "The value for the max worker threads must be between 1 and 10000.";
                }
                return errMsg;
            }
        });

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        final RunOnChangeListener docListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableButtons();
            }
        });
        nameTextField.getDocument().addDocumentListener(docListener);
        maxQueueSizeTextField.getDocument().addDocumentListener(docListener);
        threadPoolMaxTextField.getDocument().addDocumentListener(docListener);

        zoneControl.configure(workQueue);
        modelToView();
    }

    private WorkQueueManagerAdmin getWorkQueueManagerAdmin() {
        WorkQueueManagerAdmin admin = null;
        if (Registry.getDefault().isAdminContextPresent()) {
            admin = Registry.getDefault().getWorkQueueManagerAdmin();
        } else {
            logger.log(Level.WARNING, "No Admin Context present!");
        }
        return admin;
    }

    private void onOk() {
        String warningMessage = checkDuplicateWorkQueue();
        if (warningMessage != null) {
            DialogDisplayer.showMessageDialog(WorkQueuePropertiesDialog.this, warningMessage,
                    resources.getString("dialog.title.error.saving.work.queue"), JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        viewToModel(this.workQueue);
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void selectName() {
        nameTextField.requestFocus();
        nameTextField.selectAll();
    }

    private void modelToView() {
        nameTextField.setText(workQueue.getName());
        final int queueSize = workQueue.getMaxQueueSize();
        final int poolMax = workQueue.getThreadPoolMax();
        maxQueueSizeTextField.setText(String.valueOf(queueSize < 1 ? DEFAULT_MAX_QUEUE_SIZE : queueSize));
        threadPoolMaxTextField.setText(String.valueOf(poolMax < 1 ? DEFAULT_MAX_THREAD_POOL_SIZE : poolMax));
        if (workQueue.getRejectPolicy().equals(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY)) {
            failImmediatelyRadioButton.setSelected(true);
        } else {
            waitForRoomRadioButton.setSelected(true);
        }
    }

    private void viewToModel(WorkQueue workQueue) {
        workQueue.setName(nameTextField.getText().trim());
        workQueue.setMaxQueueSize(Integer.parseInt(maxQueueSizeTextField.getText().trim()));
        workQueue.setThreadPoolMax(Integer.parseInt(threadPoolMaxTextField.getText().trim()));
        if (failImmediatelyRadioButton.isSelected()) {
            workQueue.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);
        } else {
            workQueue.setRejectPolicy(WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM);
        }
        workQueue.setSecurityZone(zoneControl.getSelectedZone());
    }

    private void enableOrDisableButtons() {
        boolean enabled = isNonEmptyRequiredTextField(nameTextField.getText()) &&
                isNonEmptyRequiredTextField(maxQueueSizeTextField.getText())  &&
                isNonEmptyRequiredTextField(threadPoolMaxTextField.getText());
        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void onCancel() {
        this.dispose();
    }

    private String checkDuplicateWorkQueue() {
        WorkQueueManagerAdmin admin = getWorkQueueManagerAdmin();
        if (admin == null) return "Cannot get Work Queue Manager Admin.  Check the log and try again.";

        String originalWorkQueueName = workQueue.getName();
        String workQueueName = nameTextField.getText();
        if (originalWorkQueueName.compareToIgnoreCase(workQueueName) == 0) return null;

        try {
            for (String name : admin.getAllWorkQueueNames()) {
                if (workQueueName.compareToIgnoreCase(name) == 0) {
                    return "The work queue name \"" + name + "\" already exists. Try a new name.";
                }
            }
        } catch (FindException e) {
            return "Cannot find work queues.  Check the log and Try again.";
        }
        return null;
    }
}
