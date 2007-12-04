package com.l7tech.console.panels;

import com.l7tech.common.gui.SimpleTableModel;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.TableUtil;
import static com.l7tech.common.gui.util.TableUtil.column;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.policy.PolicyVersion;
import com.l7tech.common.util.BeanUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.common.util.Pair;
import com.l7tech.console.tree.policy.PolicyTreeCellRenderer;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;

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
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private final long policyOid;
    private final DateFormat dateFormat = DateFormat.getInstance();

    private final TreeModel emptyTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode(""));
    private final TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    private final PolicyTreeCellRenderer policyTreeCellRenderer = new PolicyTreeCellRenderer();

    private SimpleTableModel<PolicyVersion> tableModel;

    /** Predicate that matches any PolicyVersion whose 'active' flag is true. */
    private static final Functions.Unary<Boolean, PolicyVersion> IS_ACTIVE = new Functions.Unary<Boolean, PolicyVersion>() {
        public Boolean call(PolicyVersion policyVersion) {
            return policyVersion.isActive();
        }
    };

    /** Index of the "Act." table column in the table model. */
    private static final int COLUMN_IDX_ACTIVE = 0;

    public PolicyRevisionsDialog(Frame owner, long policyOid) {
        super(owner);
        this.policyOid = policyOid;
        initialize();
    }

    public PolicyRevisionsDialog(Dialog owner, long policyOid) {
        super(owner);
        this.policyOid = policyOid;
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
                showSelectedPolicyXml();
            }
        });

        setActiveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSetActive();
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

        pack();
        Utilities.centerOnScreen(this);

        fetchUpdatedRows();

        int actRow = tableModel.findFirstRow(IS_ACTIVE);
        if (actRow >= COLUMN_IDX_ACTIVE)
            versionTable.getSelectionModel().setSelectionInterval(actRow, actRow);

        enableOrDisableButtons();
        showSelectedPolicyXml();
    }

    private void doSetActive() {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;



        try {
            Registry.getDefault().getPolicyAdmin().setActivePolicyVersion(policyOid, info.right.getOid());

            for (Integer row : tableModel.findRows(IS_ACTIVE)) {
                tableModel.getRowObject(row).setActive(false);
                tableModel.fireTableCellUpdated(row, COLUMN_IDX_ACTIVE);
            }

            info.right.setActive(true);
            tableModel.fireTableRowsUpdated(info.left, info.left);
        } catch (Exception e) {
            showErrorMessage("Unable to Set Active Version", "Unable to set active version: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void doClearActive() {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;

        DialogDisplayer.showConfirmDialog(this,
                                          "This will disable this policy.  Continue anyway?",
                                          "Clear Active Version",
                                          JOptionPane.OK_CANCEL_OPTION,
                                          new DialogDisplayer.OptionListener() {
            public void reportResult(int option) {
                if (option != JOptionPane.OK_OPTION)
                    return;
                try {
                    Registry.getDefault().getPolicyAdmin().clearActivePolicyVersion(policyOid);

                    for (Integer row : tableModel.findRows(IS_ACTIVE)) {
                        tableModel.getRowObject(row).setActive(false);
                        tableModel.fireTableCellUpdated(row, COLUMN_IDX_ACTIVE);
                    }
                } catch (Exception e) {
                    showErrorMessage("Unable to Clear Active Version", "Unable to clear active version: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
    }

    private void doSetComment() {
        Pair<Integer, PolicyVersion> info = getSelectedPolicyVersion();
        if (info == null)
            return;
        PolicyVersion version = info.right;

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
            Registry.getDefault().getPolicyAdmin().setPolicyVersionComment(version.getPolicyOid(), version.getOid(), comment);
            version.setName(comment);
            tableModel.fireTableRowsUpdated(info.left, info.left);
        } catch (FindException e1) {
            showErrorMessage("Unable to Set Comment", "Unable to set comment: " + ExceptionUtils.getMessage(e1), e1);
        } catch (UpdateException e1) {
            showErrorMessage("Unable to Set Comment", "Unable to set comment: " + ExceptionUtils.getMessage(e1), e1);
        }
    }

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

    private void showSelectedPolicyXml() {
        Pair<Integer, PolicyVersion> version = getSelectedPolicyVersion();
        if (version == null) {
            policyTree.setCellRenderer(defaultRenderer);
            policyTree.setModel(emptyTreeModel);
            return;
        }
        try {
            String xml = getVersionXml(version.right);
            if (xml == null) {
                policyTree.setCellRenderer(defaultRenderer);
                policyTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Empty policy", false)));
                return;
            }
            Assertion assertion = WspReader.getDefault().parsePermissively(xml);
            TreeModel model = new PolicyTreeModel(assertion);
            policyTree.setModel(model);
            policyTree.setCellRenderer(policyTreeCellRenderer);
            Utilities.expandTree(policyTree);
        } catch (IOException e1) {
            policyTree.setCellRenderer(defaultRenderer);
            policyTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Bad policy XML: " + ExceptionUtils.getMessage(e1), false)));
        }
    }

    private String getVersionXml(PolicyVersion version) {
        String xml = version.getXml();
        if (xml == null) {
            // Fault in XML from server as revisions are clicked on for the first time
            try {
                PolicyVersion fullVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionByPrimaryKey(policyOid, version.getOid());
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
            java.util.List<PolicyVersion> vers = Registry.getDefault().getPolicyAdmin().findPolicyVersionHeadersByPolicy(policyOid);
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

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }
}
