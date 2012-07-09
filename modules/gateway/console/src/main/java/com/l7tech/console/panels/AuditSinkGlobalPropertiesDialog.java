package com.l7tech.console.panels;

import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
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


    public static final String AUDIT_SINK_POLICY_GUID_CLUSTER_PROP = "audit.sink.policy.guid";
    public static final String AUDIT_SINK_ALWAYS_SAVE_CLUSTER_PROP = "audit.sink.alwaysSaveInternal";
    public static final String AUDIT_LOOKUP_POLICY_GUID_CLUSTER_PROP = "audit.lookup.policy.guid";


    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox cbOutputToPolicy;
    private JCheckBox cbSaveToDb;
    private JButton configureButton;

    boolean committed = false;
    boolean policyEditRequested = false;
    private PolicyHeader sinkPolicyHeader = null;
    private PolicyHeader lookupPolicyHeader = null;

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
                committed = false;
                dispose();
            }
        });

        configureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doConfigure();
            }
        });

        final ActionListener buttonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDiableButtons();
            }
        };
        cbSaveToDb.addActionListener(buttonListener);
        cbOutputToPolicy.addActionListener(buttonListener);

        //getRootPane().setDefaultButton(okButton); // Not sure we want to make this quite THAT easy to commit
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(okButton, cancelButton);

        final boolean usingSinkPolicy = isUsingSinkPolicy() || isUsingLookupPolicy();
        cbOutputToPolicy.setSelected(usingSinkPolicy);
        cbSaveToDb.setSelected(!usingSinkPolicy || isAlwaysSaveToDb());
        enableDiableButtons();
    }

    private void enableDiableButtons() {
        okButton.setEnabled(isCommittable());
    }

    private void doConfigure() {
        final ExternalAuditStoreConfigWizard dialog = ExternalAuditStoreConfigWizard.getInstance(this,getSinkPolicyHeader(),getLookupPolicyHeader());
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                sinkPolicyHeader = dialog.getSinkPolicyHeader();
                lookupPolicyHeader = dialog.getLookupPolicyHeader();
            }
        });
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

    private boolean isUsingLookupPolicy() {
        try {
            String configuredGuid = ClusterPropertyCrud.getClusterProperty(AUDIT_LOOKUP_POLICY_GUID_CLUSTER_PROP);
            if (configuredGuid == null || configuredGuid.trim().length() < 1)
                return false;
            final PolicyHeader lookupHeader = getLookupPolicyHeader();
            return lookupHeader != null && configuredGuid.equals(lookupHeader.getGuid());
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to check if lookup policy configured and exists: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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

    private PolicyHeader getSinkPolicyHeader()  {
        if(sinkPolicyHeader != null ) return sinkPolicyHeader;
        try {
            Collection<PolicyHeader> allInternals = Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INTERNAL);
            for (PolicyHeader internal : allInternals) {
                if (ExternalAuditStoreConfigWizard.INTERNAL_TAG_AUDIT_SINK.equals(internal.getDescription())) {
                    sinkPolicyHeader = internal;
                    break;
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve internal policies: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return sinkPolicyHeader;
    }

    private PolicyHeader getLookupPolicyHeader()  {
        if(lookupPolicyHeader != null ) return lookupPolicyHeader;
        try {
            Collection<PolicyHeader> allInternals = Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INTERNAL);
            for (PolicyHeader internal : allInternals) {
                if (ExternalAuditStoreConfigWizard.INTERNAL_TAG_AUDIT_LOOKUP.equals(internal.getDescription())) {
                    lookupPolicyHeader = internal;
                    break;
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve internal policies: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return lookupPolicyHeader;
    }



    private boolean isCommittable() {
        return cbSaveToDb.isSelected() || ( cbOutputToPolicy.isSelected() && sinkPolicyHeader != null && lookupPolicyHeader != null );
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
                ClusterPropertyCrud.deleteClusterProperty(AUDIT_LOOKUP_POLICY_GUID_CLUSTER_PROP);
                dispose();
                return;
            }

            ClusterPropertyCrud.putClusterProperty(AUDIT_SINK_ALWAYS_SAVE_CLUSTER_PROP, String.valueOf(cbSaveToDb.isSelected()));
            if(sinkPolicyHeader!=null)
                ClusterPropertyCrud.putClusterProperty(AUDIT_SINK_POLICY_GUID_CLUSTER_PROP, sinkPolicyHeader.getGuid());
            if(lookupPolicyHeader!=null)
                ClusterPropertyCrud.putClusterProperty(AUDIT_LOOKUP_POLICY_GUID_CLUSTER_PROP, lookupPolicyHeader.getGuid());
            committed = true;

        } catch (ObjectModelException e) {
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
