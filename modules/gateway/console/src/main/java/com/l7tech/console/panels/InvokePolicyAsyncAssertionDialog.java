package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.InvokePolicyAsyncAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Logger;

public class InvokePolicyAsyncAssertionDialog extends AssertionPropertiesEditorSupport<InvokePolicyAsyncAssertion> {
    private static final Logger logger = Logger.getLogger(InvokePolicyAsyncAssertionDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.InvokePolicyAsyncAssertionDialog");

    private JPanel contentPane;
    private JComboBox<String> workQueueComboBox;
    private JButton manageQueuesButton;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox policyComboBox;
    private JLabel workQueueLabel;
    private JLabel policyToInvokeLabel;

    private InputValidator inputValidator;
    private InvokePolicyAsyncAssertion assertion;
    private boolean confirmed;
    private Collection<Policy> policies = new ArrayList<>();

    public InvokePolicyAsyncAssertionDialog(final Window owner, final InvokePolicyAsyncAssertion assertion) {
        super(owner, resources.getString("dialog.title.invoke.policy.async.props"), AssertionPropertiesEditorSupport.DEFAULT_MODALITY_TYPE);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        inputValidator = new InputValidator(this, this.getTitle());
        inputValidator.ensureComboBoxSelection(workQueueLabel.getText(), workQueueComboBox);
        inputValidator.ensureComboBoxSelection(policyToInvokeLabel.getText(), policyComboBox);

        final RunOnChangeListener policyListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                if (policyComboBox.getSelectedIndex() != -1) {
                    enableOrDisableOkButton();
                }
            }
        });

        final RunOnChangeListener workQueueListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                if (workQueueComboBox.getSelectedIndex() != -1) {
                    enableOrDisableOkButton();
                }
            }
        });

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        manageQueuesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkQueueManagerDialog dialog = new WorkQueueManagerDialog(TopComponents.getInstance().getTopParent());
                Utilities.centerOnParentWindow(dialog);
                dialog.pack();
                dialog.setVisible(true);
                if (dialog.isClosed()) {
                    if (dialog.getSelectedWorkQueueName() != null) {
                        assertion.setWorkQueueName(dialog.getSelectedWorkQueueName());
                    }
                    populateWorkQueueCombobox();
                }
            }
        });

        ((JTextField) policyComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(policyListener);
        policyComboBox.addItemListener(policyListener);
        ((JTextField) workQueueComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(workQueueListener);
        workQueueComboBox.addItemListener(workQueueListener);

        modelToView();

        setContentPane(contentPane);
        pack();
    }

    private void modelToView() {
        populateWorkQueueCombobox();
        populatePolicyCombobox();
    }

    private void viewToModel(final InvokePolicyAsyncAssertion assertion) {
        assertion.setWorkQueueName((String) workQueueComboBox.getSelectedItem());
        for (Policy policy : policies) {
            if (policy.getName().equals(policyComboBox.getSelectedItem())) {
                assertion.setPolicyName(policy.getName());
                assertion.setPolicyGoid(policy.getGoid());
                return;
            }
        }
        // Should not happen
        JOptionPane.showMessageDialog(this, resources.getString("message.error.find.policy"), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void populatePolicyCombobox() {
        String selectedPolicy = null;

        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            policies = Registry.getDefault().getPolicyAdmin().findPoliciesByTypeTagAndSubTag(
                    PolicyType.POLICY_BACKED_OPERATION, "com.l7tech.objectmodel.polback.BackgroundTask", "run");

            Collections.sort((ArrayList<Policy>)policies, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Policy)o1).getName().compareTo(((Policy)o2).getName());
                }
            });

            for (Policy policy : policies) {
                final Goid policyGoid = policy.getGoid();
                model.addElement(policy.getName());
                if (policyGoid.equals(assertion.getPolicyGoid())) {
                    selectedPolicy = policy.getName();
                }
            }

            policyComboBox.setModel(model);
            policyComboBox.setSelectedItem(selectedPolicy);
        } catch (final FindException fe) {
            JOptionPane.showMessageDialog(this, resources.getString("message.error.find.policies"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateWorkQueueCombobox() {
        String selectedWorkQueue = null;

        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            WorkQueueManagerAdmin workQueueManagerAdmin = Registry.getDefault().getWorkQueueManagerAdmin();

            for (String wqName : workQueueManagerAdmin.getAllWorkQueueNames()) {
                model.addElement(wqName);
                if (wqName.equals(assertion.getWorkQueueName())) {
                    selectedWorkQueue = wqName;
                }
            }

            workQueueComboBox.setModel(model);
            workQueueComboBox.setSelectedItem(selectedWorkQueue);
        } catch (FindException fe) {
            JOptionPane.showMessageDialog(this, resources.getString("message.error.find.work.queues"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ok(){
        confirmed = true;
        dispose();
    }

    private void cancel() {
        InvokePolicyAsyncAssertionDialog.this.dispose();
    }

    @Override
    public void setData(InvokePolicyAsyncAssertion assertion) {
        this.assertion = assertion;
        modelToView();
        configureView();
    }

    @Override
    public InvokePolicyAsyncAssertion getData(final InvokePolicyAsyncAssertion assertion) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    protected void configureView() {
        enableOrDisableOkButton();
    }

    private void enableOrDisableOkButton() {
        boolean enabled = inputValidator.isValid();
        okButton.setEnabled(enabled);
    }

}
