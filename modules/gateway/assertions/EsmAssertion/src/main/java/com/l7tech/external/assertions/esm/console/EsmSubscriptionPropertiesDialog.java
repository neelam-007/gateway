package com.l7tech.external.assertions.esm.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import com.l7tech.external.assertions.esm.EsmConstants;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * User: megery
 */
public class EsmSubscriptionPropertiesDialog extends AssertionPropertiesEditorSupport<EsmSubscriptionAssertion> {
    private JPanel mainPanel;
    private JList policyList;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel infoLabel;
    private EsmSubscriptionAssertion assertion;
    private boolean ok;

    private DefaultComboBoxModel listModel;

    public EsmSubscriptionPropertiesDialog(Window owner, EsmSubscriptionAssertion assertion) {
        super(owner, assertion);
        initialise();
    }

    private void initialise() {
        add(mainPanel);
        String labelText = "<html>Select the policy to be used for notifications subscribed through this service.<br>" +
                           "NOTE: Existing subscriptions using other policies will need to be renewed to specify this new policy</html>";
        infoLabel.setText(labelText);
        createListModel();
        
        policyList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisable();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok = true;
                viewToModel();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                enableDisable();
            }
        });
        Utilities.setEscKeyStrokeDisposes( this );
    }

    private void createListModel() {
        java.util.List<PolicyHeaderWrapper> notificationPolicyHeaders = new ArrayList<PolicyHeaderWrapper>();
        notificationPolicyHeaders.add(new PolicyHeaderWrapper(null));
        try {
            Collection<PolicyHeader> allInternalHeaders = Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INTERNAL);
            for (PolicyHeader head : allInternalHeaders) {
                if (EsmConstants.POLICY_TAG_ESM_NOTIFICATION.equals(head.getDescription())) {                    
                    notificationPolicyHeaders.add(new PolicyHeaderWrapper(head));
                }
            }

            if(assertion != null && assertion.retrieveFragmentPolicy() != null) {
                notificationPolicyHeaders.add(new PolicyHeaderWrapper(new PolicyHeader(assertion.retrieveFragmentPolicy())));
            }
        } catch (FindException e) {
            throw new RuntimeException("Unable to load policies", e);
        }

        // Sort the policies in alphabetical order
        Collections.sort(notificationPolicyHeaders, new Comparator<PolicyHeaderWrapper>() {
            @Override
            public int compare(PolicyHeaderWrapper o1, PolicyHeaderWrapper o2) {
                if(o1.getHeader() == null && o2.getHeader() != null) {
                    return -1;
                } else if(o1.getHeader() == null && o2.getHeader() == null) {
                    return 0;
                } else if(o1.getHeader() != null && o2.getHeader() == null) {
                    return 1;
                } else {
                    return o1.getHeader().getName().compareTo(o2.getHeader().getName());
                }
            }
        });

        listModel = new DefaultComboBoxModel(notificationPolicyHeaders.toArray(new PolicyHeaderWrapper[notificationPolicyHeaders.size()]));
        policyList.setModel(listModel);
    }

    private void viewToModel() {
        if (listModel.getSize() != 0) {
            PolicyHeaderWrapper header = (PolicyHeaderWrapper)policyList.getSelectedValue();
            if (header == null) throw new IllegalStateException("A policy must be selected");
            if (header.getHeader() == null)
                assertion.setNotificationPolicyGuid(null);
            else
                assertion.setNotificationPolicyGuid(header.getHeader().getGuid());
        }
    }

    private void modelToView() {
        String polId = assertion.getNotificationPolicyGuid();
        if (StringUtils.isEmpty(polId)) {
            if (policyList.getModel().getSize() > 0) policyList.setSelectedIndex(0);
            return;
        }

        ListModel model = policyList.getModel();
        PolicyHeaderWrapper selectedHeaderWrapper;
        for (int i = 0; i < model.getSize(); i++) {
            selectedHeaderWrapper = (PolicyHeaderWrapper) model.getElementAt(i);
            PolicyHeader selectedHeader = selectedHeaderWrapper.getHeader();
            if (selectedHeader != null && selectedHeader.getGuid().equals(polId)) {
                policyList.setSelectedValue(selectedHeaderWrapper, true);
                break;
            }
        }
    }


    private void enableDisable() {
        if (listModel.getSize() == 0) {
            okButton.setEnabled(!isReadOnly());
        } else {
            okButton.setEnabled(policyList.getSelectedValue() != null && !isReadOnly());
        }
    }

    @Override
    public boolean isConfirmed() {
        return ok;
    }

    @Override
    public void setData(EsmSubscriptionAssertion assertion) {
        this.assertion = assertion;
        createListModel();
        modelToView();
    }

    @Override
    public EsmSubscriptionAssertion getData(EsmSubscriptionAssertion assertion) {
        return assertion;
    }

    private class PolicyHeaderWrapper{
        PolicyHeader header;
        public PolicyHeaderWrapper(PolicyHeader head) {
            this.header = head;
        }

        public PolicyHeader getHeader() {
            return header;
        }

        public void setHeader(PolicyHeader header) {
            this.header = header;
        }

        @Override
        public String toString() {
            if (header != null) {
                return header.getName();
            } else {
                return "<no notification policy>";
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PolicyHeaderWrapper && header != null && header.equals(((PolicyHeaderWrapper) obj).getHeader());
        }
    }
}
