package com.l7tech.console.panels;

import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyCheckpointState;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AuditRecordToXmlAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class AuditSinkGlobalPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(AuditSinkGlobalPropertiesDialog.class.getName());
    private static final String INTERNAL_TAG_AUDIT_SINK = "audit-sink";
    private static final String AUDIT_SINK_POLICY_GUID_CLUSTER_PROP = "audit.sink.policy.guid";

    private JPanel mainPanel;
    private JButton editPolicyButton;
    private JRadioButton rbOutputToPolicy;
    private JRadioButton rbSaveToDb;
    private JButton okButton;
    private JButton cancelButton;

    public AuditSinkGlobalPropertiesDialog(Window owner) {
        super(owner, "Audit Sink Properties", ModalityType.DOCUMENT_MODAL);
        getContentPane().setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initMainPanel();
    }

    private void initMainPanel() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commit();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        editPolicyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSinkPolicy();
            }
        });

        //getRootPane().setDefaultButton(okButton); // Not sure we want to make this quite THAT easy to commit
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(okButton, cancelButton);

        rbOutputToPolicy.setSelected(isUsingSinkPolicy());
    }

    private boolean isUsingSinkPolicy() {
        try {
            String configuredGuid = ClusterPropertyCrud.getClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP);
            if (configuredGuid == null || configuredGuid.trim().length() < 1)
                return false;
            final PolicyHeader sinkHeader = getSinkPolicyHeader();
            return sinkHeader != null && configuredGuid.equals(sinkHeader.getGuid());
        } catch (FindException e) {
            return false;
        }
    }

    private PolicyHeader getSinkPolicyHeader() throws FindException {
        Collection<PolicyHeader> allInternals = Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INTERNAL);
        for (PolicyHeader internal : allInternals) {
            if (INTERNAL_TAG_AUDIT_SINK.equals(internal.getDescription())) {
                return internal;
            }
        }
        return null;
    }

    private PolicyHeader getOrCreateSinkPolicyHeader() throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getSinkPolicyHeader();
        if (header == null) {
            // Create new sink policy with default settings
            AllAssertion all = new AllAssertion();
            all.addChild(new AuditRecordToXmlAssertion());
            HttpRoutingAssertion routingAssertion = new HttpRoutingAssertion();
            routingAssertion.setProtectedServiceUrl("${gateway.audit.sink.url}");
            all.addChild(routingAssertion);
            String theXml = WspWriter.getPolicyXml(all);
            Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", theXml, false);
            policy.setInternalTag(INTERNAL_TAG_AUDIT_SINK);
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            TopComponents.getInstance().getServicesFolderNode().reloadChildren();
        }
        return header;
    }

    private void editSinkPolicy() {
        try {
            PolicyHeader header = getOrCreateSinkPolicyHeader();
            PolicyEntityNode node = new PolicyEntityNode(header);
            Action editAction = new EditPolicyAction(node, true);
            dispose();
            editAction.actionPerformed(null);
        } catch (FindException e) {
            err("load audit sink", e);
        } catch (PolicyAssertionException e) {
            err("configure audit sink", e);
        } catch (SaveException e) {
            err("save audit sink policy", e);
        }
    }

    private void commit() {
        try {
            if (rbSaveToDb.isSelected()) {
                ClusterPropertyCrud.putClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP, "");
                return;
            }

            PolicyHeader header = getOrCreateSinkPolicyHeader();
            String guid = header.getGuid();
            ClusterPropertyCrud.putClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP, guid);
        } catch (ObjectModelException e) {
            err("configure audit sink", e);
        } catch (PolicyAssertionException e) {
            err("configure audit sink", e);
        }
    }

    private void err(String what, Throwable t) {
        final String msg = "Unable to " + what + ": " + ExceptionUtils.getMessage(t);
        logger.log(Level.WARNING, msg, t);
        DialogDisplayer.showMessageDialog(this, "Unable to " + what, msg, null);
    }
}
