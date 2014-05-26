package com.l7tech.console.panels;

import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.panels.policydiff.PolicyDiffContext;
import com.l7tech.console.panels.policydiff.PolicyDiffWindow;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.PolicyRevisionUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Dialog that allows admin to view versions of a policy, to name them, and to
 * designate a new active version.
 */
public class PolicyRevisionsDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(PolicyRevisionsDialog.class.getName());

    private JPanel mainForm;
    private JTable versionTable;
    private JButton editButton;
    private JButton setActiveButton;
    private JButton closeButton;
    private JTree policyTree;
    private JButton clearActiveButton;
    private JButton setCommentButton;
    private JButton diffButton;

    private final EntityWithPolicyNode policyNode;
    private final Goid policyGoid;
    private final DateFormat dateFormat = DateFormat.getInstance();

    private final TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    private final PolicyTreeCellRenderer policyTreeCellRenderer = new PolicyTreeCellRenderer();

    private SimpleTableModel<PolicyVersion> tableModel;
    private Map<Goid,String> identityProviderNameMap = new HashMap<Goid,String>();

    /** Predicate that matches any PolicyVersion whose 'active' flag is true. */
    private static final Functions.Unary<Boolean, PolicyVersion> IS_ACTIVE = new Functions.Unary<Boolean, PolicyVersion>() {
        public Boolean call(PolicyVersion policyVersion) {
            return policyVersion.isActive();
        }
    };

    /** Index of the "Act." table column in the table model. */
    private static final int COLUMN_IDX_ACTIVE = 0;

    public PolicyRevisionsDialog(Frame owner, EntityWithPolicyNode policyNode, Goid policyGoid) {
        super(owner);
        this.policyNode = policyNode;
        this.policyGoid = policyGoid;
        initialize();
    }

    public PolicyRevisionsDialog(Dialog owner, EntityWithPolicyNode policyNode, Goid policyGoid) {
        super(owner);
        this.policyNode = policyNode;
        this.policyGoid = policyGoid;
        initialize();
    }

    private void initialize() {
        setTitle("Policy Revisions");
        setModal(true);
        setContentPane(mainForm);

        tableModel = TableUtil.configureTable(
                versionTable,
                column("Act.", 12, 38, 64, new Functions.Unary<Object,PolicyVersion>() {
                    public Object call(PolicyVersion policyVersion) {
                        return policyVersion.isActive() ? "*" : "";
                    }
                }),
                column("Vers.", 12, 46, 64, property("ordinal")),
                column("Administrator", 32, 64, 999999, property("userLogin")),
                column("Date and Time", 64, 120, 220, new Functions.Unary<Object,PolicyVersion>() {
                    public Object call(PolicyVersion policyVersion) {
                        return dateFormat.format(policyVersion.getTime());
                    }
                }),
                column("Comment", 128, 128, 999999, property("name"))
        );

        versionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
                showSelectedPolicyXml(false);
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                doEdit(evt);
            }
        });

        setActiveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                doSetActive(evt);
            }
        });

        clearActiveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doClearActive();
            }
        });

        setCommentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSetComment();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        diffButton.setText("Compare Policy: " + (PolicyDiffContext.hasLeftDiffPolicy()? "Right" : "Left"));

        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (PolicyDiffContext.hasLeftDiffPolicy()) {
                    dispose();
                    new PolicyDiffWindow(PolicyDiffContext.getLeftDiffPolicyInfo(), getPolicyInfo()).setVisible(true);
                } else {
                    PolicyDiffContext.setLeftDiffPolicyInfo(getPolicyInfo());
                }

                diffButton.setText("Compare Policy: " + (PolicyDiffContext.hasLeftDiffPolicy()? "Right" : "Left"));
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(closeButton);

        pack();
        Utilities.centerOnScreen(this);

        fetchUpdatedRows();

        int actRow = tableModel.findFirstRow(IS_ACTIVE);
        if (actRow >= COLUMN_IDX_ACTIVE)
            versionTable.getSelectionModel().setSelectionInterval(actRow, actRow);

        enableOrDisableButtons();
        showSelectedPolicyXml(true);
    }

    /**
     * Obtain policy information such as policy full name and policy xml
     *
     * @return a pair of two strings: policy full name (policy name, resolution, version, and active status) and policy xml.
     */
    private Pair<String, PolicyTreeModel> getPolicyInfo() {
        final PolicyVersion selectedPolicyVersion = getSelectedPolicyVersion().right;

        final Goid policyGoid;
        try {
            policyGoid = policyNode.getPolicy().getGoid();
        } catch (FindException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                "Cannot find the policy, '" + policyNode.getName() + "'", "Policy Comparison Error",
                JOptionPane.WARNING_MESSAGE, null
            );
            return null;
        }
        final PolicyVersion latestPolicyVersion = Registry.getDefault().getPolicyAdmin().findLatestRevisionForPolicy(policyGoid);
        final long latestVersionNum = latestPolicyVersion.getOrdinal();
        final String policyFullName = PolicyRevisionUtils.getDisplayName(policyNode.getName(), selectedPolicyVersion.getOrdinal(), latestVersionNum, selectedPolicyVersion.isActive());
        final String policyXml = selectedPolicyVersion.getXml();

        PolicyTreeModel policyTreeModel;
        try {
            policyTreeModel = new PolicyTreeModel(WspReader.getDefault().parsePermissively(policyXml, WspReader.Visibility.includeDisabled));
        } catch (IOException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "Cannot parse the policy XML", "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
            return null;
        }

        return new Pair<>(policyFullName, policyTreeModel);
    }

    private void doEdit(ActionEvent evt) {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;
        PolicyVersion version = info.right;
        policyNode.clearCachedEntities();
        EditPolicyAction editAction = new EditPolicyAction(policyNode, true, version);
        dispose();
        editAction.actionPerformed(evt);
    }

    private void doSetActive(ActionEvent evt) {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;

        try {
            Registry.getDefault().getPolicyAdmin().setActivePolicyVersion(policyGoid, info.right.getGoid());
            policyNode.clearCachedEntities();

            for (Integer row : tableModel.findRows(IS_ACTIVE)) {
                tableModel.getRowObject(row).setActive(false);
                tableModel.fireTableCellUpdated(row, COLUMN_IDX_ACTIVE);
            }

            info.right.setActive(true);
            tableModel.fireTableRowsUpdated(info.left, info.left);
            doEdit(evt);
        } catch (Exception e) {
            showErrorMessage("Unable to Set Active Version", "Unable to set active version: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void doClearActive() {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;

        // If the policy is Audit Sink Policy, an error dialog pops up and shows it is not allowed to clear active for the audit sink policy.
        try {
            Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(policyGoid);
            if (isAuditSinkPolicy(policy)) {
                showErrorMessage(
                    "Unable to Clear Active Version",
                    "It is not allowed to disable the audit sink policy via Clear Active.",
                    null
                );
                return;
            }
        } catch (FindException e) {
            showErrorMessage("Unable to Clear Active Version", "Unable to find the policy (GOID: " + policyGoid + ")", e);
        }

        DialogDisplayer.showConfirmDialog(this,
                                          "This will disable this policy.  Continue anyway?",
                                          "Clear Active Version",
                                          JOptionPane.OK_CANCEL_OPTION,
                                          new DialogDisplayer.OptionListener() {
            public void reportResult(int option) {
                if (option != JOptionPane.OK_OPTION)
                    return;
                try {
                    Registry.getDefault().getPolicyAdmin().clearActivePolicyVersion(policyGoid);
                    policyNode.clearCachedEntities();
                    for (Integer row : tableModel.findRows(IS_ACTIVE)) {
                        tableModel.getRowObject(row).setActive(false);
                        tableModel.fireTableCellUpdated(row, COLUMN_IDX_ACTIVE);
                    }

                    versionTable.clearSelection();
                    showSelectedPolicyXml(true);
                    // If currently this policy, make sure we don't misleadingly imply that it's active (Bug #4554)
                    WorkSpacePanel workspace = TopComponents.getInstance().getCurrentWorkspace();
                    if (workspace.getComponent() instanceof PolicyEditorPanel) {
                        PolicyEditorPanel pep = (PolicyEditorPanel)workspace.getComponent();
                        if (Goid.equals(pep.getPolicyNode().getPolicy().getGoid(), policyGoid)) {
                            pep.setOverrideVersionActive(false);
                            pep.updateHeadings();
                        }
                    }
                    Object o = policyNode.getUserObject();
                    if(o instanceof OrganizationHeader){
                        OrganizationHeader sh = (OrganizationHeader)o;
                        sh.setPolicyDisabled(true);
                    }
                } catch (Exception e) {
                    showErrorMessage("Unable to Clear Active Version", "Unable to clear active version: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
    }

    private boolean isAuditSinkPolicy(Policy policy) {
        return policy != null &&
            PolicyType.INTERNAL.equals(policy.getType()) &&
                ExternalAuditStoreConfigWizard.INTERNAL_TAG_AUDIT_SINK.equals(policy.getInternalTag());
    }

    private void doSetComment() {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;
        PolicyVersion version = info.right;

        if (version.isActive() && areUnsavedChangesToThisPolicy(version.getPolicyGoid())) {
            JOptionPane.showMessageDialog(
                    this,
                    "<html>This policy is currently open for editing and has unsaved changes.<p>&nbsp;<p>Please save or discard edits before editing the active version's comment.",
                    "Active Version Comment",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String comment = version.getName();
        if (comment == null) comment = "";
        comment = JOptionPane.showInputDialog(
                this,
                "<html>Enter a comment for this revision, or leave blank to remove any comment.<br/>Note that revisions with comments will be retained forever.",
                comment);
        if (comment == null)
            return;

        if (comment.trim().length() < 1)
            comment = null;

        try {
            Registry.getDefault().getPolicyAdmin().setPolicyVersionComment(version.getPolicyGoid(), version.getGoid(), comment);
            version.setName(comment);
            tableModel.fireTableRowsUpdated(info.left, info.left);
        } catch (FindException e1) {
            showErrorMessage("Unable to Set Comment", "Unable to set comment: " + ExceptionUtils.getMessage(e1), e1);
        } catch (UpdateException e1) {
            showErrorMessage("Unable to Set Comment", "Unable to set comment: " + ExceptionUtils.getMessage(e1), e1);
        }
    }

    /** @return a Pair of the version OID and the version for the currently selected row */
    private Pair<Integer, PolicyVersion> getSelectedPolicyVersion() {
        int row = versionTable.getSelectedRow();
        if (row < COLUMN_IDX_ACTIVE) return null;
        PolicyVersion v = tableModel.getRowObject(row);
        if (v == null) return null;
        return new Pair<Integer, PolicyVersion>(row, v);
    }

    private void enableOrDisableButtons() {
        boolean sel = getSelectedPolicyVersion() != null;
        setActiveButton.setEnabled(sel);
        setCommentButton.setEnabled(sel);
        editButton.setEnabled(sel);
    }

    private void showSelectedPolicyXml(boolean initial) {
        Pair<Integer, PolicyVersion> version = getSelectedPolicyVersion();
        if (version == null) {
            policyTree.setCellRenderer(defaultRenderer);
            String message = initial ? "Policy disabled - no active version" : "No version selected";
            policyTree.setModel(new DefaultTreeModel(makeMessageNode(message)));
            return;
        }
        try {
            String xml = getVersionXml(version.right);
            if (xml == null) {
                policyTree.setCellRenderer(defaultRenderer);
                policyTree.setModel(new DefaultTreeModel(makeMessageNode("Empty policy")));
                return;
            }
            Assertion assertion = WspReader.getDefault().parsePermissively(xml, WspReader.INCLUDE_DISABLED);
            TreeModel model = new PolicyTreeModel(assertion);
            AssertionTreeNode assTreeNode = (AssertionTreeNode)model.getRoot();
            PolicyTreeUtils.updateAssertions( assTreeNode, identityProviderNameMap );
            policyTree.setModel(model);
            policyTreeCellRenderer.setPolicyVersion(version.right);
            policyTree.setCellRenderer(policyTreeCellRenderer);
            Utilities.expandTree(policyTree);
        } catch (IOException e1) {
            policyTree.setCellRenderer(defaultRenderer);
            policyTree.setModel(new DefaultTreeModel(makeMessageNode("Bad policy XML: " + ExceptionUtils.getMessage(e1))));
        }
    }

    private static TreeNode makeMessageNode(final String message) {
        return new LeafAssertionTreeNode<CommentAssertion>(new CommentAssertion(message)) {

            public String getName(final boolean decorate) {
                return message;
            }

            protected String iconResource(boolean open) {
                return null;
            }

            public String toString() {
                return message;
            }
        };
    }

    private String getVersionXml(PolicyVersion version) {
        String xml = version.getXml();
        if (xml == null) {
            // Fault in XML from server as revisions are clicked on for the first time
            try {
                PolicyVersion fullVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionByPrimaryKey(policyGoid, version.getGoid());
                BeanUtils.copyProperties(fullVersion, version);
                xml = version.getXml();
            } catch (Exception e) {
                showErrorMessage("Unable to Load Revision", "Unable to load the policy XML for a policy revision: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return xml;
    }

    private static Functions.Unary<Object, PolicyVersion> property(String propName) {
        return Functions.propertyTransform(PolicyVersion.class, propName);
    }

    private void fetchUpdatedRows() {
        try {
            java.util.List<PolicyVersion> vers = Registry.getDefault().getPolicyAdmin().findPolicyVersionHeadersByPolicy(policyGoid);
            Collections.sort(vers, new Comparator<PolicyVersion>() {
                public int compare(PolicyVersion o1, PolicyVersion o2) {
                    return Long.valueOf(o2.getOrdinal()).compareTo(o1.getOrdinal());
                }
            });
            tableModel.setRows(vers);
        } catch (FindException e) {
            showErrorMessage("Unable to Load Revisions", "There was an error while loading policy revisions.\n\nError message: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static boolean areUnsavedChangesToThisPolicy(Goid policyGoid) {
        PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        return pep != null && Goid.equals(policyGoid, pep.getPolicyGoid()) && pep.isUnsavedChanges();
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }
}