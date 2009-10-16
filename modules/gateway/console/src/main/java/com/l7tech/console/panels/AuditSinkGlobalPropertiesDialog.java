package com.l7tech.console.panels;

import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
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
import com.l7tech.policy.assertion.*;
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
    private static final String AUDIT_SINK_ALWAYS_SAVE_CLUSTER_PROP = "audit.sink.alwaysSaveInternal";

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox cbOutputToPolicy;
    private JCheckBox cbSaveToDb;

    boolean committed = false;
    boolean policyEditRequested = false;

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
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                committed = false;
                dispose();
            }
        });

        final ActionListener buttonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(isCommittable());
            }
        };
        cbSaveToDb.addActionListener(buttonListener);
        cbOutputToPolicy.addActionListener(buttonListener);

        //getRootPane().setDefaultButton(okButton); // Not sure we want to make this quite THAT easy to commit
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(okButton, cancelButton);

        final boolean usingSinkPolicy = isUsingSinkPolicy();
        cbOutputToPolicy.setSelected(usingSinkPolicy);
        cbSaveToDb.setSelected(!usingSinkPolicy || isAlwaysSaveToDb());
    }

    private boolean isUsingSinkPolicy() {
        try {
            String configuredGuid = ClusterPropertyCrud.getClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP);
            if (configuredGuid == null || configuredGuid.trim().length() < 1)
                return false;
            final PolicyHeader sinkHeader = getSinkPolicyHeader();
            return sinkHeader != null && configuredGuid.equals(sinkHeader.getGuid());
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to check if sink policy configured and exists: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }
    }

    private boolean isAlwaysSaveToDb() {
        try {
            String val = ClusterPropertyCrud.getClusterProperty(AUDIT_SINK_ALWAYS_SAVE_CLUSTER_PROP);
            return Boolean.valueOf(val);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to check if always save to DB: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
            Policy policy = makeDefaultAuditSinkPolicyEntity();
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
        }
        return header;
    }

    private static Policy makeDefaultAuditSinkPolicyEntity() {
        String theXml = makeDefaultAuditSinkPolicyXml();
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", theXml, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_SINK);
        return policy;
    }

    private static String makeDefaultAuditSinkPolicyXml() {
        AllAssertion all = makeDefaultAuditSinkPolicyAssertions();
        return WspWriter.getPolicyXml(all);
    }

    private static AllAssertion makeDefaultAuditSinkPolicyAssertions() {
        AllAssertion all = new AllAssertion();
        all.addChild(new CommentAssertion("A simple audit sink policy could convert the audit record to XML, then post it somewhere via HTTP"));
        all.addChild(new AuditRecordToXmlAssertion());
        all.addChild(new CommentAssertion("Enable the below routing assertion or replace it with something else"));
        HttpRoutingAssertion routingAssertion = new HttpRoutingAssertion();
        routingAssertion.setProtectedServiceUrl("${gateway.audit.sink.url}");
        routingAssertion.setEnabled(false);
        all.addChild(routingAssertion);
        FalseAssertion falseAssertion = new FalseAssertion();
        String falseName = falseAssertion.meta().get(AssertionMetadata.SHORT_NAME);
        all.addChild(new CommentAssertion("The below " + falseName + " causes this sink policy to always fail."));
        all.addChild(new CommentAssertion("This will (by default) cause the record to be saved to the internal audit database."));
        all.addChild(new CommentAssertion("Remove it once you have customized the audit sink policy."));
        all.addChild(falseAssertion);
        return all;
    }

    private Action prepareEditAction() throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getOrCreateSinkPolicyHeader();
        PolicyEntityNode node = new PolicyEntityNode(header);
        return new EditPolicyAction(node, true);
    }

    private boolean isCommittable() {
        return cbOutputToPolicy.isSelected() || cbSaveToDb.isSelected();
    }

    private void commit() {
        if (!isCommittable()) {
            DialogDisplayer.showMessageDialog(this, "At least one checkbox must be checked.", "Unable to Save", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        try {
            if (!cbOutputToPolicy.isSelected()) {
                ClusterPropertyCrud.deleteClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP);
                ClusterPropertyCrud.deleteClusterProperty(AUDIT_SINK_ALWAYS_SAVE_CLUSTER_PROP);
                return;
            }

            PolicyHeader header = getOrCreateSinkPolicyHeader();
            String guid = header.getGuid();
            ClusterPropertyCrud.putClusterProperty(AUDIT_SINK_ALWAYS_SAVE_CLUSTER_PROP, String.valueOf(cbSaveToDb.isSelected()));
            ClusterPropertyCrud.putClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP, guid);

            final Action editAction = prepareEditAction();
            
            DialogDisplayer.showConfirmDialog(this,
                    "Do you want to edit the audit sink policy now?",
                    "Edit Sink Policy",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    committed = true;
                    if (JOptionPane.YES_OPTION == option) {
                        policyEditRequested = true;
                        editAction.actionPerformed(null);
                    }
                    dispose();
                }
            });

        } catch (ObjectModelException e) {
            err("configure audit sink", e);
        } catch (PolicyAssertionException e) {
            err("configure audit sink", e);
        }
    }

    public boolean isCommitted() {
        return committed;
    }

    public boolean isPolicyEditRequested() {
        return policyEditRequested;
    }

    private void err(String what, Throwable t) {
        final String msg = "Unable to " + what + ": " + ExceptionUtils.getMessage(t);
        logger.log(Level.WARNING, msg, t);
        DialogDisplayer.showMessageDialog(this, "Unable to " + what, msg, null);
    }
}
